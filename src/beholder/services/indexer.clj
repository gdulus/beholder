(ns beholder.services.indexer
  (:require [chime.core :refer [chime-at periodic-seq]]
            [taoensso.timbre :as log]
            [beholder.repositories.k8s :as k8s]
            [beholder.repositories.es :as es])
  (:import (java.time Duration Instant)))



(defn get-asyncapi-services []
  (let [beholder-config (es/get-beholder-config!)])
  (->> (k8s/list-services!)
      (filter )))




(defn start []
  (let [now (Instant/now)
        period (Duration/ofSeconds 1)]
    (log/info "Setting up indexer. First execution will happen on " now " and will repeat every " period)
    (chime-at (periodic-seq now period)
              (fn [time]
                (log/info "Chiming at" time)))))