(ns beholder.services.k8s-service
  (:require
   [beholder.repositories.es :as es]
   [beholder.repositories.k8s :as k8s]
   [beholder.services.async-job :as async]
   [beholder.utils.log :as log]
   [clojure.data :as data]))

(defn- find-removed [srv-local srv-cluster]
  (let [lcl (map :id srv-local)
        cls (map :id srv-cluster)]
    (filter some? (first (data/diff lcl cls)))))

(defn- find-changed [srv-local srv-cluster]
  (let [extractor (fn [e] {(:id e) (:resourceVersion e)})
        transofrmer #(into {} (map extractor %))
        lcl (transofrmer srv-local)]
    (filter #(let [cls-version (get % :resourceVersion)
                   cls-id (get % :id)
                   lcl-version (get lcl cls-id)]
               (or (nil? lcl-version) (> cls-version lcl-version)))
            srv-cluster)))

(defn- index-k8s-services [ctx]
  (let [cluster-k8s-srv-provider-fn (:cluster-k8s-srv-provider-fn ctx)
        local-k8s-srv-provider-fn (:local-k8s-srv-provider-fn ctx)
        changed-k8s-srv-fn (:changed-k8s-srv-fn ctx)
        removed-k8s-srv-fn (:removed-k8s-srv-fn ctx)
        srv-cluster (cluster-k8s-srv-provider-fn)
        srv-local (local-k8s-srv-provider-fn)
        srv-changed (find-changed srv-local srv-cluster)
        srv-removed (find-removed srv-local srv-cluster)]
    {:srv-changed (map changed-k8s-srv-fn srv-changed)
     :srv-removed (map removed-k8s-srv-fn srv-removed)}))

; ---------------------------------------------------------

(def ^:private ^:const indexer-job-name "k8s-service-indexer-job")
(def ^:private ^:const indexer-job-interval-sec 60)

(defn start-periodic-indexing! []
  (async/start-job indexer-job-name
                   indexer-job-interval-sec
                   (fn [_]
                     (let [report (index-k8s-services {:cluster-k8s-srv-provider-fn k8s/fetch-services!
                                                       :local-k8s-srv-provider-fn es/list-k8s-service!
                                                       :changed-k8s-srv-fn es/save-k8s-service!
                                                       :removed-k8s-srv-fn es/delete-k8s-service!})]
                       (log/info "Indexing executed with result:" report)))))

(defn get-k8s-service! [id]
  (es/get-k8s-service! id))

(defn list-k8s-service! []
  (es/list-k8s-service!))


