(ns beholder.repository
  (:require [beholder.model :as m]
            [qbits.spandex :as e]
            [schema.core :as s])
  (:import (beholder.model BeholderConfig)))

(defn config [] {:hosts ["http://127.0.0.1:9200"]})
(def ^:private c (delay (e/client (config))))

; ----------------------------------------------------------------

(defn delete-indexes! []
  (e/request @c {:method :delete :url [:config]}))


(defn create-indexes! []
  (e/request @c {:method :put :url [:config] :body {:mappings {:properties {:timestamp {:type :long}}}
                                                    :settings {:index {:sort.field :timestamp
                                                                       :sort.order :desc}}}}))

; ----------------------------------------------------------------
; BeholderConfig
; ----------------------------------------------------------------

(defn update-config! [^BeholderConfig config]
  (->>
    (s/validate BeholderConfig config)
    (merge {:timestamp (System/currentTimeMillis)})
    (assoc {:method :post :url [:config :_doc]} :body)
    (e/request @c))
  config)

(defn get-config! []
  (as->
    (e/request @c {:url  [:config :_search]
                   :body {:sort [{:timestamp "desc"}]
                          :from 0 :size 1}}) v
    (get-in v [:body :hits :hits])
    (first v)
    (:_source v)
    (dissoc v :timestamp)
    (if (some? v)
      (m/map->BeholderConfig v))))
