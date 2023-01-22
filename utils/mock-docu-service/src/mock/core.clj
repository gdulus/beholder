(ns mock.core
  (:require
    [compojure.core :refer :all]
    [ring.util.http-response :refer :all]
    [compojure.route :as route]
    [ring.middleware.defaults :refer [wrap-defaults]]))

(defroutes app-routes
           (route/resources "/static")
           (GET "/health" [] (ok "ok"))
           (ANY "*" [] (not-found "404")))

(def app
  (->
    app-routes
    (wrap-defaults {:params {:urlencoded true
                             :multipart  true
                             :nested true
                             :keywordize true}})))