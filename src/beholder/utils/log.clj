(ns beholder.utils.log
  (:require [clojure.string :as s]))

(defn- log [level msg & other]
  (println (str "[" level "]" msg " " (s/join " " other))))

(defn info [msg & other]
  (log "INFO" msg other))

(defn warn [msg & other]
  (log "WARN" msg other))

(defn error [msg exp]
  (log "ERROR" msg exp))

(defn spy
  ([_ value]
   (spy _ "=>" value))
  ([_ msg value]
   (log "SPY" msg value)
   value))
