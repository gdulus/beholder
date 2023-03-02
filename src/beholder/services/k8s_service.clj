(ns beholder.services.k8s-service
  (:require
   [beholder.repositories.es :as es]
   [beholder.repositories.k8s :as k8s]
   [chime.core :refer [chime-at periodic-seq]]
   [clojure.data :as data]
   [beholder.utils.log :as log])
  (:import
   (java.time Duration Instant)))

(defn- filter-to-be-deleted [persisted cluster]
  (let [a (map :id persisted)
        b (map :id cluster)]
    (filter some? (first (data/diff a b)))))

(defn- filter-to-be-persisted [persisted cluster]
  (let [extractor (fn [e] {(:id e) (:resourceVersion e)})
        transofrmer #(into {} (map extractor %))
        a (transofrmer persisted)]
    (filter #(let [c-version (get % :resourceVersion)
                   c-id (get % :id)
                   p-version (get a c-id)]
               (or (nil? p-version)
                   (> c-version p-version))) cluster)))

(defn- index-k8s-services! [cluster-k8s-srv-provider stored-k8s-srv-provider]
  (let [found-in-cluster (cluster-k8s-srv-provider)
        found-persisted (stored-k8s-srv-provider)
        to-be-persisted (filter-to-be-persisted found-persisted found-in-cluster)
        to-be-deleted (filter-to-be-deleted found-persisted found-in-cluster)]
    {:found-in-cluster (count found-in-cluster)
     :found-persisted (count found-persisted)
     :persisted (count (map es/save-k8s-service! to-be-persisted))
     :deleted (count (map es/delete-k8s-service! to-be-deleted))}))

(defn start-indexer []
  (let [now (Instant/now)
        period (Duration/ofSeconds 10)
        new-binding period] (log/info "Setting up indexer. First execution will happen on " now " and will repeat every " new-binding)
       (chime-at (periodic-seq now new-binding)
                 (fn [_]
                   (log/info "Executing K8S service indexing")
                   (let [report (index-k8s-services! k8s/fetch-services! es/list-k8s-service!)]
                     (try
                       (log/info "Indexing executed with result:" report)
                       (catch Exception e
                         (log/error "There was an error while indexing K8S services" e))))))))

(defn get-k8s-service! [id]
  (es/get-k8s-service! id))

(defn list-k8s-service! []
  (es/list-k8s-service!))
