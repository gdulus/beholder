(ns beholder.k8s-test
  (:require [clojure.test :refer :all]
            [beholder.k8s :refer [list-services]]
            [beholder.model :as m]))

(def empty-response {})
(def non-empty-response {:kind       "ServiceList",
                         :apiVersion "v1",
                         :metadata   {:resourceVersion "46056"},
                         :items      [
                                      {:metadata {:name "beholder", :namespace "default", :uid "c5553c2f-fd21-4b57-921a-a3ac66bf53b1", :resourceVersion "12522", :creationTimestamp "2022-08-07T11:58:40Z", :labels {:app "beholder"}, :managedFields [{:manager "kubectl-expose", :operation "Update", :apiVersion "v1", :time "2022-08-07T11:58:40Z", :fieldsType "FieldsV1", :fieldsV1 {}}]},
                                       :spec     {:selector {:app "beholder"}, :allocateLoadBalancerNodePorts true, :internalTrafficPolicy "Cluster", :externalTrafficPolicy "Cluster", :ipFamilies ["IPv4"], :ipFamilyPolicy "SingleStack", :clusterIP "10.99.213.16", :type "LoadBalancer", :clusterIPs ["10.99.213.16"], :ports [{:protocol "TCP", :port 3000, :targetPort 3000, :nodePort 30924}], :sessionAffinity "None"},
                                       :status   {:loadBalancer {}}}
                                      {:metadata {:name "hello-minikube", :namespace "default", :uid "93ac81ac-3c25-4967-92ca-224251437fb8", :resourceVersion "483", :creationTimestamp "2022-05-28T10:19:33Z", :labels {:app "hello-minikube"}, :managedFields [{:manager "kubectl-expose", :operation "Update", :apiVersion "v1", :time "2022-05-28T10:19:33Z", :fieldsType "FieldsV1", :fieldsV1 {}}]},
                                       :spec     {:selector {:app "hello-minikube"}, :internalTrafficPolicy "Cluster", :externalTrafficPolicy "Cluster", :ipFamilies ["IPv4"], :ipFamilyPolicy "SingleStack", :clusterIP "10.104.148.167", :type "NodePort", :clusterIPs ["10.104.148.167"], :ports [{:protocol "TCP", :port 8080, :targetPort 8080, :nodePort 31003}], :sessionAffinity "None"},
                                       :status   {:loadBalancer {}}}
                                      {:metadata {:name "kubernetes", :namespace "default", :uid "a2b05d1c-b207-4886-bbcc-f0d073931928", :resourceVersion "196", :creationTimestamp "2022-05-28T10:18:44Z", :labels {:component "apiserver", :provider "kubernetes"}, :managedFields [{:manager "kube-apiserver", :operation "Update", :apiVersion "v1", :time "2022-05-28T10:18:44Z", :fieldsType "FieldsV1", :fieldsV1 {}}]},
                                       :spec     {:ports [{:name "https", :protocol "TCP", :port 443, :targetPort 8443}], :clusterIP "10.96.0.1", :clusterIPs ["10.96.0.1"], :type "ClusterIP", :sessionAffinity "None", :ipFamilies ["IPv4"], :ipFamilyPolicy "SingleStack", :internalTrafficPolicy "Cluster"},
                                       :status   {:loadBalancer {}}}
                                      {:metadata {:name "openapi-app", :namespace "default", :uid "a57f9d9d-2db5-4323-a066-0fa8a8e633c4", :resourceVersion "7969", :creationTimestamp "2022-08-03T21:17:23Z", :labels {:app "openapi-app"}, :managedFields [{:manager "kubectl-expose", :operation "Update", :apiVersion "v1", :time "2022-08-03T21:17:23Z", :fieldsType "FieldsV1", :fieldsV1 {}}]},
                                       :spec     {:selector {:app "openapi-app"}, :allocateLoadBalancerNodePorts true, :internalTrafficPolicy "Cluster", :externalTrafficPolicy "Cluster", :ipFamilies ["IPv4"], :ipFamilyPolicy "SingleStack", :clusterIP "10.100.186.35", :type "LoadBalancer", :clusterIPs ["10.100.186.35"], :ports [{:protocol "TCP", :port 3000, :targetPort 3000, :nodePort 31362}], :sessionAffinity "None"},
                                       :status   {:loadBalancer {}}}]})

(defn find-K8SService-by-name [coll name]
  (first (filter #(= name (:name %)) coll)))

(deftest list-services-test

  (testing "Test list-services with empty response"
    (with-redefs [beholder.k8s/load-k8s-services (fn [] empty-response)]
      (is (empty? (list-services)))))

  (testing "Test list-services with non empty response"
    (with-redefs [beholder.k8s/load-k8s-services (fn [] non-empty-response)]
      (let [response (list-services)]
        (is (= 3 (count response)))
        (is (= (m/map->K8SService {:name "beholder", :namespace "default", :port 3000, :url "http://beholder"})
               (find-K8SService-by-name response "beholder")))
        (is (= (m/map->K8SService {:name "hello-minikube", :namespace "default", :port 8080, :url "http://hello-minikube"})
               (find-K8SService-by-name response "hello-minikube")))
        (is (= (m/map->K8SService {:name "openapi-app", :namespace "default", :port 3000, :url "http://openapi-app"})
               (find-K8SService-by-name response "openapi-app")))))))


