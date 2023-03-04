(ns beholder.repositories.k8s-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [beholder.repositories.k8s :as k8s]
   [beholder.repositories.es :as es]
   [beholder.model :as m]))

(def non-empty-response {:kind       "ServiceList",
                         :apiVersion "v1",
                         :metadata   {:resourceVersion "46056"},
                         :items      [{:metadata {:name "beholder", :namespace "default", :uid "id1", :resourceVersion "12522", :creationTimestamp "2022-08-07T11:58:40Z", :labels {:app "beholder" :openapi "true"}, :managedFields [{:manager "kubectl-expose", :operation "Update", :apiVersion "v1", :time "2022-08-07T11:58:40Z", :fieldsType "FieldsV1", :fieldsV1 {}}]},
                                       :spec     {:selector {:app "beholder"}, :allocateLoadBalancerNodePorts true, :internalTrafficPolicy "Cluster", :externalTrafficPolicy "Cluster", :ipFamilies ["IPv4"], :ipFamilyPolicy "SingleStack", :clusterIP "10.99.213.16", :type "LoadBalancer", :clusterIPs ["10.99.213.16"], :ports [{:protocol "TCP", :port 3000, :targetPort 3000, :nodePort 30924}], :sessionAffinity "None"},
                                       :status   {:loadBalancer {}}}
                                      {:metadata {:name "hello-minikube", :namespace "default", :uid "id2", :resourceVersion "483", :creationTimestamp "2022-05-28T10:19:33Z", :labels {:app "hello-minikube" :openapi "true"}, :managedFields [{:manager "kubectl-expose", :operation "Update", :apiVersion "v1", :time "2022-05-28T10:19:33Z", :fieldsType "FieldsV1", :fieldsV1 {}}]},
                                       :spec     {:selector {:app "hello-minikube"}, :internalTrafficPolicy "Cluster", :externalTrafficPolicy "Cluster", :ipFamilies ["IPv4"], :ipFamilyPolicy "SingleStack", :clusterIP "10.104.148.167", :type "NodePort", :clusterIPs ["10.104.148.167"], :ports [{:protocol "TCP", :port 8080, :targetPort 8080, :nodePort 31003}], :sessionAffinity "None"},
                                       :status   {:loadBalancer {}}}
                                      {:metadata {:name "kubernetes", :namespace "default", :uid "id3", :resourceVersion "196", :creationTimestamp "2022-05-28T10:18:44Z", :labels {:component "apiserver", :provider "kubernetes"}, :managedFields [{:manager "kube-apiserver", :operation "Update", :apiVersion "v1", :time "2022-05-28T10:18:44Z", :fieldsType "FieldsV1", :fieldsV1 {}}]},
                                       :spec     {:ports [{:name "https", :protocol "TCP", :port 443, :targetPort 8443}], :clusterIP "10.96.0.1", :clusterIPs ["10.96.0.1"], :type "ClusterIP", :sessionAffinity "None", :ipFamilies ["IPv4"], :ipFamilyPolicy "SingleStack", :internalTrafficPolicy "Cluster"},
                                       :status   {:loadBalancer {}}}
                                      {:metadata {:name "openapi-app", :namespace "default", :uid "id4", :resourceVersion "7969", :creationTimestamp "2022-08-03T21:17:23Z", :labels {:app "openapi-app" :openapi "true"}, :managedFields [{:manager "kubectl-expose", :operation "Update", :apiVersion "v1", :time "2022-08-03T21:17:23Z", :fieldsType "FieldsV1", :fieldsV1 {}}]},
                                       :spec     {:selector {:app "openapi-app"}, :allocateLoadBalancerNodePorts true, :internalTrafficPolicy "Cluster", :externalTrafficPolicy "Cluster", :ipFamilies ["IPv4"], :ipFamilyPolicy "SingleStack", :clusterIP "10.100.186.35", :type "LoadBalancer", :clusterIPs ["10.100.186.35"], :ports [{:protocol "TCP", :port 3000, :targetPort 3000, :nodePort 31362}], :sessionAffinity "None"},
                                       :status   {:loadBalancer {}}}]})

