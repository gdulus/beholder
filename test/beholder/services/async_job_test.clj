(ns beholder.services.async-job-test
  (:require
   [beholder.repositories.es :as es]
   [beholder.services.async-job :as sut]
   [chime.core :as chime]
   [clojure.test :refer [deftest is testing]]
   [beholder.utils.date :as date]
   [beholder.model :as m]))

(deftest start-job-test

  (testing "job throws an execption"
    (let [job-name "test-job"
          interval 100
          saved-job-run nil]
      (with-redefs [beholder.repositories.es/get-async-job-run! (fn [_] saved-job-run)
                    beholder.repositories.es/save-async-job-run! (fn [_] (is false))
                    chime/chime-at (fn [_ f] (f nil))]
        (let [assert-fn  (fn [_] (throw (RuntimeException. "Error")))
              result (sut/start-job job-name interval assert-fn)]
          (is (nil? result))))))

  (testing "starting job first time"
    (let [job-name "test-job"
          interval 100
          now (date/now)
          saved-job-run nil]
      (with-redefs [beholder.repositories.es/get-async-job-run! (fn [_] saved-job-run)
                    beholder.repositories.es/save-async-job-run! identity
                    chime/chime-at (fn [_ f] (f nil))]
        (let [assert-fn (fn [r]
                          (is (= job-name (:name r)))
                          (is (date/<=+ now (:lastRun r))))
              result (sut/start-job job-name interval assert-fn)]
          (assert-fn result)))))

  (testing "starting job second time"
    (let [job-name "test-job"
          interval 10
          now (date/now)
          past (.setTime now (- (.getTime now) 10000))
          saved-job-run (m/map->AsyncJobRun {:name "test-job" :lastRun past})]
      (with-redefs [beholder.repositories.es/get-async-job-run! (fn [_] saved-job-run)
                    beholder.repositories.es/save-async-job-run! identity
                    chime/chime-at (fn [_ f] (f nil))]
        (let [assert-fn #(is (= saved-job-run %))
              result (sut/start-job job-name interval assert-fn)]
          (is (= job-name (:name result)))
          (is (date/<=+ now (:lastRun result))))))))
