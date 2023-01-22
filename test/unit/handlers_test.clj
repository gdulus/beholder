(ns unit.handlers-test
  (:require [beholder.handlers :as routes]
            [beholder.model :as m]
            [clojure.string :as str]
            [clojure.test :refer :all]
            [ring.mock.request :as mock]))

(deftest test-service_id_openapi_proxy-route

  (testing "Test /service/:id/doc/openapi/proxy route - file exists"
    (with-redefs [beholder.repositories.config/get-beholder-config! (fn [] (m/map->BeholderConfig {}))
                  beholder.repositories.config/get-service-config (fn [_] (m/map->ServiceConfig {:openApiPath "/gdulus/beholder/main/test/empty.json"}))
                  beholder.repositories.k8s/get-service! (fn [_] (m/map->KubernetesService {:url "https://raw.githubusercontent.com"}))]
      (let [response (routes/app-routes (mock/request :get "/service/:id/doc/openapi/proxy"))]
        (is (= "{\"status\": \"test\"}" (:body response)))
        (is (= 200 (:status response))))))

  (testing "Test /service/:id/doc/openapi/proxy route - file does not exists"
    (with-redefs [beholder.repositories.config/get-beholder-config! (fn [] (m/map->BeholderConfig {}))
                  beholder.repositories.config/get-service-config (fn [_] (m/map->ServiceConfig {:openApiPath "/gdulus/beholder/main/test/notexists.json"}))
                  beholder.repositories.k8s/get-service! (fn [_] (m/map->KubernetesService {:url "https://raw.githubusercontent.com"}))]
      (let [response (routes/app-routes (mock/request :get "/service/:id/doc/openapi/proxy"))]
        (is (= "" (:body response)))
        (is (= 200 (:status response)))))))

; --------------------------------------------------------------------------------

(deftest test-service_id_config-route

  (testing "Test service/:id/config route - OpenAPI disabled"
    (with-redefs [beholder.repositories.config/get-beholder-config! (fn [] (m/map->BeholderConfig {}))
                  beholder.repositories.config/get-service-config (fn [_] (m/map->ServiceConfig {:openApiPath "/gdulus/beholder/main/test/empty.json"}))
                  beholder.repositories.k8s/get-service! (fn [_] (m/map->KubernetesService {:url    "https://raw.githubusercontent.com"
                                                                                            :labels {}}))]
      (let [response (routes/app-routes (mock/request :get "/service/12345678/config"))]
        (is (str/includes? (:body response) "To enable OpenAPI docu mark service with \"openapi\" label."))
        (is (= 200 (:status response))))))

  (testing "Test service/:id/config route - OpenAPI file check -> file exists"
    (with-redefs [beholder.repositories.config/get-beholder-config! (fn [] (m/map->BeholderConfig {}))
                  beholder.repositories.config/get-service-config (fn [_] (m/map->ServiceConfig {:openApiPath "/gdulus/beholder/main/test/empty.json"}))
                  beholder.repositories.k8s/get-service! (fn [_] (m/map->KubernetesService {:url    "https://raw.githubusercontent.com"
                                                                                            :labels {:openapi true}}))]
      (let [response (routes/app-routes (mock/request :get "/service/12345678/config"))]
        (is (str/includes? (:body response) "File found"))
        (is (= 200 (:status response))))))

  (testing "Test service/:id/config route - OpenAPI file check -> file not exists"
    (with-redefs [beholder.repositories.config/get-beholder-config! (fn [] (m/map->BeholderConfig {}))
                  beholder.repositories.config/get-service-config (fn [_] (m/map->ServiceConfig {:openApiPath "/gdulus/beholder/main/test/not-exists.json"}))
                  beholder.repositories.k8s/get-service! (fn [_] (m/map->KubernetesService {:url    "https://raw.githubusercontent.com"
                                                                                            :labels {:openapi true}}))]
      (let [response (routes/app-routes (mock/request :get "/service/12345678/config"))]
        (is (str/includes? (:body response) "File not found"))
        (is (= 200 (:status response))))))

  (testing "Test service/:id/config route - OpenAPI file check -> error while checking for the file"
    (with-redefs [beholder.repositories.config/get-beholder-config! (fn [] (m/map->BeholderConfig {}))
                  beholder.repositories.config/get-service-config (fn [_] (m/map->ServiceConfig {}))
                  beholder.repositories.k8s/get-service! (fn [_] (m/map->KubernetesService {:labels {:openapi true}}))]
      (let [response (routes/app-routes (mock/request :get "/service/12345678/config"))]
        (is (str/includes? (:body response) "Error while checking for the file"))
        (is (= 200 (:status response))))))

  (testing "Test service/:id/config route - AsyncAPI disabled"
    (with-redefs [beholder.repositories.config/get-beholder-config! (fn [] (m/map->BeholderConfig {}))
                  beholder.repositories.config/get-service-config (fn [_] (m/map->ServiceConfig {}))
                  beholder.repositories.k8s/get-service! (fn [_] (m/map->KubernetesService {}))]
      (let [response (routes/app-routes (mock/request :get "/service/12345678/config"))]
        (is (str/includes? (:body response) "To enable AsyncAPI docu mark service with \"asyncapi\" label."))
        (is (= 200 (:status response)))))))