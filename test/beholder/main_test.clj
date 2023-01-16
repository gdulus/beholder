(ns beholder.main-test
  (:require [beholder.main :as routes]
            [beholder.model :as m]
            [clj-test-containers.core :as tc]
            [clojure.test :refer :all]
            [ring.mock.request :as mock]))

(defn start-container []
  (->
    (tc/create {:image-name    "docker.elastic.co/elasticsearch/elasticsearch:7.17.6"
                :exposed-ports [9200]
                :env-vars      {"discovery.type" "single-node"
                                "ES_JAVA_OPTS"   "-Xms750m -Xmx750m"}
                :wait-for      {:wait-strategy   :log
                                :message         "\"message\": \"started\""
                                :startup-timeout 15}
                })
    (tc/start!)))


(deftest ^:unit your-handler-test
  (with-redefs [beholder.repositories.config/get-beholder-config! (fn [] (m/map->BeholderConfig {}))
                beholder.repositories.config/get-service-config (fn [id] (m/map->ServiceConfig {:openApiPath "/gdulus/beholder/main/test/empty.json"}))
                beholder.repositories.k8s/get-service! (fn [id] (m/map->KubernetesService {:url "https://raw.githubusercontent.com"}))]
    (is (= (routes/app-routes (mock/request :get "/service/12345678/openapi"))
           {:status  200
            :body    "{}"}))))

