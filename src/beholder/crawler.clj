(ns beholder.crawler
  (:require [beholder.repository :as r]
            [beholder.model :as m]
            [etaoin.api :as api]))

(def ^:private driver (api/firefox {:headless true}))

(defn get-content! [url]
  (do
    (api/go driver url)
    (api/wait-visible driver [{:class :title}])
    (api/get-source driver)))








