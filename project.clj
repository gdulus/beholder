(defproject beholder "0.1.0-SNAPSHOT"
  :description "API documentation search engine with microservice and K8S first approach."
  :license {:name "MIT License" :url "https://opensource.org/licenses/MIT"}
  :dependencies [
                 ;; -------------- base -----------------------------------
                 [org.clojure/clojure "1.11.1"]
                 [prismatic/schema "1.2.1"]
                 ;; -------------- persistence ---------------------------
                 [cc.qbits/spandex "0.7.10"]
                 ;; -------------- web -----------------------------------
                 [etaoin "0.4.6"]
                 [compojure "1.6.3"]
                 [ring/ring-defaults "0.3.3"]
                 [ring-json-response "0.2.0"]
                 [metosin/ring-http-response "0.9.3"]
                 [selmer "1.12.50"]
                 [environ "1.2.0"]
                 ;; --------------- logging ------------------------------
                 [org.apache.logging.log4j/log4j-api "2.17.2"]
                 [org.apache.logging.log4j/log4j-core "2.17.2"]
                 [org.apache.logging.log4j/log4j-1.2-api "2.17.2"]
                 [org.clojure/tools.logging "1.2.4"]
                 ]
  :plugins [
            ; make sure to change intellij repl run options to "run it with lein"
            [lein-environ "1.2.0"]
            [lein-ring "0.12.6"]
            [lein-eftest "0.5.9"]
            ]
  :repl-options {:init-ns beholder.core}
  :ring {:handler beholder.core/app
         :nrepl   {:start? true :port 41111}})
