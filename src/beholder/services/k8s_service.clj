(ns beholder.services.k8s-service
  (:require
   [beholder.repositories.es :as es]
   [beholder.repositories.k8s :as k8s]
   [chime.core :refer [chime-at periodic-seq]]
   [clojure.data :as data]
   [beholder.utils.log :as log])
  (:import
   (java.time Duration Instant)))

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

(defn start-periodic-indexing! []
  (let [now (Instant/now)
        period (Duration/ofSeconds 60)
        new-binding period]
    (log/info "Setting up indexer. First execution will happen on " now " and will repeat every " new-binding)
    (chime-at (periodic-seq now new-binding)
              (fn [_]
                (log/info "Executing K8S service indexing")
                (try
                  (let [report (index-k8s-services {:cluster-k8s-srv-provider-fn k8s/fetch-services!
                                                    :local-k8s-srv-provider-fn es/list-k8s-service!
                                                    :changed-k8s-srv-fn es/save-k8s-service!
                                                    :removed-k8s-srv-fn es/delete-k8s-service!})]

                    (log/info "Indexing executed with result:" report))
                  (catch Exception e
                    (log/error "There was an error while indexing K8S services" e)
                    (throw e)))))))

(defn get-k8s-service! [id]
  (es/get-k8s-service! id))

(defn list-k8s-service! []
  (es/list-k8s-service!))
