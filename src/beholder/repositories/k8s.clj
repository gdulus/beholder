(ns beholder.repositories.k8s
  (:require [beholder.model :as m]
            [beholder.repositories.es :as es]
            [environ.core :refer [env]]
            [kubernetes-api.core :as k8s]
            [schema.core :as s]
            [taoensso.timbre :as log])
  (:import (beholder.model KubernetesService)))

(def ^:private k8s (delay (k8s/client (env :k8s-apiserver)
                                      {:insecure? true
                                       :token     (env :k8s-token)})))

(defn- load-k8s-services [n]
  (log/info "Retrieving list of K8S services from namespace: " n)
  (k8s/invoke @k8s {:kind    :Service
                    :action  :list
                    :request {:namespace n}}))

(defn- valid-service? [openapi-label asyncapi-label list-resource]
  (let [labels (get-in list-resource [:metadata :labels])]
    (and (some? (get-in list-resource [:spec :selector :app]))
         (or (contains? labels openapi-label)
             (contains? labels asyncapi-label)))))

(defn- get-port [list-resource]
  (->> (get-in list-resource [:spec :ports])
       (first)
       (:port)))

(defn- build-url [list-resource]
  (let [app-name (get-in list-resource [:spec :selector :app])
        override (env (keyword app-name))
        domain (or override app-name)
        port (when-not override (str ":" (get-port list-resource)))]
    (str "http://" domain port)))

(defn- response->KubernetesService [list-resource openapi-label asyncapi-label]
  (let [labels (get-in list-resource [:metadata :labels])]
    (m/->KubernetesService
      (get-in list-resource [:metadata :uid])
      (get-in list-resource [:metadata :name])
      (get-in list-resource [:metadata :namespace])
      (build-url list-resource)
      labels
      (contains? labels openapi-label)
      (contains? labels asyncapi-label))))

(defn- validate-KubernetesService [service]
  (s/validate KubernetesService service))

; ----------------------------------------------------------------

(defn list-services! []
  (let [config (es/get-beholder-config!)
        openapi-label (m/get-openapi-label config)
        asyncapi-label (m/get-asyncapi-label config)
        namespaces (m/get-namespaces config)]
    (->>
      (map load-k8s-services namespaces)
      (mapcat :items)
      (filter #(valid-service? openapi-label asyncapi-label %))
      (map #(response->KubernetesService % openapi-label asyncapi-label))
      (map validate-KubernetesService))))

(defn get-service! [id]
  (->>
    (list-services!)
    (filter #(= id (:id %)))
    (first)))



