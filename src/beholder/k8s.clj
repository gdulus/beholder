(ns beholder.k8s
  (:require [kubernetes-api.core :as k8s]
            [environ.core :refer [env]]
            [beholder.model :as m]
            [schema.core :as s])
  (:import (beholder.model K8SService)))

(def ^:private k8s (delay (k8s/client (env :k8s-apiserver)
                                      {:insecure? true
                                       :token     (env :k8s-token)})))

(defn- load-k8s-services []
  (k8s/invoke @k8s {:kind    :Service
                    :action  :list
                    :request {:namespace (env :k8s-namespace)}}))

(defn- https-callable-k8s-service? [list-resource]
  (some? (get-in list-resource [:spec :selector :app])))

(defn- k8s-services->K8SService [list-resource]
  (m/->K8SService
    (get-in list-resource [:metadata :name])
    (get-in list-resource [:metadata :namespace])
    (str "http://" (get-in list-resource [:spec :selector :app]))
    (->>
      (get-in list-resource [:spec :ports])
      (first)
      (:port))
    ))

(defn- validate-K8SService [service]
  (s/validate K8SService service))

; ----------------------------------------------------------------

(defn list-services []
  (->>
    (load-k8s-services)
    (:items)
    (filter https-callable-k8s-service?)
    (map k8s-services->K8SService)
    (map validate-K8SService)
    ))




