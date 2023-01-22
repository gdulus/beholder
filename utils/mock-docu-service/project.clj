(defproject mock-docu-service "1.0.0"
            :dependencies [
                           [org.clojure/clojure "1.11.1"]
                           [compojure "1.6.3"]
                           [ring/ring-defaults "0.3.3"]
                           [ring-json-response "0.2.0"]
                           [metosin/ring-http-response "0.9.3"]]

            :plugins [[lein-ring "0.12.6"]]

            :ring {:handler mock.core/app
                   :nrepl   {:start? true :port 41111}}

            :repl-options {:init-ns mock.core})
