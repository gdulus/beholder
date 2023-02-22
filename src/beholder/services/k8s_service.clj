(ns beholder.services.k8s-service
  (:require
   [beholder.repositories.es :as es]
   [beholder.repositories.k8s :as k8s]
   [chime.core :refer [chime-at periodic-seq]]
   [clojure.data :as data]
   [taoensso.timbre :as log])
  (:import
   (java.time Duration Instant)))

(defn- to-be-deleted [persisted cluster]
  (let [a (map :id persisted)
        b (map :id cluster)]
    (filter some? (first (data/diff a b)))))

(defn- to-be-persisted [persisted cluster]
  (let [extractor (fn [e] {(:id e) (:resourceVersion e)})
        transofrmer #(into {} (map extractor %))
        a (transofrmer persisted)]
    (filter #(let [c-version (get % :resourceVersion)
                   c-id (get % :id)
                   p-version (get a c-id)]
               (or (nil? p-version)
                   (> c-version p-version))) cluster)))

(defn- index-k8s-services! []
  (let [found-in-cluster (k8s/fetch-services!)
        found-persisted (es/list-k8s-service!)
        to-be-persisted (to-be-persisted found-persisted found-in-cluster)
        to-be-deleted (to-be-deleted found-persisted found-in-cluster)]
    {:found-in-cluster (count found-in-cluster)
     :found-persisted (count found-persisted)
     :persisted (count (map es/save-k8s-service! to-be-persisted))
     :deleted (count (map es/delete-k8s-service! to-be-deleted))}))

;(index-k8s-services!)

(defn start-indexer []
  (let [now (Instant/now)
        period (Duration/ofSeconds 10)
        new-binding period] (log/info "Setting up indexer. First execution will happen on " now " and will repeat every " new-binding)
       (chime-at (periodic-seq now new-binding)
                 (fn [_]
                   (log/info "Executing K8S service indexing")
                   (let [s (index-k8s-services!)]
                     (try
                       (log/info (str "Indexed " (count s) " k8s services"))
                       (catch Exception e
                         (log/error "There was an error while indexing K8S services" e))))))))

(defn get-k8s-service! [id]
  (es/get-k8s-service! id))

(defn list-k8s-service! []
  (es/list-k8s-service!))
