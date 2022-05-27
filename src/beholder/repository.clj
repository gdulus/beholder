(ns beholder.repository
  (:require [qbits.spandex :as e]
            [clojure.tools.logging :as log]
            [schema.core :as s]
            [beholder.model :as m])
  (:import (beholder.model Documentation)))

(def ^:private c (e/client {:hosts ["http://127.0.0.1:9200"]}))
(def ^:private doc-instance-url [:config :doc-instance])

(defn- doc-instance->Documentation [doc-instance]
  (->> (:_source doc-instance)
       (merge {:id (:_id doc-instance)})
       (m/map->Documentation)
       (s/validate Documentation)))

; ----------------------------------------------------------------

(defn delete-indexes! []
  (try
    (e/request c {:method :delete :url [:config]})
    (e/request c {:method :delete :url [:docs]})
    (catch Exception e (log/error "Error while deleting index" e))))

(defn create-indexes! []
  (try
    (e/request c {:method :put :url [:config]})
    (e/request c {:method :put :url [:docs]})
    (catch Exception e (log/error "Error while crating index" e))))

; ----------------------------------------------------------------

(defn save-documentation! [data]
  (as-> data v
        (s/validate Documentation v)
        (e/request c {:method :post :url doc-instance-url :body v})))

(defn get-documentation! []
  (as-> (concat doc-instance-url [:_search]) v
        (e/request c {:url v})
        (get-in v [:body :hits :hits])
        (map #(doc-instance->Documentation %) v)
        ))
