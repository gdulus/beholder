(ns beholder.template
  (:require [selmer.parser :as selmer]
            [selmer.util :refer [without-escaping]]))

(defn text
  ([file] (text file {}))
  ([file params] (selmer/render-file (str "templates/" file) params)))

(defn html
  ([file]
   (html file {}))
  ([file params]
   (without-escaping
     (selmer/render-file (str "templates/" file) params))))
