(ns beholder.helpers
  (:require [clj-http.client :as client]
            [taoensso.timbre :as log]))

(defn remote-resource-exists? [url]
  (try
    (case
      (:status (client/head url {:throw-exceptions false}))
      200 :found
      404 :not-found
      :error)
    (catch Throwable e
      (log/error "Error while checking resource" e)
      :error)))

(defn fetch-remote-resource [url]
  (try
    (client/get url)
    (catch Throwable e
      (log/error "Error while loading resource" e)
      "")))
