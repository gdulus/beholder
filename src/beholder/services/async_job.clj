(ns beholder.services.async-job
  (:require
   [beholder.repositories.es :as es]
   [beholder.utils.date :as date]
   [chime.core :as chime :refer [periodic-seq]]
   [beholder.model :as m]
   [beholder.utils.log :as log])
  (:import
   [java.time Duration Instant]))

(defn start-job [name interval exec-fn]
  (let [start (Instant/now)
        period (Duration/ofSeconds interval)
        first-job-run (m/map->AsyncJobRun {:name name :lastRun (date/unix-date)})]
    (log/info "Starting job '" name "' with delay = " start " and period = " period)
    (chime/chime-at (periodic-seq start period)
                    (fn [_]
                      (try
                        (let [current-job-run (m/map->AsyncJobRun {:name name :lastRun (date/now)})
                              persisted-job-run (es/get-async-job-run! name)
                              last-job-run (or persisted-job-run first-job-run)]
                          (log/info "Running job '" name "' with job-run config = " last-job-run)
                          (exec-fn last-job-run)
                          (es/save-async-job-run! current-job-run))
                        (catch Throwable e
                          (log/error (str "There was an error while running job " name) e)))))))
