(ns beholder.crawler
  (:require [beholder.repository :as r]
            [beholder.model :as m]
            [etaoin.api :as api]
            [clojure.string :as s]
            [jsoup.soup :as jsoup]))

(defn- get-clean-uri [element attr]
  (-> (.attr element attr)
      (s/replace "./" "")))

(defn- clean-references [document attr prefix]
  (->> document
       (jsoup/select (str "*[" attr "]"))
       (map #(.attr % attr (str prefix "/" (get-clean-uri % attr))))
       (last)
       (.ownerDocument)))

(defn- clean-page [url prefix]
  (-> (jsoup/get! url)
      (clean-references "href" prefix)
      (clean-references "src" prefix)))

(def url "https://www.easybill.de/api")
(def prefix "http://localhost:3000/doc/static/DbOYD4EB2JNT9ifWrtcm/")
(println (clean-page url prefix))


;(def ^:private driver (api/firefox {:headless true}))
;
;(defn get-content! [url]
;  (do
;    (api/go driver url)
;    (api/wait-visible driver [{:class :title}])
;    (api/get-source driver)))
;
;
;





