(ns beholder.handlers
  (:require [beholder.helpers :refer [fetch-remote-resource remote-resource-exists?]]
            [beholder.model :as m]
            [beholder.repositories.es :as es]
            [beholder.repositories.k8s :as k8s]
            [beholder.services.carrier :as carrier]
            [beholder.template :as tmpl]
            [clojure.string :refer [split]]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults]]
            [ring.util.http-response :refer :all]
            [taoensso.timbre :as log]))

(defroutes app-routes
           (route/resources "/static")

           ;; ------------------------------------------------------------
           ;; DASHBOARD
           ;; ------------------------------------------------------------

           (GET "/" []
             (ok (tmpl/html "dashboard.html"
                            {:services (carrier/list-carriers!)})))

           ;; ------------------------------------------------------------
           ;; GLOBAL CONFIG
           ;; ------------------------------------------------------------

           (context "/config" []
             (GET "/" []
               (as->
                 (es/get-beholder-config!) v
                 (or v (m/map->BeholderConfig {:namespaces []}))
                 {:data v}
                 (tmpl/html "beholder-config.html" v)
                 (ok v)))

             (POST "/" [namespaces openApiLabel openApiPath asyncApiLabel asyncApiPath]
               (->>
                 (m/->BeholderConfig (split namespaces #",")
                                     openApiLabel
                                     openApiPath
                                     asyncApiLabel
                                     asyncApiPath)
                 (es/save-beholder-config!)
                 (assoc {:status :success} :data)
                 (tmpl/html "beholder-config.html")
                 (ok))))

           ;; ------------------------------------------------------------
           ;; SERVICE CONFIG
           ;; ------------------------------------------------------------

           (context "/service/:id" [id]
             (GET "/config" [status]
               (let [beholder-conf (es/get-beholder-config!)
                     service-conf (es/get-service-config id)
                     k8s-service (k8s/get-service! id)
                     ; ----------
                     openapi-url (m/get-openapi-url beholder-conf k8s-service service-conf)
                     openapi-label (m/get-openapi-label beholder-conf)
                     openapi-supported? (contains? (:labels k8s-service) openapi-label)
                     ; ----------
                     asyncapi-url (m/get-asyncapi-url beholder-conf k8s-service service-conf)
                     asyncapi-label (m/get-asyncapi-label beholder-conf)
                     asyncapi-supported? (contains? (:labels k8s-service) asyncapi-label)]
                 (ok (tmpl/html "service-config.html"
                                {:status   status
                                 :service  k8s-service
                                 :data     service-conf
                                 :openapi  {:url        openapi-url
                                            :label      (name openapi-label)
                                            :supported? openapi-supported?
                                            :status     (when openapi-supported? (remote-resource-exists? openapi-url))}
                                 :asyncapi {:url        asyncapi-url
                                            :label      (name asyncapi-label)
                                            :supported? asyncapi-supported?
                                            :status     (when asyncapi-supported? (remote-resource-exists? asyncapi-url))}}))))

             (POST "/config" [openApiPath asyncApiPath team repo description]
               (as->
                 (m/map->ServiceConfig {:serviceId    id
                                        :openApiPath  openApiPath
                                        :asyncApiPath asyncApiPath
                                        :team         team
                                        :repo         repo
                                        :description  description}) v
                 (es/save-service-config! v)
                 (str "/service/" id "/config?status=success")
                 (moved-permanently v))))

           ;; ------------------------------------------------------------
           ;; DOCUMENTATION RENDERING - OPENAPI
           ;; ------------------------------------------------------------

           (context "/service/:id/doc/openapi" [id]
             (GET "/" []
               (ok (tmpl/html "openapi-ui.html" {:id id})))

             (GET "/proxy" []
               (let [beholder-conf (es/get-beholder-config!)
                     service-conf (es/get-service-config id)
                     k8s-service (k8s/get-service! id)
                     api-doc-url (m/get-openapi-url beholder-conf k8s-service service-conf)]
                 (log/info "Requesting OpenAPI doc from" api-doc-url)
                 (fetch-remote-resource api-doc-url))))

           ;; ------------------------------------------------------------
           ;; OTHER
           ;; ------------------------------------------------------------

           (ANY "*" [] (not-found "404")))


(defn wrap-fallback-exception
  [handler]
  (fn [request]
    (try
      (handler request)
      (catch Exception e
        (log/error "Error while handling request" e)
        (internal-server-error (tmpl/html "errors/500.html" {:error e}))))))

(def app
  (->
    app-routes
    (wrap-defaults {:params {:urlencoded true
                             :multipart  true
                             :nested     true
                             :keywordize true}})
    wrap-fallback-exception))
