(ns beholder.repositories.k8s
  (:require [beholder.model :as m]
            [beholder.repositories.config :as c]
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

(defn- https-callable-k8s-service? [openapi-label list-resource]
  (and (some? (get-in list-resource [:spec :selector :app]))
       (contains? (get-in list-resource [:metadata :labels]) (keyword openapi-label))))

(defn- get-port [list-resource]
  (->> (get-in list-resource [:spec :ports])
       (first)
       (:port)))

(defn- response->KubernetesService [list-resource]
  (m/->KubernetesService
    (get-in list-resource [:metadata :uid])
    (get-in list-resource [:metadata :name])
    (get-in list-resource [:metadata :namespace])
    (str "http://" (get-in list-resource [:spec :selector :app]) ":" (get-port list-resource))
    (get-in list-resource [:metadata :labels])
    ))

(defn- validate-KubernetesService [service]
  (s/validate KubernetesService service))

; ----------------------------------------------------------------

(defn list-services! []
  (let [config (c/get-beholder-config!)
        openapi-label (log/spy :info "OpenaAPI label" (m/get-openapi-label config))]
    (->>
      (m/get-namespaces config)
      (log/spy :info "Got namespaces")
      (map load-k8s-services)
      (mapcat :items)
      (filter #(https-callable-k8s-service? openapi-label %))
      (map response->KubernetesService)
      (map validate-KubernetesService))))

(defn get-service! [id]
  (->>
    (list-services!)
    (filter #(= id (:id %)))
    (first)))



