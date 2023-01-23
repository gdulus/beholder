(ns beholder.services.indexer
  (:require [chime.core :refer [chime-at periodic-seq]]
            [taoensso.timbre :as log])
  (:import (java.time Duration Instant)))



(defn start []
  (let [now (Instant/now)
        period (Duration/ofSeconds 1)]
    (log/info "Setting up indexer. First execution will happen on " now " and will repeat every " period)
    (chime-at (periodic-seq now period)
              (fn [time]
                (log/info "Chiming at" time)))))