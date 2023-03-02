(ns beholder.repositories.es
  (:require [beholder.model :as m]
            [clojure.instant :as instant]
            [clojure.string :as str]
            [environ.core :refer [env]]
            [qbits.spandex :as e]
            [schema.core :as s]
            [beholder.utils.log :as log])
  (:import (beholder.model BeholderConfig K8SService K8SServiceConfig ServiceDocumentation)))

(defn config []
  (log/spy :info
           "ElasticSearch connection to hosts"
           {:hosts (str/split (env :es-hosts) #",")}))

(def ^:private c (delay (e/client (config))))

; ----------------------------------------------------------------
; Indexes
; ----------------------------------------------------------------

(defn delete-indexes! []
  (e/request @c {:method :delete :url [:beholder-config]})
  (e/request @c {:method :delete :url [:k8s-services]})
  (e/request @c {:method :delete :url [:services-documentation]}))

(defn create-indexes! []
  (e/request @c {:method :put :url [:beholder-config]})
  (e/request @c {:method :put :url [:k8s-services]})
  (e/request @c {:method :put :url [:services-documentation]}))

(defn- get-by-id [url]
  (try
    (-> (e/request @c {:url url})
        (get-in [:body :_source]))
    (catch Exception e
      (if (= (:status (ex-data e)) 404)
        nil
        (throw e)))))

; ----------------------------------------------------------------
; Basic CRUD operations
; ----------------------------------------------------------------

(defn save [url model-class model-data]
  (->>
   (s/validate model-class model-data)
   (assoc {:method :put :url url} :body)
   (e/request @c))
  model-data)

(defn get [url model-class ->model-record]
  (when-let [conf (not-empty (get-by-id url))]
    (s/validate model-class (->model-record conf))))

(defn delete [index id]
  (e/request @c {:method :delete :url [index :_doc id]}))

(defn list [index model-class ->model-record]
  (try
    (as->
     (e/request @c {:url [index :_search]}) v
      (get-in v [:body :hits :hits])
      (map :_source v)
      (map ->model-record v)
      (map #(s/validate model-class %) v))
    (catch Exception e
      (log/error "There was an issue when listing resources" e)
      [])))

; ----------------------------------------------------------------
; BeholderConfig
; ----------------------------------------------------------------

(def ^:private ^:const beholder-config-id (str "beholder-config-id-00dcc92e"))

;; TODO update tests
(defn get-beholder-config! []
  (if-let [config (get [:beholder-config :_doc beholder-config-id]
                       BeholderConfig
                       m/map->BeholderConfig)]
    config
    (m/map->BeholderConfig {:namespaces []})))

(defn save-beholder-config! [config]
  (save [:beholder-config :_doc beholder-config-id]
        BeholderConfig
        config))

; ----------------------------------------------------------------
; K8SService
; ----------------------------------------------------------------

(defn- doc->K8SService [doc]
  (->>
   (:creationTimestamp doc)
   (instant/read-instant-date)
   (assoc doc :creationTimestamp)
   m/map->K8SService))

(defn save-k8s-service! [config]
  (save [:k8s-services :_doc (:id config)]
        K8SService
        config))

(defn get-k8s-service! [id]
  (get [:k8s-services :_doc id]
       K8SService
       doc->K8SService))

(defn list-k8s-service! []
  (list :k8s-services K8SService doc->K8SService))

(defn delete-k8s-service! [id]
  (delete :k8s-services id))

; ----------------------------------------------------------------
; ServiceConfig
; ----------------------------------------------------------------

(defn save-service-config! [id config]
  (save [:services-config :_doc (:serviceId config)]
        K8SServiceConfig
        config))

(defn get-service-config! [id]
  (get [:services-config :_doc id]
       K8SServiceConfig
       m/map->K8SServiceConfig))

(defn list-service-configs! []
  (list :services-config K8SServiceConfig m/map->K8SServiceConfig))

; ----------------------------------------------------------------
; ServiceDocumentation
; ----------------------------------------------------------------

(defn get-service-documentation! [id]
  (when-let [raw (not-empty (get-by-id [:services-documentation :_doc id]))]
    (let [async-doc (m/map->AsyncApiDocumentation (:asyncApiDoc raw))]
      (m/map->ServiceDocumentation (assoc raw :asyncApiDoc async-doc)))))

(defn save-service-documentation! [^ServiceDocumentation doc]
  (save [:services-documentation :_doc (:serviceId doc)]
        ServiceDocumentation
        doc))
