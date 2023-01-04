(ns beholder.repositories.k8s-test
  (:require [beholder.model :as m]
            [beholder.repositories.k8s :refer [list-services!]]
            [clojure.test :refer :all]))

(def non-empty-response {:kind       "ServiceList",
                         :apiVersion "v1",
                         :metadata   {:resourceVersion "46056"},
                         :items      [
                                      {:metadata {:name "beholder", :namespace "default", :uid "id1", :resourceVersion "12522", :creationTimestamp "2022-08-07T11:58:40Z", :labels {:app "beholder"}, :managedFields [{:manager "kubectl-expose", :operation "Update", :apiVersion "v1", :time "2022-08-07T11:58:40Z", :fieldsType "FieldsV1", :fieldsV1 {}}]},
                                       :spec     {:selector {:app "beholder"}, :allocateLoadBalancerNodePorts true, :internalTrafficPolicy "Cluster", :externalTrafficPolicy "Cluster", :ipFamilies ["IPv4"], :ipFamilyPolicy "SingleStack", :clusterIP "10.99.213.16", :type "LoadBalancer", :clusterIPs ["10.99.213.16"], :ports [{:protocol "TCP", :port 3000, :targetPort 3000, :nodePort 30924}], :sessionAffinity "None"},
                                       :status   {:loadBalancer {}}}
                                      {:metadata {:name "hello-minikube", :namespace "default", :uid "id2", :resourceVersion "483", :creationTimestamp "2022-05-28T10:19:33Z", :labels {:app "hello-minikube"}, :managedFields [{:manager "kubectl-expose", :operation "Update", :apiVersion "v1", :time "2022-05-28T10:19:33Z", :fieldsType "FieldsV1", :fieldsV1 {}}]},
                                       :spec     {:selector {:app "hello-minikube"}, :internalTrafficPolicy "Cluster", :externalTrafficPolicy "Cluster", :ipFamilies ["IPv4"], :ipFamilyPolicy "SingleStack", :clusterIP "10.104.148.167", :type "NodePort", :clusterIPs ["10.104.148.167"], :ports [{:protocol "TCP", :port 8080, :targetPort 8080, :nodePort 31003}], :sessionAffinity "None"},
                                       :status   {:loadBalancer {}}}
                                      {:metadata {:name "kubernetes", :namespace "default", :uid "id3", :resourceVersion "196", :creationTimestamp "2022-05-28T10:18:44Z", :labels {:component "apiserver", :provider "kubernetes"}, :managedFields [{:manager "kube-apiserver", :operation "Update", :apiVersion "v1", :time "2022-05-28T10:18:44Z", :fieldsType "FieldsV1", :fieldsV1 {}}]},
                                       :spec     {:ports [{:name "https", :protocol "TCP", :port 443, :targetPort 8443}], :clusterIP "10.96.0.1", :clusterIPs ["10.96.0.1"], :type "ClusterIP", :sessionAffinity "None", :ipFamilies ["IPv4"], :ipFamilyPolicy "SingleStack", :internalTrafficPolicy "Cluster"},
                                       :status   {:loadBalancer {}}}
                                      {:metadata {:name "openapi-app", :namespace "default", :uid "id4", :resourceVersion "7969", :creationTimestamp "2022-08-03T21:17:23Z", :labels {:app "openapi-app"}, :managedFields [{:manager "kubectl-expose", :operation "Update", :apiVersion "v1", :time "2022-08-03T21:17:23Z", :fieldsType "FieldsV1", :fieldsV1 {}}]},
                                       :spec     {:selector {:app "openapi-app"}, :allocateLoadBalancerNodePorts true, :internalTrafficPolicy "Cluster", :externalTrafficPolicy "Cluster", :ipFamilies ["IPv4"], :ipFamilyPolicy "SingleStack", :clusterIP "10.100.186.35", :type "LoadBalancer", :clusterIPs ["10.100.186.35"], :ports [{:protocol "TCP", :port 3000, :targetPort 3000, :nodePort 31362}], :sessionAffinity "None"},
                                       :status   {:loadBalancer {}}}]})

