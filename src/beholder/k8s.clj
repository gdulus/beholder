(ns beholder.k8s
  (:require [kubernetes-api.core :as k8s]))


(def k8s (k8s/client "http://localhost:8080" {}))
(k8s/explore k8s)

(k8s/invoke k8s {:kind :Service
                 :action :list
                 :request {:namespace "kube-system"}})


(k8s/info k8s {:kind :Pod
                 :action :list
                 :namespace "default"})
