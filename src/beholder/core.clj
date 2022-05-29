(ns beholder.core
  (:require [compojure.core :refer :all]
            [ring.util.http-response :refer :all]
            [compojure.route :as route]
            [beholder.template :as tmpl]
            [clojure.tools.logging :as log]
            [beholder.bootstrap :as boot]
            [ring.middleware.defaults :refer [wrap-defaults]]
            [beholder.repository :as r]
            [ring.util.json-response :refer [json-response]]
            [qbits.spandex :as s]))

(log/info "Initializing ElasticSearch with test data. Refresh page to see the results")
(boot/init-elastic)

; ---------------------------------------------------------------

(defn- dashboard []
  (ok (tmpl/html "index.html"
                 {:docs (r/get-documentation!)})))

; ---------------------------------------------------------------

(defroutes app-routes
           (route/resources "/static")

           ; ----------------- WEB
           (GET "/" [] (dashboard))
           (GET "/doc/:_/:id" [id] (str id))

           ; ----------------- API

           (GET "/api/v1/conf/documentations" [] (json-response {:data (r/get-documentation!)}))
           (GET "/*" [] (not-found "404")))

(def app
  (->
    app-routes
    (wrap-defaults {:params {:urlencoded true
                             :multipart  true
                             :nested     true
                             :keywordize true}})))