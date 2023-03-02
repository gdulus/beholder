(ns beholder.helpers
  (:require [clj-http.client :as client]
            [beholder.utils.log :as log]))

(defn remote-resource-exists? [url]
  (try
    (case
     (:status (client/head url {:throw-exceptions false}))
      200 :found
      404 :not-found
      :error)
    (catch Exception _
      (log/warn "Error while checking resource")
      :error)))

(defn fetch-remote-resource [url]
  (try
    (client/get url)
    (catch Exception _
      (log/warn "Error while loading resource")
      "")))
