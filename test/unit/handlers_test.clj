(ns unit.handlers-test
  (:require [beholder.handlers :as routes]
            [beholder.model :as m]
            [clojure.string :as str]
            [clojure.test :refer :all]
            [ring.mock.request :as mock]))

(deftest test-service_id_openapi_proxy-route

  (testing "Test /service/:id/doc/openapi/proxy route - file exists"
    (with-redefs [beholder.repositories.es/get-beholder-config! (fn [] (m/map->BeholderConfig {}))
                  beholder.repositories.es/get-service-config! (fn [_] (m/map->K8SServiceConfig {:openApiPath "/gdulus/beholder/main/test/empty.json"}))
                  beholder.repositories.k8s/get-service! (fn [_] (m/map->K8SService {:url "https://raw.githubusercontent.com"}))]
      (let [response (routes/app-routes (mock/request :get "/service/:id/doc/openapi/proxy"))]
        (is (= "{\"status\": \"test\"}" (:body response)))
        (is (= 200 (:status response))))))

  (testing "Test /service/:id/doc/openapi/proxy route - file does not exists"
    (with-redefs [beholder.repositories.es/get-beholder-config! (fn [] (m/map->BeholderConfig {}))
                  beholder.repositories.es/get-service-config! (fn [_] (m/map->K8SServiceConfig {:openApiPath "/gdulus/beholder/main/test/notexists.json"}))
                  beholder.repositories.k8s/get-service! (fn [_] (m/map->K8SService {:url "https://raw.githubusercontent.com"}))]
      (let [response (routes/app-routes (mock/request :get "/service/:id/doc/openapi/proxy"))]
        (is (= "" (:body response)))
        (is (= 200 (:status response)))))))

; --------------------------------------------------------------------------------

