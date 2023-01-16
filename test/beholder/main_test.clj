(ns beholder.main-test
  (:require [beholder.main :as routes]
            [beholder.model :as m]
            [clojure.string :as str]
            [clojure.test :refer :all]
            [ring.mock.request :as mock]))

(deftest ^:unit handlers-test

  (testing "Test service/:id/openapi route"
    (with-redefs [beholder.repositories.config/get-beholder-config! (fn [] (m/map->BeholderConfig {}))
                  beholder.repositories.config/get-service-config (fn [id] (m/map->ServiceConfig {:openApiPath "/gdulus/beholder/main/test/empty.json"}))
                  beholder.repositories.k8s/get-service! (fn [id] (m/map->KubernetesService {:url "https://raw.githubusercontent.com"}))]
      (let [response (routes/app-routes (mock/request :get "/service/12345678/openapi"))]
        (is (= "{\"status\": \"test\"}" (:body response)))
        (is (= 200 (:status response))))))

  (testing "Test service/:id/config route - file exists"
    (with-redefs [beholder.repositories.config/get-beholder-config! (fn [] (m/map->BeholderConfig {}))
                  beholder.repositories.config/get-service-config (fn [id] (m/map->ServiceConfig {:openApiPath "/gdulus/beholder/main/test/empty.json"}))
                  beholder.repositories.k8s/get-service! (fn [id] (m/map->KubernetesService {:url "https://raw.githubusercontent.com"}))]
      (let [response (routes/app-routes (mock/request :get "/service/12345678/config"))]
        (is (str/includes? (:body response) "File found"))
        (is (= 200 (:status response))))))

  (testing "Test service/:id/config route - file not exists"
    (with-redefs [beholder.repositories.config/get-beholder-config! (fn [] (m/map->BeholderConfig {}))
                  beholder.repositories.config/get-service-config (fn [id] (m/map->ServiceConfig {:openApiPath "/gdulus/beholder/main/test/not-exists.json"}))
                  beholder.repositories.k8s/get-service! (fn [id] (m/map->KubernetesService {:url "https://raw.githubusercontent.com"}))]
      (let [response (routes/app-routes (mock/request :get "/service/12345678/config"))]
        (is (str/includes? (:body response) "File not found"))
        (is (= 200 (:status response))))))

  (testing "Test service/:id/config route - error while checking for the file"
    (with-redefs [beholder.repositories.config/get-beholder-config! (fn [] (m/map->BeholderConfig {}))
                  beholder.repositories.config/get-service-config (fn [id] (m/map->ServiceConfig {}))
                  beholder.repositories.k8s/get-service! (fn [id] (m/map->KubernetesService {}))]
      (let [response (routes/app-routes (mock/request :get "/service/12345678/config"))]
        (is (str/includes? (:body response) "Error while checking for the file"))
        (is (= 200 (:status response)))))))

