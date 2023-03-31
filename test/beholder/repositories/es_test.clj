(ns beholder.repositories.es-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [beholder.model :as m]
   [beholder.utils.date :as date]
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

(defn sleep 
  ([]
   (sleep 1))
  ([sec]
   (Thread/sleep (* sec 1000))))

(deftest ^:integration testing-es-respository
  (let [container (start-container)]
    (with-redefs [beholder.repositories.es/config (fn [] (build-mocked-es-config container))]

; ------------------------------------------
      
      (testing "AsyncJobRun CRUD operations"
        (testing "flow save -> get"
          (let [run         (m/map->AsyncJobRun {:name    "test-job"
                                                 :lastRun (date/now)})
                save-result (es/save-async-job-run! run)
                load-result (es/get-async-job-run! "test-job")]
            (is (= save-result run))
            (is (= load-result run)))))

; ------------------------------------------
      
      (testing "BeholderConfig CRUD operations"
        (testing "flow save -> get"
          (let [conf        (m/map->BeholderConfig {:namespaces    ["n1" "n2"]
                                                    :openApiLabel  "openApiCustom"
                                                    :openApiPath   "/path/to/openapi"
                                                    :asyncApiLabel "asyncApiCustom"
                                                    :asyncApiPath  "/path/to/asyncapi"})
                save-result (es/save-beholder-config! conf)
                load-result (es/get-beholder-config!)]
            (is (= save-result conf))
            (is (= load-result conf)))))
      
      ; ------------------------------------------      
      
      (testing "K8SServiceConfig CRUD operations"
        (testing "flow save -> get"
          (let [conf        (m/map->K8SServiceConfig {:serviceId    "1"
                                                      :openApiPath  "/path/to/openapi"
                                                      :asyncApiPath "/path/to/asyncapi"
                                                      :team         "team name"
                                                      :repo         "repo name"
                                                      :description  "desc"})
                save-result (es/save-k8s-service-config! conf)
                load-result (es/get-k8s-service-config! "1")]
            (is (= conf save-result))
            (is (= conf load-result))))
        (testing "flow save -> list"
          (let [conf        (m/map->K8SServiceConfig {:serviceId    "2"
                                                      :openApiPath  "/path/to/openapi"
                                                      :asyncApiPath "/path/to/asyncapi"
                                                      :team         "team name"
                                                      :repo         "repo name"
                                                      :description  "desc"})
                _           (es/save-k8s-service-config! conf)
                _           (sleep)
                list-result (es/list-k8s-service-configs!)]
            (is (= 2 (count list-result)))))
        (testing "flow save -> find"
          (let [conf        (m/map->K8SServiceConfig {:serviceId    "3"
                                                      :openApiPath  "/path/to/openapi"
                                                      :asyncApiPath "/path/to/asyncapi"
                                                      :team         "team name"
                                                      :repo         "repo name"
                                                      :description  "desc"})
                _           (es/save-k8s-service-config! conf)
                _           (sleep)
                find-result (es/find-k8s-service-configs! :ids ["1" "3"])]
            (is (= 2 (count find-result))))))

; ------------------------------------------
      
      (testing "K8SService CRUD operations"
        (testing "flow save -> get"
          (let [srv               (m/map->K8SService {:id               "1"
                                                      :name             "test service"
                                                      :namespace        "default"
                                                      :url              "http://example.org"
                                                      :labels           {:a "1"
                                                                         :b "2"}
                                                      :openApiEnabled?  true
                                                      :asyncApiEnabled? true
                                                      :resourceVersion  666
                                                      :lastUpdated      nil})
                save-result       (es/save-k8s-service! srv)
                load-result       (es/get-k8s-service! "1")
                ; ----
                last-updated-save (:lastUpdated save-result)
                srv-saved         (assoc srv :lastUpdated last-updated-save)
                srv-get           (assoc srv :lastUpdated last-updated-save)]
            (is (= save-result srv-saved))
            (is (= load-result srv-get))))

        (testing "flow save, save -> list"
          (let [srv1       (m/map->K8SService {:id               "2"
                                               :name             "test service a"
                                               :namespace        "default"
                                               :url              "http://example.org"
                                               :labels           {:a "1"
                                                                  :b "2"}
                                               :openApiEnabled?  true
                                               :asyncApiEnabled? true
                                               :resourceVersion  666
                                               :lastUpdated      nil})
                srv2       (m/map->K8SService {:id               "3"
                                               :name             "test service b"
                                               :namespace        "default"
                                               :url              "http://example.org"
                                               :labels           {:a "1"
                                                                  :b "2"}
                                               :openApiEnabled?  true
                                               :asyncApiEnabled? true
                                               :resourceVersion  666
                                               :lastUpdated      nil})

                _          (es/save-k8s-service! srv1)
                _          (es/save-k8s-service! srv2)
                _          (sleep)
                result     (es/list-k8s-service!)
                result-ids (map :id result)]
            (is (<= 2 (count result)))
            (is (.contains result-ids "2"))
            (is (.contains result-ids "3"))))

        (testing "flow save -> get -> delete -> get"
          (let [srv           (m/map->K8SService {:id               "4"
                                                  :name             "test service a"
                                                  :namespace        "default"
                                                  :url              "http://example.org"
                                                  :labels           {:a "1"
                                                                     :b "2"}
                                                  :openApiEnabled?  true
                                                  :asyncApiEnabled? true
                                                  :resourceVersion  666
                                                  :lastUpdated      nil})
                _             (es/save-k8s-service! srv)
                before-delete (es/get-k8s-service! "4")
                _             (es/delete-k8s-service! "4")
                _             (sleep)
                afetr-delete  (es/get-k8s-service! "4")]
            (is (not (nil? before-delete)))
            (is (nil? afetr-delete))))

        (testing "flow save -> list & filter by last-update"
          (let [srv             (m/map->K8SService {:id               "5"
                                                    :name             "test service a"
                                                    :namespace        "default"
                                                    :url              "http://example.org"
                                                    :labels           {:a "1"
                                                                       :b "2"}
                                                    :openApiEnabled?  true
                                                    :asyncApiEnabled? true
                                                    :resourceVersion  666
                                                    :lastUpdated      nil}) 
                _               (sleep 2)
                _               (es/save-k8s-service! srv)
                _               (sleep) 
                result-now      (es/list-k8s-service! :last-updated (date/now))
                result-just     (es/list-k8s-service! :last-updated (date/now -2))
                result-just-ids (map :id result-just)
                result-all      (es/list-k8s-service! :last-updated (date/unix-date))
                result-all-ids  (map :id result-all)]
            (is (= [] result-now))
            (is (= ["5"] result-just-ids))
            (is (= ["1" "2" "3" "5"] result-all-ids))))))

;; ---
    (tc/stop! container)
    (tc/perform-cleanup!)))

(deftest get-async-job-run-test
  (is (= 1 1)))