(def multiple-namespaces {"n1"
                          {:kind       "ServiceList",
                           :apiVersion "v1",
                           :metadata   {:resourceVersion "46056"},
                           :items      [{:metadata {:name "app1", :namespace "n1", :uid "id1", :resourceVersion "12522", :creationTimestamp "2022-08-07T11:58:40Z", :labels {:app "app1" :openapi "true"}, :managedFields [{:manager "kubectl-expose", :operation "Update", :apiVersion "v1", :time "2022-08-07T11:58:40Z", :fieldsType "FieldsV1", :fieldsV1 {}}]},
                                         :spec     {:selector {:app "beholder"}, :allocateLoadBalancerNodePorts true, :internalTrafficPolicy "Cluster", :externalTrafficPolicy "Cluster", :ipFamilies ["IPv4"], :ipFamilyPolicy "SingleStack", :clusterIP "10.99.213.16", :type "LoadBalancer", :clusterIPs ["10.99.213.16"], :ports [{:protocol "TCP", :port 3000, :targetPort 3000, :nodePort 30924}], :sessionAffinity "None"},
                                         :status   {:loadBalancer {}}}]}
                          "n2"
                          {:kind       "ServiceList",
                           :apiVersion "v1",
                           :metadata   {:resourceVersion "46056"},
                           :items      [{:metadata {:name "app2", :namespace "n2", :uid "id2", :resourceVersion "12522", :creationTimestamp "2022-08-07T11:58:40Z", :labels {:app "app2" :openapi "true"}, :managedFields [{:manager "kubectl-expose", :operation "Update", :apiVersion "v1", :time "2022-08-07T11:58:40Z", :fieldsType "FieldsV1", :fieldsV1 {}}]},
                                         :spec     {:selector {:app "beholder"}, :allocateLoadBalancerNodePorts true, :internalTrafficPolicy "Cluster", :externalTrafficPolicy "Cluster", :ipFamilies ["IPv4"], :ipFamilyPolicy "SingleStack", :clusterIP "10.99.213.16", :type "LoadBalancer", :clusterIPs ["10.99.213.16"], :ports [{:protocol "TCP", :port 3000, :targetPort 3000, :nodePort 30924}], :sessionAffinity "None"},
                                         :status   {:loadBalancer {}}}]}})

(def test-filter-responses {:kind       "ServiceList",
                            :apiVersion "v1",
                            :metadata   {:resourceVersion "46056"},
                            :items      [{:metadata {:name "no-openapi-label", :namespace "default", :uid "id1", :resourceVersion "12522", :creationTimestamp "2022-08-07T11:58:40Z", :labels {:app "openapi"}, :managedFields [{:manager "kubectl-expose", :operation "Update", :apiVersion "v1", :time "2022-08-07T11:58:40Z", :fieldsType "FieldsV1", :fieldsV1 {}}]},
                                          :spec     {:selector {:app "no-openapi-label"}, :allocateLoadBalancerNodePorts true, :internalTrafficPolicy "Cluster", :externalTrafficPolicy "Cluster", :ipFamilies ["IPv4"], :ipFamilyPolicy "SingleStack", :clusterIP "10.99.213.16", :type "LoadBalancer", :clusterIPs ["10.99.213.16"], :ports [{:protocol "TCP", :port 3000, :targetPort 3000, :nodePort 30924}], :sessionAffinity "None"},
                                          :status   {:loadBalancer {}}}
                                         {:metadata {:name "default-openapi-label", :namespace "default", :uid "id2", :resourceVersion "483", :creationTimestamp "2022-05-28T10:19:33Z", :labels {:app "default-openapi-label" :openapi "true"}, :managedFields [{:manager "kubectl-expose", :operation "Update", :apiVersion "v1", :time "2022-05-28T10:19:33Z", :fieldsType "FieldsV1", :fieldsV1 {}}]},
                                          :spec     {:selector {:app "default-openapi-label"}, :internalTrafficPolicy "Cluster", :externalTrafficPolicy "Cluster", :ipFamilies ["IPv4"], :ipFamilyPolicy "SingleStack", :clusterIP "10.104.148.167", :type "NodePort", :clusterIPs ["10.104.148.167"], :ports [{:protocol "TCP", :port 8080, :targetPort 8080, :nodePort 31003}], :sessionAffinity "None"},
                                          :status   {:loadBalancer {}}}
                                         {:metadata {:name "non-app", :namespace "default", :uid "id3", :resourceVersion "196", :creationTimestamp "2022-05-28T10:18:44Z", :labels {:component "apiserver", :provider "kubernetes"}, :managedFields [{:manager "kube-apiserver", :operation "Update", :apiVersion "v1", :time "2022-05-28T10:18:44Z", :fieldsType "FieldsV1", :fieldsV1 {}}]},
                                          :spec     {:ports [{:name "https", :protocol "TCP", :port 443, :targetPort 8443}], :clusterIP "10.96.0.1", :clusterIPs ["10.96.0.1"], :type "ClusterIP", :sessionAffinity "None", :ipFamilies ["IPv4"], :ipFamilyPolicy "SingleStack", :internalTrafficPolicy "Cluster"},
                                          :status   {:loadBalancer {}}}
                                         {:metadata {:name "custom-openapi-label", :namespace "default", :uid "id4", :resourceVersion "7969", :creationTimestamp "2022-08-03T21:17:23Z", :labels {:app "custom-openapi-label" :custom-openapi "true"}, :managedFields [{:manager "kubectl-expose", :operation "Update", :apiVersion "v1", :time "2022-08-03T21:17:23Z", :fieldsType "FieldsV1", :fieldsV1 {}}]},
                                          :spec     {:selector {:app "custom-openapi-label"}, :allocateLoadBalancerNodePorts true, :internalTrafficPolicy "Cluster", :externalTrafficPolicy "Cluster", :ipFamilies ["IPv4"], :ipFamilyPolicy "SingleStack", :clusterIP "10.100.186.35", :type "LoadBalancer", :clusterIPs ["10.100.186.35"], :ports [{:protocol "TCP", :port 3000, :targetPort 3000, :nodePort 31362}], :sessionAffinity "None"},
                                          :status   {:loadBalancer {}}}]})

