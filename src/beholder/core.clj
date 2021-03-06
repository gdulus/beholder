(ns beholder.core
  (:require [compojure.core :refer :all]
            [ring.util.http-response :refer :all]
            [compojure.route :as route]
            [beholder.template :as tmpl]
            [beholder.proxy :as proxy]
            [clojure.tools.logging :as log]
            [beholder.bootstrap :as boot]
            [ring.middleware.defaults :refer [wrap-defaults]]
            [beholder.repository :as r]
            [ring.util.json-response :refer [json-response]]
            [qbits.spandex :as s]))

(log/info "Initializing ElasticSearch with test data. Refresh page to see the results")
; (boot/init-elastic)

; ---------------------------------------------------------------

(defroutes app-routes
           (route/resources "/static")

           (GET "/" []
             (ok (tmpl/html "dashboard.html"
                            {:docs (r/list-documentation!)})))

           (GET "/doc/:id" [id]
             (ok (tmpl/html "doc.html"
                            {:doc {:id id}})))

           (GET "config" [id]
             (ok (tmpl/html "doc.html"
                            {:doc {:id id}})))

           (context "/proxy" []
             ;; Load page
             (GET "/:id" [id]
               (ok (->> (r/get-documentation! id)
                        (proxy/request-page! "/proxy/static/"))))
             ;; Load static resources
             (GET "/static/:encoded-url/:resource" [encoded-url resource]
               (proxy/request-content! encoded-url resource)))

           (ANY "*" [] (not-found "404")))

(def app
  (->
    app-routes
    (wrap-defaults {:params {:urlencoded true
                             :multipart  true
                             :nested     true
                             :keywordize true}})))