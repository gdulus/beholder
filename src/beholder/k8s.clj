(ns beholder.k8s
  (:require [kubernetes-api.core :as k8s]))

; todos
; deploy to minikube image with swagger on the board
; deploy to minikube image with openapi on the board

; how to get service address
; - clusterIP

(def k8s (k8s/client "http://localhost:8080" {}))
(k8s/explore k8s)

(clojure.pprint/pprint (k8s/invoke k8s {:kind    :Service
                                        :action  :list
                                        :request {:namespace "default"}}))


(clojure.pprint/pprint (k8s/invoke k8s {:kind    :Service
                                        :action  :get
                                        :request {:name "hello-minikube" :namespace "default"}}))

(k8s/info k8s {:kind      :Service
               :action    :get})
