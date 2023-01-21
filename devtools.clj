#!/usr/bin/env bb

(require '[bb-dialog.core :refer :all]
         '[clojure.string :as str]
         '[babashka.process :refer [shell process check sh pipeline pb]])

(def commands {:init-dev        "Init dev env"
               :build-beholder  "Build beholder docker image"
               :deploy-beholder "Deploy beholder to local K8S"})

(defn safe-shell [cmd]
  (try
    (shell cmd)
    (catch Exception _)))

(defn what-to-do-dialog []
  (menu "BEHOLDER DEV TOOLS" "What do you want to do?" commands))

; -----------------------------------------------------------------------

(defn print-help []
  (println "Usage : ./dt.clj <command>")
  (println "Supported commands :")
  (run! (fn [[k v]] (println (str "- " (name k) " -> " v))) commands))

(defn beholder-clean []
  (println "-----------------------------")
  (println "cleaning docker")
  (println "-----------------------------")
  (safe-shell "docker rm beholder")
  (safe-shell "docker image rm beholder:dev"))

(defn k8s-clean []
  (println "-----------------------------")
  (println "cleaning minikube")
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



