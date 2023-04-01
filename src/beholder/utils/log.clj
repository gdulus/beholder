(ns beholder.utils.log
  (:require [clojure.string :as s]
            [clojure.pprint :as p]
            [beholder.utils.date :as date]))

(defn log [level msg & other]
  (println (str "[" level "][" (date/now) "]:"  msg " " (s/join " " other))))

(defn info [msg & other]
  (log "INFO" msg other))

(defn warn [msg & other]
  (log "WARN" msg other))

(defn error [msg exp]
  (log "ERROR" msg ">>" (ex-message exp) ">>" (with-out-str (p/pprint (ex-data exp)))))

(defn spy
  ([_ value]
   (spy _ "=>" value))
  ([_ msg value]
   (log "SPY" msg value)
   value))