(deftest k8s-respository-is
  (testing "Testing fetch-services!"
    (testing "empty response"
      (with-redefs [beholder.repositories.k8s/load-k8s-services (fn [_] {})
                    beholder.repositories.es/get-beholder-config! (fn [] (m/map->BeholderConfig {}))]
        (let [result (k8s/fetch-services!)]
          (is (empty? result)))))

;--------------------------

    (testing "non empty response"
      (testing "all responses from one namespace"
        (with-redefs [beholder.repositories.k8s/load-k8s-services (fn [_] non-empty-response)
                      beholder.repositories.es/get-beholder-config! (fn [] (m/map->BeholderConfig {}))]
          (let [result (k8s/fetch-services!)]
            (is (= 3 (count result)))
            (is (= (m/map->K8SService {:id "id1",
                                       :name "beholder",
                                       :namespace "default",
                                       :url "http://beholder:3000",
                                       :labels {:app "beholder", :openapi "true"},
                                       :openApiEnabled? true,
                                       :asyncApiEnabled? false,
                                       :resourceVersion 12522,
                                       :lastUpdated nil})
                   (first result)))
            (is (= (m/map->K8SService {:id "id2",
                                       :name "hello-minikube",
                                       :namespace "default",
                                       :url "http://hello-minikube:8080",
                                       :labels {:app "hello-minikube", :openapi "true"},
                                       :openApiEnabled? true,
                                       :asyncApiEnabled? false,
                                       :resourceVersion 483,
                                       :lastUpdated nil})
                   (second result)))
            (is (= (m/map->K8SService {:id "id4",
                                       :name "openapi-app",
                                       :namespace "default",
                                       :url "http://openapi-app:3000",
                                       :labels {:app "openapi-app", :openapi "true"},
                                       :openApiEnabled? true,
                                       :asyncApiEnabled? false,
                                       :resourceVersion 7969,
                                       :lastUpdated nil})
                   (last result))))))

;--------------------------

      (testing "filtering"
        (testing "with default gloabl config"
          (with-redefs [beholder.repositories.k8s/load-k8s-services (fn [_] test-filter-responses)
                        beholder.repositories.es/get-beholder-config! (fn [] (m/map->BeholderConfig {}))]
            (let [result (k8s/fetch-services!)]
              (is (= 1 (count result)))
              (is (= (m/map->K8SService    {:id "id2",
                                            :name "default-openapi-label",
                                            :namespace "default",
                                            :url "http://default-openapi-label:8080",
                                            :labels {:app "default-openapi-label", :openapi "true"},
                                            :openApiEnabled? true,
                                            :asyncApiEnabled? false,
                                            :resourceVersion 483,
                                            :lastUpdated nil})
                     (first result))))))

        (testing "with custom label"
          (with-redefs [beholder.repositories.k8s/load-k8s-services (fn [_] test-filter-responses)
                        beholder.repositories.es/get-beholder-config! (fn [] (m/map->BeholderConfig {:openApiLabel "custom-openapi"}))]
            (let [result (k8s/fetch-services!)]
              (is (= 1 (count result)))
              (is (= (m/map->K8SService   {:id "id4",
                                           :name "custom-openapi-label",
                                           :namespace "default",
                                           :url "http://custom-openapi-label:3000",
                                           :labels {:app "custom-openapi-label", :custom-openapi "true"},
                                           :openApiEnabled? true,
                                           :asyncApiEnabled? false,
                                           :resourceVersion 7969,
                                           :lastUpdated nil})
                     (first result)))))))

;--------------------------

      (testing "namespace parsing"
        (testing "with all namespaces selected"
          (with-redefs [beholder.repositories.k8s/load-k8s-services (fn [ns] (get multiple-namespaces ns))
                        beholder.repositories.es/get-beholder-config! (fn [] (m/map->BeholderConfig {:namespaces ["n1" "n2"]}))]
            (let [result (k8s/fetch-services!)]
              (is (= 2 (count result)))
              (is (= (m/map->K8SService   {:id "id1",
                                           :name "app1",
                                           :namespace "n1",
                                           :url "http://beholder:3000",
                                           :labels {:app "app1", :openapi "true"},
                                           :openApiEnabled? true,
                                           :asyncApiEnabled? false,
                                           :resourceVersion 12522,
                                           :lastUpdated nil})
                     (first result)))
              (is (= (m/map->K8SService {:id "id2",
                                         :name "app2",
                                         :namespace "n2",
                                         :url "http://beholder:3000",
                                         :labels {:app "app2", :openapi "true"},
                                         :openApiEnabled? true,
                                         :asyncApiEnabled? false,
                                         :resourceVersion 12522,
                                         :lastUpdated nil})
                     (second result))))))

        (testing "with n1 namespace selected"
          (with-redefs [beholder.repositories.k8s/load-k8s-services (fn [ns] (get multiple-namespaces ns))
                        beholder.repositories.es/get-beholder-config! (fn [] (m/map->BeholderConfig {:namespaces ["n1"]}))]
            (let [result (k8s/fetch-services!)]
              (is (= 1 (count result)))
              (is (= (m/map->K8SService   {:id "id1",
                                           :name "app1",
                                           :namespace "n1",
                                           :url "http://beholder:3000",
                                           :labels {:app "app1", :openapi "true"},
                                           :openApiEnabled? true,
                                           :asyncApiEnabled? false,
                                           :resourceVersion 12522,
                                           :lastUpdated nil})
                     (first result))))))

        (testing "with n2 namespace selected"
          (with-redefs [beholder.repositories.k8s/load-k8s-services (fn [ns] (get multiple-namespaces ns))
                        beholder.repositories.es/get-beholder-config! (fn [] (m/map->BeholderConfig {:namespaces ["n2"]}))]
            (let [result (k8s/fetch-services!)]
              (is (= 1 (count result)))
              (is (= (m/map->K8SService {:id "id2",
                                         :name "app2",
                                         :namespace "n2",
                                         :url "http://beholder:3000",
                                         :labels {:app "app2", :openapi "true"},
                                         :openApiEnabled? true,
                                         :asyncApiEnabled? false,
                                         :resourceVersion 12522,
                                         :lastUpdated nil})
                     (first result))))))
        (testing "wrong namespaces selected"
          (with-redefs [beholder.repositories.k8s/load-k8s-services (fn [ns] (get multiple-namespaces ns))
                        beholder.repositories.es/get-beholder-config! (fn [] (m/map->BeholderConfig {:namespaces ["wrong"]}))]
            (let [result (k8s/fetch-services!)]
              (is (empty? result)))))))))
