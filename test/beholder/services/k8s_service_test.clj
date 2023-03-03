(ns beholder.services.k8s-service-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [beholder.model :as m]
   [beholder.services.k8s-service :as subject]))

(defn- k8s-srv [id version]
  (m/map->K8SService {:id (str id)
                      :name (str (rand))
                      :resourceVersion version}))

(deftest index-k8s-service-test
  (testing "No services localy and in cluster"
    (let [ctx {:cluster-k8s-srv-provider-fn #(vector)
               :local-k8s-srv-provider-fn #(vector)
               :changed-k8s-srv-fn identity
               :removed-k8s-srv-fn identity}
          result (#'beholder.services.k8s-service/index-k8s-services ctx)]
      (is (empty? (:srv-changed result)))
      (is (empty? (:srv-result result)))))

  (testing "Services found only in cluster but not locally"
    (let [cluster-srv [(k8s-srv 1 1) (k8s-srv 2 1)]
          ctx {:cluster-k8s-srv-provider-fn #(identity cluster-srv)
               :local-k8s-srv-provider-fn #(vector)
               :changed-k8s-srv-fn identity
               :removed-k8s-srv-fn identity}
          result (#'beholder.services.k8s-service/index-k8s-services ctx)]
      (is (= cluster-srv (:srv-changed result)))
      (is (empty? (:srv-removed result)))))

  (testing "Services found only locally but not in cluster"
    (let [local-srv [(k8s-srv 1 1) (k8s-srv 2 1)]
          ctx {:cluster-k8s-srv-provider-fn #(vector)
               :local-k8s-srv-provider-fn #(identity local-srv)
               :changed-k8s-srv-fn identity
               :removed-k8s-srv-fn identity}
          result (#'beholder.services.k8s-service/index-k8s-services ctx)]
      (is (empty? (:srv-changed result)))
      (is (= ["1" "2"] (:srv-removed result)))))

  (testing "Services found in cluster and local, with the same ids and versions"
    (let [cluster-srv [(k8s-srv 1 1) (k8s-srv 2 1)]
          local-srv [(k8s-srv 1 1) (k8s-srv 2 1)]
          ctx {:cluster-k8s-srv-provider-fn #(identity cluster-srv)
               :local-k8s-srv-provider-fn #(identity local-srv)
               :changed-k8s-srv-fn identity
               :removed-k8s-srv-fn identity}
          result (#'beholder.services.k8s-service/index-k8s-services ctx)]
      (is (empty? (:srv-changed result)))
      (is (empty? (:srv-removed result)))))

  (testing "Services found in cluster and local, but one from cluster is in new version"
    (let [cluster-srv [(k8s-srv 1 2) (k8s-srv 2 1)]
          local-srv [(k8s-srv 1 1) (k8s-srv 2 1)]
          ctx {:cluster-k8s-srv-provider-fn #(identity cluster-srv)
               :local-k8s-srv-provider-fn #(identity local-srv)
               :changed-k8s-srv-fn identity
               :removed-k8s-srv-fn identity}
          result (#'beholder.services.k8s-service/index-k8s-services ctx)]
      (is (= [(first cluster-srv)] (:srv-changed result)))
      (is (empty? (:srv-removed result)))))

  (testing "Services found in cluster and local, but one from cluster is removed"
    (let [cluster-srv [(k8s-srv 1 1) (k8s-srv 2 1)]
          local-srv [(k8s-srv 2 1)]
          ctx {:cluster-k8s-srv-provider-fn #(identity cluster-srv)
               :local-k8s-srv-provider-fn #(identity local-srv)
               :changed-k8s-srv-fn identity
               :removed-k8s-srv-fn identity}
          result (#'beholder.services.k8s-service/index-k8s-services ctx)]
      (is (empty? (:srv-changed result)))
      (is (= ["1"] (:srv-removed result))))))
