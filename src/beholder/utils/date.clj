(ns beholder.utils.date
  (:require
   [clojure.instant :as instant])
  (:import
   [java.time.temporal ChronoUnit]
   [java.text SimpleDateFormat]))

(defn unix-date []
  (-> (SimpleDateFormat. "dd/MM/yyyy")
      (.parse "01/01/1970")))

(defn now
  ([offset]
   (let [n (now)
         t (+ (.getTime n) offset)]
     (.setTime n t)
     n))
  ([]
   (-> (java.util.Date.)
       (.toInstant)
       (.truncatedTo ChronoUnit/SECONDS)
       (java.util.Date/from))))

(defn parse-str [str]
  (instant/read-instant-date str))

(defn convert-map-field [field m]
  (->> (field m)
       (parse-str)
       (assoc m field)))

(defn <=+ [a b]
  (>= 0 (compare a b)))
