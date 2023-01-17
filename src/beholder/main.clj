(ns beholder.main
  (:require [beholder.dashboard :as dashboard]
            [beholder.model :as m]
            [beholder.repositories.config :as conf]
            [beholder.repositories.k8s :as k8s]
            [beholder.template :as tmpl]
            [clj-http.client :as client]
            [clojure.string :refer [split]]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults]]
            [ring.util.http-response :refer :all]
            [taoensso.timbre :as log]))


; ---------------------------------------------------------------

(defn- eval-code [code]
  (try
    {:result (eval (read-string code))
     :code   code}
    (catch Exception e
      {:result e
       :code   code}
      )))

(defn- get-openapi-status [openapi-doc-url]
  (try
    (case (:status (client/head openapi-doc-url {:throw-exceptions false}))
      200 :ok
      404 :not-found
      :error)
    (catch Exception e
      :error)))

(defroutes app-routes
           (route/resources "/static")

           (GET "/" []
             (ok (tmpl/html "dashboard.html"
                            {:services (dashboard/list-services!)})))

           ;; ------------------------------------------------------------

           (context "/service/:id" [id]

             (GET "/config" [status]
               (let [beholder-conf (conf/get-beholder-config!)
                     service-conf (conf/get-service-config id)
                     k8s-service (k8s/get-service! id)
                     api-doc-url (m/get-openapi-url beholder-conf k8s-service service-conf)
                     api-doc-status (get-openapi-status api-doc-url)]
                 (ok (tmpl/html "service-config.html"
                                {:service        k8s-service
                                 :data           service-conf
                                 :api-doc-url    api-doc-url
                                 :api-doc-status api-doc-status
                                 :status         status}))))

             (POST "/config" [openApiPath team repo description]
               (as->
                 (m/->ServiceConfig id openApiPath team repo description) v
                 (conf/save-service-config! v)
                 (str "/service/" id "/config?status=success")
                 (moved-permanently v)))

             (GET "/openapi" [id]
               (let [beholder-conf (conf/get-beholder-config!)
                     service-conf (conf/get-service-config id)
                     k8s-service (k8s/get-service! id)
                     api-doc-url (m/get-openapi-url beholder-conf k8s-service service-conf)]
                 (log/info "Requesting OpenAPI doc from" api-doc-url)
                 (client/get api-doc-url))))

           ;; ------------------------------------------------------------

           (context "/config" []
             (GET "/" []
               (as->
                 (conf/get-beholder-config!) v
                 (or v (m/map->BeholderConfig {:namespaces []}))
                 {:data v}
                 (tmpl/html "beholder-config.html" v)
                 (ok v)))

             (POST "/" [namespaces openApiLabel openApiPath]
               (->>
                 (m/->BeholderConfig (split namespaces #",") openApiLabel, openApiPath)
                 (conf/save-beholder-config!)
                 (assoc {:status :success} :data)
                 (tmpl/html "beholder-config.html")
                 (ok))))

           ; ------------------------------------------------------------

           (GET "/doc/openapi/:id" [id]
             (ok (tmpl/html "swagger-ui.html" {:id id})))

           ; ------------------------------------------------------------

           ;(context "/debug" []
           ;  (GET "/" [] (ok (tmpl/html "debug.html")))
           ;  (POST "/" [code] (ok (tmpl/html "debug.html" (eval-code code)))))

           (ANY "*" [] (not-found "404")))


(defn wrap-fallback-exception
  [handler]
  (fn [request]
    (try
      (handler request)
      (catch Exception e
        (log/error "Error while handling request" e)
        (internal-server-error (tmpl/html "errors/500.html" {:error e}))))))


(log/info "Starting Beholder app")

(def app
  (->
    app-routes
    (wrap-defaults {:params {:urlencoded true
                             :multipart  true
                             :nested     true
                             :keywordize true}})
    wrap-fallback-exception))
