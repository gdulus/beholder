(ns beholder.repository-test
  (:require [beholder.model :as m]
            [beholder.repositories.config :as r]
            [clj-test-containers.core :as tc]
            [clojure.test :refer :all]))

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

(defn build-mocked-es-config [container]
  {:hosts [(str "http://" (:host container) ":" (get (:mapped-ports container) 9200))]})

(deftest ^:eftest/synchronized ^:integration update-config!-test
  (let [container (start-container)]
    (with-redefs [beholder.repositories.config/config (fn [] (build-mocked-es-config container))]

      (testing "When update-config! executes successfully should return saved BeholderConfig"
        (is (some? (r/create-indexes!)))
        (is (= (m/->BeholderConfig ["namespace_1" "namespace_2" "namespace_3"], "openApiLabel_1", "my/path/1")
               (r/save-beholder-config! (m/->BeholderConfig ["namespace_1" "namespace_2" "namespace_3"], "openApiLabel_1", "my/path/1"))))
        (is (some? (r/delete-indexes!))))

      (testing "When no BeholderConfig entries get-config! should return nil"
        (is (some? (r/create-indexes!)))
        (is (nil? (r/get-beholder-config!)))
        (is (some? (r/delete-indexes!))))

      (testing "When one BeholderConfig entry get-config! should return it"
        (is (some? (r/create-indexes!)))
        (is (some? (r/save-beholder-config! (m/->BeholderConfig ["namespace_1" "namespace_2" "namespace_3"], "openApiLabel_1", "my/path/1"))))
        (Thread/sleep 1000)
        (is (= (m/->BeholderConfig ["namespace_1" "namespace_2" "namespace_3"], "openApiLabel_1", "my/path/1")
               (r/get-beholder-config!)))
        (is (some? (r/delete-indexes!))))

      (testing "When more than one BeholderConfig entries get-config! should return the latest one"
        (is (some? (r/create-indexes!)))
        (is (some? (r/save-beholder-config! (m/->BeholderConfig ["namespace_1" "namespace_2" "namespace_3"], "openApiLabel_1", "my/path/1"))))
        (is (some? (r/save-beholder-config! (m/->BeholderConfig ["namespace_1" "namespace_2" "namespace_3"], "openApiLabel_2", "my/path/2"))))
        (is (some? (r/save-beholder-config! (m/->BeholderConfig ["namespace_1" "namespace_2" "namespace_3"], "openApiLabel_3", "my/path/3"))))
        (is (some? (r/save-beholder-config! (m/->BeholderConfig ["namespace_1" "namespace_2" "namespace_3"], "openApiLabel_4", "my/path/4"))))
        (Thread/sleep 1000)
        (is (= (m/->BeholderConfig ["namespace_1" "namespace_2" "namespace_3"], "openApiLabel_4", "my/path/4")
               (r/get-beholder-config!)))
        (is (some? (r/delete-indexes!)))))

    (tc/stop! container)
    (tc/perform-cleanup!)))

