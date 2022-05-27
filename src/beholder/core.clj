(ns beholder.core
  (:require [compojure.core :refer :all]
            [ring.util.http-response :refer :all]
            [compojure.route :as route]
            [beholder.template :as tmpl]
            [beholder.crawler :as crawler]
            [ring.middleware.defaults :refer [wrap-defaults]]
            [beholder.repository :as r]
            [ring.util.json-response :refer [json-response]]
            [qbits.spandex :as s]))

(defroutes app-routes
           (route/resources "/static")
           (GET "/" [] "test")
           (GET "/easybill" [] (crawler/get-content! "https://www.easybill.de/api/"))
           (GET "/api/v1/conf/documentations" [] (json-response {:data (r/get-documentation!)}))
           (GET "/*" [] (not-found "404")))

(def app
  (->
    app-routes
    (wrap-defaults {:params {:urlencoded true
                             :multipart  true
                             :nested     true
                             :keywordize true}})))




;(def c (s/client {:hosts ["http://127.0.0.1:9200"]}))
;(s/request c {:url [:_aliases]})

; CREATE INDEXES
;(s/request c {:url [:_aliases]})
;(s/request c {:method :put :url [:config]})
;(s/request c {:method :put :url [:docs]})

; CREATE DATA
;(s/request c {:method :delete :url [:instances]})
;(s/request c {:method :delete :url [:docs]})

;(try
;  (s/request c {:method :post :url [:config :instances] :body {:name "EasyBill" :url "https://www.easybill.de/api/" :type "swagger"}})
;  (catch Exception e
;    println e
;    ))
