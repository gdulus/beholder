(ns beholder.utils.date
  (:require
   [clojure.instant :as instant])
  (:import
   [java.time.temporal ChronoUnit]))

(defn now []
  (-> (java.util.Date.)
      (.toInstant)
      (.truncatedTo ChronoUnit/SECONDS)
      (java.util.Date/from)))

(defn parse-str [str]
  (instant/read-instant-date str))

(defn convert-map-field [field m]
  (->> (field m)
       (parse-str)
       (assoc m field)))
