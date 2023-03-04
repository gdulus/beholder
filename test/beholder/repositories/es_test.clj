(ns beholder.repositories.es-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [beholder.model :as m]
   [clj-test-containers.core :as tc]
   [beholder.repositories.es :as r]))

(defn start-container []
  (->
   (tc/create {:image-name    "docker.elastic.co/elasticsearch/elasticsearch:7.17.6"
               :exposed-ports [9200]
               :env-vars      {"discovery.type" "single-node"
                               "ES_JAVA_OPTS"   "-Xms750m -Xmx750m"}
               :wait-for      {:wait-strategy   :log
                               :message         "\"message\": \"started\""
                               :startup-timeout 15}})
   (tc/start!)))

(defn build-mocked-es-config [container]
  {:hosts [(str "http://" (:host container) ":" (get (:mapped-ports container) 9200))]})

(deftest ^:integration testing-es-respository
  (let [container (start-container)]
    (with-redefs [beholder.repositories.es/config (fn [] (build-mocked-es-config container))]
;; ----
      (testing "K8SService CRUD operations"
        (testing "testing save-k8s-service!"
          (let [srv (m/map->K8SService {})]
            (is (= true true))))))
;; ----
    (tc/stop! container)
    (tc/perform-cleanup!)))
