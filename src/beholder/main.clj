(ns beholder.main
  (:require [beholder.k8s :as k8s]
            [beholder.template :as tmpl]
            [clj-http.client :as client]
            [clojure.tools.logging :as log]
            [compojure.core :refer :all]
            [compojure.route :as route]
    ;[beholder.proxy :as proxy]
            [ring.middleware.defaults :refer [wrap-defaults]]
    ;[beholder.bootstrap :as boot]
            [ring.util.http-response :refer :all]
    ;[beholder.repository :as r]
    ;[qbits.spandex :as s]
            ))

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
                            {:services (k8s/list-services!)})))

           (GET "/doc/swagger/:id" [id]
             (ok (tmpl/html "swagger-ui.html"
                            {:id id})))

           (GET "/proxy/:id/swagger" [id]
             (as->
               (k8s/get-service! id) v
               (:url v)
               (str v "/static/api.json") v
               (client/get v)))

           (context "/debug" []
             (GET "/" [] (ok (tmpl/html "debug.html" {})))
             (POST "/" [code] (ok (tmpl/html "debug.html" (eval-code code)))))

           ;
           ;(GET "/doc/:id" [id]
           ;  (ok (tmpl/html "doc.html"
           ;                 {:doc {:id id}})))
           ;
           ;(GET "/config" []
           ;  (ok (tmpl/html "config.html")))
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

(def app
  (->
    app-routes
    (wrap-defaults {:params {:urlencoded true
                             :multipart  true
                             :nested     true
                             :keywordize true}})))
