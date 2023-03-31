(ns beholder.services.k8s-service-doc-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [beholder.model :as m]
   [beholder.utils.date :as d]
   [beholder.services.k8s-service-doc :as subject]))

(deftest index-k8s-service-doc-test
  (testing "No service found"
    (let [;; with
          since  (d/unix-date)
          ctx    {:beholder-config-provider-fn (fn [] (m/map->BeholderConfig {}))
                  :get-asyncapi-fn             (fn [_] "")
                  :get-openapi-fn              (fn [_] "")
                  :srv-provider-fn             (fn [& {:keys [last-updated]}] 
                                                 (is (= last-updated since))
                                                 [])
                  :srv-configs-provider-fn     (fn [& {:keys [_]}] [])
                  :changed-k8s-srv-doc-fn      identity}
          ;; when
          result (#'beholder.services.k8s-service-doc/execute-indexing since ctx)]
      ;; then
      (is (empty? result))))
  (testing "One Service found"
    (let [;; with
          ctx    {:beholder-config-provider-fn (fn [] (m/map->BeholderConfig {}))
                  :get-asyncapi-fn             (fn [_] "AsyncApi Doc")
                  :get-openapi-fn              (fn [_] "OpeApi Doc")
                  :srv-provider-fn             (fn [& {:keys [_]}] 
                                                 [(m/map->K8SService {:id "1"})])
                  :srv-configs-provider-fn     (fn [& {:keys [_]}] 
                                                 [(m/map->K8SServiceConfig {:serviceId "1"})])
                  :changed-k8s-srv-doc-fn      identity}
          ;; when
          result (#'beholder.services.k8s-service-doc/execute-indexing (d/unix-date) ctx)]
      ;; then
      (is (= 1 (count result)))
      (is (= (m/map->K8SServiceDoc {:serviceId   "1" 
                                    :openApiDoc  "OpeApi Doc"
                                    :asyncApiDoc "AsyncApi Doc"}) 
             (first result)))))
  (testing "Two Service found"
    (let [;; with
          ctx    {:beholder-config-provider-fn (fn [] (m/map->BeholderConfig {}))
                  :get-asyncapi-fn             (fn [url] (get {"http://s1.example.org/s1/asyncapi" "AsyncApi Doc S1"
                                                               "http://s2.example.org/s2/asyncapi" "AsyncApi Doc S2"}
                                                              url))
                  :get-openapi-fn              (fn [url] (get {"http://s1.example.org/s1/openapi" "OpeApi Doc S1"
                                                               "http://s2.example.org/s2/openapi" "OpeApi Doc S2"}
                                                              url))
                  :srv-provider-fn             (fn [& {:keys [_]}]
                                                 [(m/map->K8SService {:id  "1"
                                                                      :url "http://s1.example.org"})
                                                  (m/map->K8SService {:id  "2"
                                                                      :url "http://s2.example.org"})])
                  :srv-configs-provider-fn     (fn [& {:keys [_]}]
                                                 [(m/map->K8SServiceConfig {:serviceId    "1"
                                                                            :openApiPath  "/s1/openapi"
                                                                            :asyncApiPath "/s1/asyncapi"})
                                                  (m/map->K8SServiceConfig {:serviceId    "2"
                                                                            :openApiPath  "/s2/openapi"
                                                                            :asyncApiPath "/s2/asyncapi"})])
                  :changed-k8s-srv-doc-fn      identity}
          ;; when
          result (#'beholder.services.k8s-service-doc/execute-indexing (d/unix-date) ctx)]
      ;; then
      (is (= 2 (count result)))
      (is (= (m/map->K8SServiceDoc {:serviceId   "1"
                                    :openApiDoc  "OpeApi Doc S1"
                                    :asyncApiDoc "AsyncApi Doc S1"})
             (first result)))
      (is (= (m/map->K8SServiceDoc {:serviceId   "2"
                                    :openApiDoc  "OpeApi Doc S2"
                                    :asyncApiDoc "AsyncApi Doc S2"})
             (last result))))))