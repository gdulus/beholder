(ns beholder.repositories.es-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [beholder.model :as m]
   [clj-test-containers.core :as tc]
   [beholder.repositories.es :as es]))

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
      (testing "BeholderConfig CRUD operations"
        (testing "flow save and get config"
          (let [conf (m/map->BeholderConfig {:namespaces ["n1" "n2"]
                                             :openApiLabel "openApiCustom"
                                             :openApiPath "/path/to/openapi"
                                             :asyncApiLabel "asyncApiCustom"
                                             :asyncApiPath "/path/to/asyncapi"})
                save-result (es/save-beholder-config! conf)
                load-result (es/get-beholder-config!)]
            (is (= save-result conf))
            (is (= load-result conf)))))
;; ----
;;
      (testing "K8SService CRUD operations"
        (testing "flow save and get"
          (let [conf (m/map->K8SService {:id "123"
                                         :name "test service"
                                         :namespace "default"
                                         :url "http://example.org"
                                         :labels {:a "1" :b "2"}
                                         :openApiEnabled? true
                                         :asyncApiEnabled? true
                                         :resourceVersion 666
                                         :lastUpdated nil})
                save-result (es/save-k8s-service! conf)
                load-result (es/get-k8s-service! "123")]
            (is (= save-result conf))
            (is (= load-result conf))))))
;; ---
    (tc/stop! container)
    (tc/perform-cleanup!)))
