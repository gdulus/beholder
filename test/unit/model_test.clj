(ns unit.model-test
  (:require [beholder.model :as m]
            [clojure.test :refer :all]))

(deftest ^:unit test-BeholderConfig-model
  (testing "Test get-namespaces"
    (is (= (m/get-namespaces (m/->BeholderConfig nil "" "" "" "")) ["default"]))
    (is (= (m/get-namespaces (m/->BeholderConfig [] "" "" "" "")) ["default"]))
    (is (= (m/get-namespaces (m/->BeholderConfig [""] "" "" "" "")) ["default"]))
    (is (= (m/get-namespaces (m/->BeholderConfig ["n1"] "" "" "" "")) ["n1"]))
    (is (= (m/get-namespaces (m/->BeholderConfig ["n1" "n2"] "" "" "" "")) ["n1" "n2"]))))


(deftest ^:unit test-common-functions
  (testing "Test get-openapi-url"
    (is (= "/api/openapi.yml"
           (m/get-openapi-url (m/map->BeholderConfig {})
                              (m/map->KubernetesService {})
                              (m/map->ServiceConfig {}))))

    (is (= "http://test.com/api/openapi.yml"
           (m/get-openapi-url (m/map->BeholderConfig {})
                              (m/map->KubernetesService {:url "http://test.com"})
                              (m/map->ServiceConfig {}))))

    (is (= "http://test.com/api/global.openapi.yml"
           (m/get-openapi-url (m/map->BeholderConfig {:openApiPath "api/global.openapi.yml"})
                              (m/map->KubernetesService {:url "http://test.com"})
                              (m/map->ServiceConfig {}))))

    (is (= "http://test.com/api/local.openapi.yml"
           (m/get-openapi-url (m/map->BeholderConfig {:openApiPath "api/global.openapi.yml"})
                              (m/map->KubernetesService {:url "http://test.com"})
                              (m/map->ServiceConfig {:openApiPath "api/local.openapi.yml"}))))

    (is (= "http://test.com/api/local.openapi.yml"
           (m/get-openapi-url (m/map->BeholderConfig {})
                              (m/map->KubernetesService {:url "http://test.com"})
                              (m/map->ServiceConfig {:openApiPath "api/local.openapi.yml"}))))

    (is (= "http://test.com/api/local.openapi.yml"
           (m/get-openapi-url (m/map->BeholderConfig {})
                              (m/map->KubernetesService {:url "http://test.com"})
                              (m/map->ServiceConfig {:openApiPath "//api///local.openapi.yml"}))))))
