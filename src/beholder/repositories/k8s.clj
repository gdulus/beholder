(ns beholder.repositories.k8s
  (:require [beholder.model :as m]
            [beholder.repositories.es :as es]
            [environ.core :refer [env]]
            [kubernetes-api.core :as k8s]
            [schema.core :as s]
            [beholder.utils.log :as log])
  (:import (beholder.model K8SService)))

(def ^:private k8s (delay (k8s/client (env :k8s-apiserver)
                                      {:insecure? true
                                       :token     (env :k8s-token)})))

(defn- load-k8s-services [n]
  (log/info "Retrieving list of K8S services from namespace: " n)
  (k8s/invoke @k8s {:kind    :Service
                    :action  :list
                    :request {:namespace n}}))

(defn- valid-service? [list-resource]
  (some? (get-in list-resource [:spec :selector :app])))

(defn- doc-enabled? [k8s-service]
  (or (:openApiEnabled? k8s-service)
      (:asyncApiEnabled? k8s-service)))

(defn- get-port [list-resource]
  (->> (get-in list-resource [:spec :ports])
       (first)
       (:port)))

(defn- build-url [list-resource]
  (let [app-name (get-in list-resource [:spec :selector :app])
        port (get-port list-resource)]
    (str "http://" app-name ":" port)))

(defn- response->K8SService [list-resource openapi-label asyncapi-label]
  (let [labels (get-in list-resource [:metadata :labels])]
    (m/map->K8SService {
                        :id               (get-in list-resource [:metadata :uid])
                        :name             (get-in list-resource [:metadata :name])
                        :namespace        (get-in list-resource [:metadata :namespace])
                        :url              (build-url list-resource)
                        :labels           labels
                        :openApiEnabled?  (contains? labels openapi-label)
                        :asyncApiEnabled? (contains? labels asyncapi-label)
                        :resourceVersion  (Integer/parseInt (get-in list-resource [:metadata :resourceVersion]))
                        :lastUpdated      nil})))

(defn- validate-K8SService [service]
  (s/validate K8SService service))

; ----------------------------------------------------------------

(defn fetch-services! []
  (let [config (es/get-beholder-config!)
        openapi-label (m/get-openapi-label config)
        asyncapi-label (m/get-asyncapi-label config)
        namespaces (m/get-namespaces config)]
    (->>
     (map load-k8s-services namespaces)
     (mapcat :items)
     (filter valid-service?)
     (map #(response->K8SService % openapi-label asyncapi-label))
     (map validate-K8SService)
     (filter doc-enabled?))))
