#!/usr/bin/env bb

(require '[bb-dialog.core :refer :all]
         '[babashka.process :refer [shell]])

(def commands {:init-dev         "Init dev env"
               :build-beholder   "Build beholder docker image"
               :deploy-beholder  "Deploy beholder to local K8S"
               :start-beholder   "Start beholder app"
               :build-mock       "Build mock service docker image"
               :deploy-mock      "Deploy mock to local K8S"
               :deploy-generator "Deploy asyncapi generator to local K8S"})

(defn safe-shell
  ([cmd] (try
           (shell cmd)
           (catch Exception _)))
  ([options cmd] (try
                   (shell options cmd)
                   (catch Exception _))))

(defn what-to-do-dialog []
  (menu "BEHOLDER DEV TOOLS" "What do you want to do?" commands))

; -----------------------------------------------------------------------

(defn print-help []
  (println "Usage : ./devtools.clj (command?) ")
  (println "Supported commands :")
  (run! (fn [[k v]] (println (str "- " (name k) " -> " v))) commands))

(defn beholder-clean []
  (println "-----------------------------")
  (println "removing beholder from docker repo")
  (println "-----------------------------")
  (safe-shell "docker rm beholder")
  (safe-shell "docker image rm beholder:dev"))

(defn k8s-clean []
  (println "-----------------------------")
  (println "removing beholder from k8s")
  (println "-----------------------------")
  (safe-shell "kubectl delete deployment beholder")
  (safe-shell "kubectl delete service beholder"))

(defn beholder-build []
  (println "-----------------------------")
  (println "building beholder docker image")
  (println "-----------------------------")
  (shell "lein clean")
  (shell "lein ring uberjar")
  (shell "docker build . -t beholder:dev -f Dockerfile.dev"))

(defn mock-docu-service-build []
  (println "-----------------------------")
  (println "building mock service")
  (println "-----------------------------")
  (shell {:dir "utils/mock-docu-service"} "lein clean")
  (shell {:dir "utils/mock-docu-service"} "lein ring uberjar")
  (shell {:dir "utils/mock-docu-service"} "docker build . -t mock-docu-service:dev -f Dockerfile"))


(defmulti command identity)
(defmethod command :default [_] (print-help))
(defmethod command :help [_] (print-help))
(defmethod command :init-dev [_] (do
                                   (shell "clear")
                                   (println "-----------------------------")
                                   (println "initializing dev env")
                                   (println "-----------------------------")
                                   (println "MAKE SURE TO RUN:")
                                   (println "eval $(minikube docker-env)")
                                   (println "-----------------------------")
                                   (shell "minikube start --driver=docker")
                                   (safe-shell "kubectl proxy --port=8080 &")))

(defmethod command :build-beholder [_] (do
                                         (shell "clear")
                                         (beholder-build)))

(defmethod command :deploy-beholder [_] (do
                                          (shell "clear")
                                          (k8s-clean)
                                          (shell "sleep 3")
                                          (beholder-clean)
                                          (beholder-build)
                                          (shell "kubectl create deployment beholder --image=beholder:dev")
                                          (shell "kubectl expose deployment beholder --type=LoadBalancer --port=3000")
                                          (shell "sleep 3")
                                          (println "-----------------------------")
                                          (println "beholder can be found under url: ")
                                          (shell "minikube service beholder --url")))

(defmethod command :start-beholder [_] (do
                                         (shell "clear")
                                         (println "-----------------------------")
                                         (println "starting beholder app")
                                         (println "-----------------------------")
                                         (shell "lein clean")
                                         (shell "lein ring server")))

(defmethod command :build-mock [_] (do
                                     (shell "clear")
                                     (mock-docu-service-build)))

(defmethod command :deploy-mock [_] (do
                                      (shell "clear")
                                      (println "-----------------------------")
                                      (println "removing mock from docker repo")
                                      (println "-----------------------------")
                                      (safe-shell "docker rm mock-docu-service")
                                      (safe-shell "docker image rm mock-docu-service:dev")

                                      (println "-----------------------------")
                                      (println "removing mocks from k8s")
                                      (println "-----------------------------")
                                      (safe-shell "kubectl delete deployment mock-docu-openapi")
                                      (safe-shell "kubectl delete service mock-docu-openapi")
                                      (safe-shell "kubectl delete deployment mock-docu-asyncapi")
                                      (safe-shell "kubectl delete service mock-docu-asyncapi")
                                      (safe-shell "kubectl delete deployment mock-docu-both")
                                      (safe-shell "kubectl delete service mock-docu-both")

                                      (mock-docu-service-build)

                                      (println "-----------------------------")
                                      (println "deploying mocks to k8s")
                                      (println "-----------------------------")
                                      (shell "kubectl create deployment mock-docu-openapi --image=mock-docu-service:dev")
                                      (shell "kubectl expose deployment mock-docu-openapi --type=LoadBalancer --port=3000")
                                      (shell "kubectl label service mock-docu-openapi openapi=true")

                                      (shell "kubectl create deployment mock-docu-asyncapi --image=mock-docu-service:dev")
                                      (shell "kubectl expose deployment mock-docu-asyncapi --type=LoadBalancer --port=3000")
                                      (shell "kubectl label service mock-docu-asyncapi asyncapi=true")

                                      (shell "kubectl create deployment mock-docu-both --image=mock-docu-service:dev")
                                      (shell "kubectl expose deployment mock-docu-both --type=LoadBalancer --port=3000")
                                      (shell "kubectl label service mock-docu-both openapi=true")
                                      (shell "kubectl label service mock-docu-both asyncapi=true")))


(defmethod command :deploy-generator [_] (do
                                           (shell "clear")

                                           (println "-----------------------------")
                                           (println "pulling asyncapi-generator-service image")
                                           (println "-----------------------------")
                                           (shell " docker pull gdulus/asyncapi-generator-service")

                                           (println "-----------------------------")
                                           (println "removing asyncapi-generator-service from k8s")
                                           (println "-----------------------------")
                                           (safe-shell "kubectl delete deployment asyncapi-generator-service")
                                           (safe-shell "kubectl delete service asyncapi-generator-service")

                                           (println "-----------------------------")
                                           (println "deploying asyncapi-generator-service to k8s")
                                           (println "-----------------------------")
                                           (shell "kubectl create deployment asyncapi-generator-service --image=gdulus/asyncapi-generator-service:latest")
                                           (shell "kubectl expose deployment asyncapi-generator-service --type=LoadBalancer --port=3000")))


; -----------------------------------------------------------------------

(try
  (let [[cmd] *command-line-args*]
    (as->
      cmd v
      (or (keyword v) (what-to-do-dialog))
      (command v))
    (System/exit 0))
  (catch Exception e
    (shell "clear")
    (System/exit 0)))