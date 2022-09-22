(ns beholder.main
  (:require [beholder.model :as m]
            [beholder.repositories.config :as conf]
            [beholder.repositories.k8s :as k8s]
            [beholder.dashboard :as dashboard]
            [beholder.template :as tmpl]
            [clj-http.client :as client]
            [clojure.string :refer [split]]
            [clojure.tools.logging :as log]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults]]
            [ring.util.http-response :refer :all]))

(log/info "Initializing ElasticSearch with test data. Refresh page to see the results")
; (boot/init-elastic)

; ---------------------------------------------------------------

(defn- eval-code [code]
  (try
    {:result (eval (read-string code))
     :code   code}
    (catch Exception e
      {:result e
       :code   code}
      )))


(defroutes app-routes
           (route/resources "/static")

           (GET "/" []
             (ok (tmpl/html "dashboard.html"
                            {:services (dashboard/list-services!)})))

           ;; ------------------------------------------------------------

           (context "/service/:id" [id]
             (GET "/config" []
               (ok (tmpl/html "service-config.html"
                              {:service (k8s/get-service! id)
                               :data    (conf/get-service-config id)})))

             (POST "/config" [openApiPath team repo description]
               (->>
                 (m/->ServiceConfig id openApiPath team repo description)
                 (conf/save-service-config!)
                 (assoc {:status :success :service (k8s/get-service! id)} :data)
                 (tmpl/html "service-config.html")
                 (ok))))

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

           ;; ------------------------------------------------------------

           (context "/debug" []
             (GET "/" [] (ok (tmpl/html "debug.html")))
             (POST "/" [code] (ok (tmpl/html "debug.html" (eval-code code)))))

           (GET "/doc/swagger/:id" [id]
             (ok (tmpl/html "swagger-ui.html"
                            {:id id})))

           (GET "/proxy/:id/swagger" [id]
             (as->
               (k8s/get-service! id) v
               (:url v)
               (str v "/static/api.json") v
               (client/get v)))


           ;
           ;(GET "/doc/:id" [id]
           ;  (ok (tmpl/html "doc.html"
           ;                 {:doc {:id id}})))
           ;

           ;
           ;(context "/proxy" []
           ;  ;; Load page
           ;  (GET "/:id" [id]
           ;    (ok (->> (r/get-documentation! id)
           ;             (proxy/request-page! "/proxy/static/"))))
           ;  ;; Load static resources
           ;  (GET "/static/:encoded-url/:resource" [encoded-url resource]
           ;    (proxy/request-content! encoded-url resource)))

           (ANY "*" [] (not-found "404")))


(defn wrap-fallback-exception
  [handler]
  (fn [request]
    (try
      (handler request)
      (catch Exception e
        (internal-server-error (tmpl/html "errors/500.html" {:error e}))))))

(def app
  (->
    app-routes
    (wrap-defaults {:params {:urlencoded true
                             :multipart  true
                             :nested     true
                             :keywordize true}})
    wrap-fallback-exception))
