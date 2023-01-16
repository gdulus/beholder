(ns beholder.main-test
  (:require [beholder.main :as routes]
            [beholder.model :as m]
            [clojure.test :refer :all]
            [ring.mock.request :as mock]))

(deftest ^:unit your-handler-test
  (with-redefs [beholder.repositories.config/get-beholder-config! (fn [] (m/map->BeholderConfig {}))
                beholder.repositories.config/get-service-config (fn [id] (m/map->ServiceConfig {:openApiPath "/gdulus/beholder/main/test/empty.json"}))
                beholder.repositories.k8s/get-service! (fn [id] (m/map->KubernetesService {:url "https://raw.githubusercontent.com"}))]
    (let [response (routes/app-routes (mock/request :get "/service/12345678/openapi"))]
      (is (= "{\"status\": \"test\"}" (:body response)))
      (is (= 200 (:status response))))))

