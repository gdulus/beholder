(ns beholder.k8s-test
  (:require [clojure.test :refer :all])
  (:require [beholder.k8s :refer [list-services]]))

(deftest list-services-test
  (testing "Test list-services with empty response"
    (with-redefs [beholder.k8s/load-list-resources (fn [] {})]
      (is (empty? (list-services))))))


