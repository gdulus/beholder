(ns integration.es-repository-test
  (:require [beholder.model :as m]
            [beholder.repositories.es :as r]
            [clj-test-containers.core :as tc]
            [clojure.test :refer :all]))

(defn- start-container []
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

(defn- build-mocked-es-config [container]
  {:hosts [(str "http://" (:host container) ":" (get (:mapped-ports container) 9200))]})

(defn- wait []
  (Thread/sleep 1000))

; -------------------------------------------------------------

(deftest test-crud-operations

  (let [container (start-container)]
    (with-redefs [beholder.repositories.es/config (fn [] (build-mocked-es-config container))]

      (testing "When no ServiceDocumentation entries get-service-documentation! should return nil"
        (is (some? (r/create-indexes!)))
        (is (nil? (r/get-service-documentation! 123)))
        (is (some? (r/delete-indexes!))))

      (testing "When saved ServiceDocumentation get-service-documentation! should return it"
        (let [service-docs (m/->ServiceDocumentation "123" "openapi doc" "asyncapi doc")]
          (is (some? (r/create-indexes!)))
          (is (= service-docs (r/save-service-documentation! service-docs)))
          (wait)
          (is (= service-docs (r/get-service-documentation! "123")))
          (is (some? (r/delete-indexes!)))))

      ; -----------

      (testing "When no ServiceConfig entries get-service-config should return nil"
        (is (some? (r/create-indexes!)))
        (is (nil? (r/get-service-config 123)))
        (is (some? (r/delete-indexes!))))

      (testing "When saved ServiceConfig get-service-config should return it"
        (let [service-config (m/->ServiceConfig "123" "path/openapi" "path/asyncapi" "my team" "path/repo" "my description")]
          (is (some? (r/create-indexes!)))
          (is (= service-config (r/save-service-config! service-config)))
          (wait)
          (is (= service-config (r/get-service-config "123")))
          (is (some? (r/delete-indexes!)))))

      (testing "When saved ServiceConfig list-service-configs should return without duplicates"
        (let [sc1v1 (m/->ServiceConfig "1" "path/openapi1" "path/asyncapi1" "my team1" "path/repo1" "my description1")
              sc1v2 (m/->ServiceConfig "1" "path/openapi1v2" "path/asyncapi1v2" "my team1v2" "path/repo1v2" "my description1v2")
              sc2v1 (m/->ServiceConfig "2" "path/openapi2" "path/asyncapi2" "my team2" "path/repo2" "my description2")]
          (is (some? (r/create-indexes!)))

          (is (= sc1v1 (r/save-service-config! sc1v1)))
          (wait)
          (is (= 1 (count (r/list-service-configs))))

          (is (= sc2v1 (r/save-service-config! sc2v1)))
          (wait)
          (is (= 2 (count (r/list-service-configs))))

          (is (= sc1v2 (r/save-service-config! sc1v2)))
          (wait)
          (is (= 2 (count (r/list-service-configs))))
          (is (= sc1v2 (first (filter #(= (:serviceId %) "1") (r/list-service-configs)))))
          (is (= sc2v1 (first (filter #(= (:serviceId %) "2") (r/list-service-configs)))))

          (is (some? (r/delete-indexes!)))))

      ; -----------

      (testing "When save-beholder-config! executes successfully should return saved BeholderConfig"
        (is (some? (r/create-indexes!)))
        (is (= (m/->BeholderConfig ["namespace_1" "namespace_2" "namespace_3"],
                                   "openApiLabel_1"
                                   "my/path/1"
                                   "asyncApiLabel_1"
                                   "my/path/2")
               (r/save-beholder-config! (m/->BeholderConfig ["namespace_1" "namespace_2" "namespace_3"],
                                                            "openApiLabel_1"
                                                            "my/path/1"
                                                            "asyncApiLabel_1"
                                                            "my/path/2"))))
        (is (some? (r/delete-indexes!))))

      (testing "When no BeholderConfig entries get-beholder-config! should return nil"
        (is (some? (r/create-indexes!)))
        (is (nil? (r/get-beholder-config!)))
        (is (some? (r/delete-indexes!))))

      (testing "When one BeholderConfig entry get-beholder-config! should return it"
        (is (some? (r/create-indexes!)))
        (is (some? (r/save-beholder-config! (m/->BeholderConfig ["namespace_1" "namespace_2" "namespace_3"]
                                                                "openApiLabel_1"
                                                                "my/path/1"
                                                                "asyncApiLabel_1"
                                                                "my/path/2"))))
        (wait)
        (is (= (m/->BeholderConfig ["namespace_1" "namespace_2" "namespace_3"]
                                   "openApiLabel_1"
                                   "my/path/1"
                                   "asyncApiLabel_1"
                                   "my/path/2")
               (r/get-beholder-config!)))
        (is (some? (r/delete-indexes!))))

      (testing "When more than one BeholderConfig entries get-beholder-config! should return the latest one"
        (is (some? (r/create-indexes!)))
        (is (some? (r/save-beholder-config! (m/->BeholderConfig ["namespace_1" "namespace_2" "namespace_3"]
                                                                "openApiLabel_1"
                                                                "my/path/1"
                                                                "asyncApiLabel_1"
                                                                "my/async/1"))))
        (is (some? (r/save-beholder-config! (m/->BeholderConfig ["namespace_1" "namespace_2" "namespace_3"]
                                                                "openApiLabel_2"
                                                                "my/path/2"
                                                                "asyncApiLabel_2"
                                                                "my/async/2"))))
        (is (some? (r/save-beholder-config! (m/->BeholderConfig ["namespace_1" "namespace_2" "namespace_3"]
                                                                "openApiLabel_3"
                                                                "my/path/3"
                                                                "asyncApiLabel_3"
                                                                "my/async/3"))))
        (is (some? (r/save-beholder-config! (m/->BeholderConfig ["namespace_1" "namespace_2" "namespace_3"]
                                                                "openApiLabel_4"
                                                                "my/path/4"
                                                                "asyncApiLabel_4"
                                                                "my/async/4"))))
        (wait)
        (is (= (m/->BeholderConfig ["namespace_1" "namespace_2" "namespace_3"]
                                   "openApiLabel_4"
                                   "my/path/4"
                                   "asyncApiLabel_4"
                                   "my/async/4")
               (r/get-beholder-config!)))
        (is (some? (r/delete-indexes!)))))

    (tc/stop! container)
    (tc/perform-cleanup!)))

