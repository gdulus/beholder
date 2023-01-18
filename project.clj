(defproject beholder "0.1.0"
  :description "Documentation search engine with K8S first approach."
  :license {:name "MIT License" :url "https://opensource.org/licenses/MIT"}
  :dependencies [
                 ;; -------------- base -----------------------------------
                 [org.clojure/clojure "1.11.1"]
                 [prismatic/schema "1.4.1"]
                 [jarohen/chime "0.3.3"]
                 ;; -------------- k8s -----------------------------------
                 [nubank/k8s-api "0.1.2"]
                 [io.fabric8/kubernetes-client "6.3.1"]
                 ;; -------------- persistence ---------------------------
                 [cc.qbits/spandex "0.7.10"]
                 ;; -------------- web -----------------------------------
                 [etaoin "1.0.39"]
                 [compojure "1.6.3"]
                 [ring/ring-defaults "0.3.4"]
                 [ring-json-response "0.2.0"]
                 [metosin/ring-http-response "0.9.3"]
                 [selmer "1.12.55"]
                 [environ "1.2.0"]
                 [clj-soup/clojure-soup "0.1.3"]
                 [clj-http "3.12.3"]
                 [org.clojure/data.json "2.4.0"]

                 ;; --------------- testing ------------------------------
                 [clj-test-containers "0.7.4"]
                 [ring/ring-mock "0.4.0"]

                 ;; --------------- logging ------------------------------
                 [com.taoensso/timbre "6.0.4"]
                 ]
  :plugins [
            [lein-environ "1.2.0"]                          ; make sure to change intellij repl run options to "run it with lein"
            [lein-ring "0.12.6"]
            [lein-eftest "0.6.0"]]
  :repl-options {:init-ns beholder.main}
  :ring {:handler beholder.main/app
         :nrepl   {:start? true :port 41111}})