(deftest test-service_id_config-route

  (testing "Test service/:id/config route - disabled services"
    (with-redefs [beholder.repositories.es/get-beholder-config! (fn [] (m/map->BeholderConfig {}))
                  beholder.repositories.es/get-service-config! (fn [_] (m/map->K8SServiceConfig {:openApiPath "/gdulus/beholder/main/test/empty.json"}))
                  beholder.repositories.k8s/get-service! (fn [_] (m/map->K8SService {:url                     "https://raw.githubusercontent.com"
                                                                                            :labels           {}
                                                                                            :openApiEnabled?  false
                                                                                            :asyncApiEnabled? false}))]
      (let [response (routes/app-routes (mock/request :get "/service/12345678/config"))]
        (is (str/includes? (:body response) "To enable OpenAPI docu mark service with \"openapi\" label."))
        (is (str/includes? (:body response) "To enable AsyncAPI docu mark service with \"asyncapi\" label."))
        (is (= 200 (:status response))))))

  ; ---

  (testing "Test service/:id/config route - OpenAPI file check -> file exists"
    (with-redefs [beholder.repositories.es/get-beholder-config! (fn [] (m/map->BeholderConfig {}))
                  beholder.repositories.es/get-service-config! (fn [_] (m/map->K8SServiceConfig {:openApiPath "/gdulus/beholder/main/test/empty.json"}))
                  beholder.repositories.k8s/get-service! (fn [_] (m/map->K8SService {:url                     "https://raw.githubusercontent.com"
                                                                                            :labels           {:openapi true}
                                                                                            :openApiEnabled?  true
                                                                                            :asyncApiEnabled? false}))]
      (let [response (routes/app-routes (mock/request :get "/service/12345678/config"))]
        (is (str/includes? (:body response) "To enable AsyncAPI docu mark service with \"asyncapi\" label."))
        (is (str/includes? (:body response) "File found"))
        (is (= 200 (:status response))))))

  (testing "Test service/:id/config route - OpenAPI file check -> file not exists"
    (with-redefs [beholder.repositories.es/get-beholder-config! (fn [] (m/map->BeholderConfig {}))
                  beholder.repositories.es/get-service-config! (fn [_] (m/map->K8SServiceConfig {:openApiPath "/gdulus/beholder/main/test/not-exists.json"}))
                  beholder.repositories.k8s/get-service! (fn [_] (m/map->K8SService {:url                     "https://raw.githubusercontent.com"
                                                                                            :labels           {:openapi true}
                                                                                            :openApiEnabled?  true
                                                                                            :asyncApiEnabled? false}))]
      (let [response (routes/app-routes (mock/request :get "/service/12345678/config"))]
        (is (str/includes? (:body response) "To enable AsyncAPI docu mark service with \"asyncapi\" label."))
        (is (str/includes? (:body response) "https://raw.githubusercontent.com/gdulus/beholder/main/test/not-exists.json"))
        (is (str/includes? (:body response) "File not found"))
        (is (= 200 (:status response))))))

  (testing "Test service/:id/config route - OpenAPI file check -> error while checking for the file"
    (with-redefs [beholder.repositories.es/get-beholder-config! (fn [] (m/map->BeholderConfig {}))
                  beholder.repositories.es/get-service-config! (fn [_] (m/map->K8SServiceConfig {}))
                  beholder.repositories.k8s/get-service! (fn [_] (m/map->K8SService {:labels                  {:openapi true}
                                                                                            :openApiEnabled?  true
                                                                                            :asyncApiEnabled? false}))]
      (let [response (routes/app-routes (mock/request :get "/service/12345678/config"))]
        (is (str/includes? (:body response) "To enable AsyncAPI docu mark service with \"asyncapi\" label."))
        (is (str/includes? (:body response) "Error while checking for the file"))
        (is (= 200 (:status response))))))

  ; ---

  (testing "Test service/:id/config route - AsyncAPI file check -> file exists"
    (with-redefs [beholder.repositories.es/get-beholder-config! (fn [] (m/map->BeholderConfig {}))
                  beholder.repositories.es/get-service-config! (fn [_] (m/map->K8SServiceConfig {:asyncApiPath "/gdulus/beholder/main/test/empty.json"}))
                  beholder.repositories.k8s/get-service! (fn [_] (m/map->K8SService {:url                     "https://raw.githubusercontent.com"
                                                                                            :labels           {:asyncapi true}
                                                                                            :openApiEnabled?  false
                                                                                            :asyncApiEnabled? true}))]
      (let [response (routes/app-routes (mock/request :get "/service/12345678/config"))]
        (is (str/includes? (:body response) "To enable OpenAPI docu mark service with \"openapi\" label."))
        (is (str/includes? (:body response) "File found"))
        (is (= 200 (:status response))))))

  (testing "Test service/:id/config route - AsyncAPI file check -> file not exists"
    (with-redefs [beholder.repositories.es/get-beholder-config! (fn [] (m/map->BeholderConfig {}))
                  beholder.repositories.es/get-service-config! (fn [_] (m/map->K8SServiceConfig {:asyncApiPath "/gdulus/beholder/main/test/not-exists.json"}))
                  beholder.repositories.k8s/get-service! (fn [_] (m/map->K8SService {:url                     "https://raw.githubusercontent.com"
                                                                                            :labels           {:asyncapi true}
                                                                                            :openApiEnabled?  false
                                                                                            :asyncApiEnabled? true}))]
      (let [response (routes/app-routes (mock/request :get "/service/12345678/config"))]
        (is (str/includes? (:body response) "To enable OpenAPI docu mark service with \"openapi\" label."))
        (is (str/includes? (:body response) "https://raw.githubusercontent.com/gdulus/beholder/main/test/not-exists.json"))
        (is (str/includes? (:body response) "File not found"))
        (is (= 200 (:status response))))))

  (testing "Test service/:id/config route - AsyncAPI file check -> error while checking for the file"
    (with-redefs [beholder.repositories.es/get-beholder-config! (fn [] (m/map->BeholderConfig {}))
                  beholder.repositories.es/get-service-config! (fn [_] (m/map->K8SServiceConfig {}))
                  beholder.repositories.k8s/get-service! (fn [_] (m/map->K8SService {:labels                  {:asyncapi true}
                                                                                            :openApiEnabled?  false
                                                                                            :asyncApiEnabled? true}))]
      (let [response (routes/app-routes (mock/request :get "/service/12345678/config"))]
        (is (str/includes? (:body response) "To enable OpenAPI docu mark service with \"openapi\" label."))
        (is (str/includes? (:body response) "Error while checking for the file"))
        (is (= 200 (:status response))))))

  )