(ns beholder.repository
  (:require [qbits.spandex :as e]
            [schema.core :as s])
  (:import (beholder.model BeholderConfig)))

(def ^:private c (e/client {:hosts ["http://127.0.0.1:9200"]}))

; ----------------------------------------------------------------

(defn delete-indexes! []
  (try
    (e/request c {:method :delete :url [:config]})
    (catch Exception e (clojure.pprint/pprint e))))

(defn create-indexes! []
  (try
    (e/request c {:method :put :url [:config] :body {:mappings {:properties {:timestamp {:type :long}}}
                                                     :settings {:index {:sort.field :timestamp
                                                                        :sort.order :desc}}}})
    (catch Exception e (clojure.pprint/pprint e))))

; ----------------------------------------------------------------
; BeholderConfig
; ----------------------------------------------------------------

(defn update-config! [^BeholderConfig config]
  (->>
    (s/validate BeholderConfig config)
    (merge {:timestamp (System/currentTimeMillis)})
    (assoc {:method :post :url [:config :_doc]} :body)
    (e/request c)))

(defn get-config! ^BeholderConfig []
  (e/request c {:url  [:config :_search]
                :body {:sort [{:timestamp "desc"}]
                       :from 0
                       :size 1}}))

(get-config!)