(def multiple-namespaces {"n1"
                          {:kind       "ServiceList",
                           :apiVersion "v1",
                           :metadata   {:resourceVersion "46056"},
                           :items      [
                                        {:metadata {:name "app1", :namespace "n1", :uid "id1", :resourceVersion "12522", :creationTimestamp "2022-08-07T11:58:40Z", :labels {:app "app1"}, :managedFields [{:manager "kubectl-expose", :operation "Update", :apiVersion "v1", :time "2022-08-07T11:58:40Z", :fieldsType "FieldsV1", :fieldsV1 {}}]},
                                         :spec     {:selector {:app "beholder"}, :allocateLoadBalancerNodePorts true, :internalTrafficPolicy "Cluster", :externalTrafficPolicy "Cluster", :ipFamilies ["IPv4"], :ipFamilyPolicy "SingleStack", :clusterIP "10.99.213.16", :type "LoadBalancer", :clusterIPs ["10.99.213.16"], :ports [{:protocol "TCP", :port 3000, :targetPort 3000, :nodePort 30924}], :sessionAffinity "None"},
                                         :status   {:loadBalancer {}}}]}
                          "n2"
                          {:kind       "ServiceList",
                           :apiVersion "v1",
                           :metadata   {:resourceVersion "46056"},
                           :items      [
                                        {:metadata {:name "app2", :namespace "n2", :uid "id2", :resourceVersion "12522", :creationTimestamp "2022-08-07T11:58:40Z", :labels {:app "app2"}, :managedFields [{:manager "kubectl-expose", :operation "Update", :apiVersion "v1", :time "2022-08-07T11:58:40Z", :fieldsType "FieldsV1", :fieldsV1 {}}]},
                                         :spec     {:selector {:app "beholder"}, :allocateLoadBalancerNodePorts true, :internalTrafficPolicy "Cluster", :externalTrafficPolicy "Cluster", :ipFamilies ["IPv4"], :ipFamilyPolicy "SingleStack", :clusterIP "10.99.213.16", :type "LoadBalancer", :clusterIPs ["10.99.213.16"], :ports [{:protocol "TCP", :port 3000, :targetPort 3000, :nodePort 30924}], :sessionAffinity "None"},
                                         :status   {:loadBalancer {}}}]}})

(defn- find-K8SService-by-name [coll name]
  (first (filter #(= name (:name %)) coll)))

(deftest ^:unit list-services-test
  (testing "Test list-services with empty response"
    (with-redefs [beholder.repositories.k8s/load-k8s-services (fn [namespace] {})
                  beholder.repositories.config/get-beholder-config! (fn [] (m/->BeholderConfig "" "" ""))]
      (is (empty? (list-services!)))))


  (testing "Test list-services with non empty response"
    (with-redefs [beholder.repositories.k8s/load-k8s-services (fn [namespace] non-empty-response)
                  beholder.repositories.config/get-beholder-config! (fn [] (m/->BeholderConfig "" "" ""))]
      (let [response (list-services!)]
        (is (= 3 (count response)))
        (is (= (m/map->KubernetesService {:id "id1" :name "beholder", :namespace "default", :url "http://beholder:3000", :labels {:app "beholder"}})
               (find-K8SService-by-name response "beholder")))
        (is (= (m/map->KubernetesService {:id "id2" :name "hello-minikube", :namespace "default", :url "http://hello-minikube:8080", :labels {:app "hello-minikube"}})
               (find-K8SService-by-name response "hello-minikube")))
        (is (= (m/map->KubernetesService {:id "id4" :name "openapi-app", :namespace "default", :url "http://openapi-app:3000", :labels {:app "openapi-app"}})
               (find-K8SService-by-name response "openapi-app"))))))


  (testing "Test list-services different namespaces"
    (with-redefs [beholder.repositories.k8s/load-k8s-services (fn [namespace] (get multiple-namespaces namespace))
                  beholder.repositories.config/get-beholder-config! (fn [] (m/->BeholderConfig ["n1","n2"] "" ""))]
      (let [response (list-services!)]
        (is (= 2 (count response)))
        (is (= (m/map->KubernetesService {:id "id1" :name "app1", :namespace "n1", :url "http://beholder:3000", :labels {:app "app1"}})
               (find-K8SService-by-name response "app1")))
        (is (= (m/map->KubernetesService {:id "id2" :name "app2", :namespace "n2", :url "http://beholder:3000", :labels {:app "app2"}})
               (find-K8SService-by-name response "app2"))))))
  )


