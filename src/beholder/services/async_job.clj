(ns beholder.services.async-job
  (:require
   [beholder.repositories.es :as es]
   [beholder.utils.date :as date]
   [chime.core :as chime :refer [periodic-seq]]
   [beholder.model :as m]
   [beholder.utils.log :as log])
  (:import
   [java.time Duration Instant]))

(def ^:private synchronizer (atom {}))

(defn- sync-job-run 
  "Synchronizes job run. Act of synchronization does not need to be atomic as concurent executions of 
   the job with the same name are spaced by at least one sec"
  [job-name exec-fn]
  (if-not (get @synchronizer job-name)
    (try 
      (swap! synchronizer assoc job-name :running)
      (exec-fn)
      (finally (swap! synchronizer dissoc job-name)))
    (log/warn "Job" job-name "already running. Skipping execution")))

(defn start-job [name interval exec-fn]
  (let [start         (Instant/now)
        period        (Duration/ofSeconds interval)
        first-job-run (m/map->AsyncJobRun {:name    name
                                           :lastRun (date/unix-date)})]
    (log/info "Starting job '" name "' with delay = " start " and period = " period)
    (chime/chime-at (periodic-seq start period)
                    (fn [_]
                      (sync-job-run name
                                    #(try 
                                       (let [current-job-run   (m/map->AsyncJobRun {:name    name
                                                                                    :lastRun (date/now)})
                                             persisted-job-run (es/get-async-job-run! name)
                                             last-job-run      (or persisted-job-run first-job-run)]
                                         (log/info "Running job '" name "' with job-run config = " last-job-run)
                                         (exec-fn last-job-run)
                                         (es/save-async-job-run! current-job-run))
                                       (catch Throwable e
                                         (log/error (str "There was an error while running job " name) e))))))))