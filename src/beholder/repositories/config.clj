(ns beholder.repositories.config
  (:require [beholder.model :as m]
            [clojure.string :as str]
            [environ.core :refer [env]]
            [qbits.spandex :as e]
            [schema.core :as s]
            [taoensso.timbre :refer [spy]])
  (:import (beholder.model BeholderConfig)
           (beholder.model ServiceConfig)))

(defn config []
  (spy :info
       "Creating ElasticSearch connection to hosts"
       {:hosts (str/split (env :es-hosts) #",")}))

(def ^:private c (delay (e/client (config))))

; ----------------------------------------------------------------
; Indexes
; ----------------------------------------------------------------

(defn delete-indexes! []
  (e/request @c {:method :delete :url [:beholder-config]})
  (e/request @c {:method :delete :url [:services-config]}))


(defn create-indexes! []
  (e/request @c {:method :put :url [:beholder-config]})
  (e/request @c {:method :put :url [:services-config]}))

; ----------------------------------------------------------------
; Basic operations
; ----------------------------------------------------------------

(defn- get-by-id [url]
  "Safe return of the data. When searching by id ES throw 404 when not found.
  This method prevents 404 exception but allows propagation of other issues"
  (try
    (-> (e/request @c {:url url})
        (get-in [:body :_source]))
    (catch Exception e
      (if (= (:status (ex-data e)) 404)
        nil
        (throw e)))))

(defn get-by-id-and-validate [url model-class ->model-record]
  (when-let [conf (get-by-id url)]
    (s/validate model-class (->model-record conf))))

(defn validate-and-save [url model-class model-data]
  (->>
    (s/validate model-class model-data)
    (assoc {:method :put :url url} :body)
    (e/request @c))
  model-data)

; ----------------------------------------------------------------
; BeholderConfig
; ----------------------------------------------------------------

(def ^:private ^:const beholder-config-id (str "beholder-config-id-00dcc92e"))

(defn get-beholder-config! []
  (get-by-id-and-validate [:beholder-config :_doc beholder-config-id]
                          BeholderConfig
                          m/map->BeholderConfig))

(defn save-beholder-config! [^BeholderConfig config]
  (validate-and-save [:beholder-config :_doc beholder-config-id]
                     BeholderConfig
                     config))

; ----------------------------------------------------------------
; ServiceConfig
; ----------------------------------------------------------------

(defn list-service-configs []
  (as->
    (e/request @c {:url [:services-config :_search]}) v
    (get-in v [:body :hits :hits])
    (map :_source v)
    (map m/map->ServiceConfig v)
    (map #(s/validate ServiceConfig %) v)))


(defn get-service-config [id]
  (get-by-id-and-validate [:services-config :_doc id]
                          ServiceConfig
                          m/map->ServiceConfig))

(defn save-service-config! [^BeholderConfig config]
  (validate-and-save [:services-config :_doc (:serviceId config)]
                     ServiceConfig
                     config))
