(ns beholder.utils.date-test
  (:require [beholder.utils.date :as sut]
            [clojure.test :refer [deftest testing is]]))

(deftest baisc-time-operations []
  (testing "unix-time"
    (let [ut (sut/unix-date)
          past (sut/now -10000)
          now  (sut/now)]
      (is (some? ut))
      (is (some? past))
      (is (some? now))
      (is (sut/<=+ ut past))
      (is (sut/<=+ past now)))))
