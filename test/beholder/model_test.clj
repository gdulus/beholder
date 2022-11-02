(ns beholder.model-test
  (:require [beholder.model :as m]
            [clojure.test :refer :all]))

(deftest ^:unit test-BeholderConfig-model
  (testing "Test get-namespaces"
    (is (= (m/get-namespaces (m/->BeholderConfig nil "" "")) ["default"]))
    (is (= (m/get-namespaces (m/->BeholderConfig [] "" "")) ["default"]))
    (is (= (m/get-namespaces (m/->BeholderConfig [""] "" "")) ["default"]))
    (is (= (m/get-namespaces (m/->BeholderConfig ["n1"] "" "")) ["n1"]))
    (is (= (m/get-namespaces (m/->BeholderConfig ["n1" "n2"] "" "")) ["n1" "n2"]))))
