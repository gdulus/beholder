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
                               :startup-timeout 120}})
   (tc/start!)))

(defn build-mocked-es-config [container]
  {:hosts [(str "http://" (:host container) ":" (get (:mapped-ports container) 9200))]})

(defn sleep []
  (Thread/sleep 1000))

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
        (testing "flow save -> get"
          (let [srv (m/map->K8SService {:id "1"
                                        :name "test service"
                                        :namespace "default"
                                        :url "http://example.org"
                                        :labels {:a "1" :b "2"}
                                        :openApiEnabled? true
                                        :asyncApiEnabled? true
                                        :resourceVersion 666
                                        :lastUpdated nil})
                ; ---
                ; Inst returned by save and date returned by get differ by miliseconds
                ; First is using java api directly second uses istant/read-instant-date.
                ;    example diff:
                ;    save  #inst "2023-03-05T11:26:24.031-00:00"
                ;    get   #inst "2023-03-05T11:26:24.000-00:00"
                ; ---
                save-result (es/save-k8s-service! srv)
                last-updated-save (:lastUpdated save-result)
                srv-saved (assoc srv :lastUpdated last-updated-save)
                ; ----
                load-result (es/get-k8s-service! "1")
                last-updated-get (:lastUpdated load-result)
                srv-get (assoc srv :lastUpdated last-updated-get)]
            (is (= save-result srv-saved))
            (is (= load-result srv-get))))

        (testing "flow save, save -> list"
          (let [srv1 (m/map->K8SService {:id "2"
                                         :name "test service a"
                                         :namespace "default"
                                         :url "http://example.org"
                                         :labels {:a "1" :b "2"}
                                         :openApiEnabled? true
                                         :asyncApiEnabled? true
                                         :resourceVersion 666
                                         :lastUpdated nil})
                srv2 (m/map->K8SService {:id "3"
                                         :name "test service b"
                                         :namespace "default"
                                         :url "http://example.org"
                                         :labels {:a "1" :b "2"}
                                         :openApiEnabled? true
                                         :asyncApiEnabled? true
                                         :resourceVersion 666
                                         :lastUpdated nil})

                _ (es/save-k8s-service! srv1)
                _ (es/save-k8s-service! srv2)
                _ (sleep)
                result (es/list-k8s-service!)
                result-ids (map :id result)]
            (is (<= 2 (count result)))
            (is (.contains result-ids "2"))
            (is (.contains result-ids "3"))))

        (testing "flow save -> get -> delete -> get"
          (let [srv (m/map->K8SService {:id "4"
                                        :name "test service a"
                                        :namespace "default"
                                        :url "http://example.org"
                                        :labels {:a "1" :b "2"}
                                        :openApiEnabled? true
                                        :asyncApiEnabled? true
                                        :resourceVersion 666
                                        :lastUpdated nil})
                _ (es/save-k8s-service! srv)
                before-delete (es/get-k8s-service! "4")
                _ (es/delete-k8s-service! "4")
                _ (sleep)
                afetr-delete (es/get-k8s-service! "4")]
            (is (not (nil? before-delete)))
            (is (nil? afetr-delete))))))

;; ---
    (tc/stop! container)
    (tc/perform-cleanup!)))
