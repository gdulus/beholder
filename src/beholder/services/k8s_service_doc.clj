(ns beholder.services.k8s-service-doc
  (:require
   [beholder.services.async-job :as async]
   [beholder.repositories.doc :as doc]
   [beholder.model :as m]
   [beholder.utils.log :as log]
   [beholder.repositories.es :as es]))

(defn- find-k8s-conf [k8s-srv-confs k8s-srv]
  (let [id (:id k8s-srv)]
    (first (filter #(= id (:serviceId %)) k8s-srv-confs))))

(defn- build-K8SServiceDoc [beholder-conf k8s-srv k8s-srv-conf get-asyncapi-fn get-openapi-fn]
  (let [openapi-url  (m/get-openapi-url beholder-conf k8s-srv k8s-srv-conf)
        asyncapi-url (m/get-asyncapi-url beholder-conf k8s-srv k8s-srv-conf)
        asyncapi-doc (future (get-asyncapi-fn asyncapi-url))
        openapi-doc  (future (get-openapi-fn openapi-url))]
    (m/map->K8SServiceDoc {:serviceId   (:id k8s-srv)
                           :asyncApiDoc @asyncapi-doc
                           :openApiDoc  @openapi-doc})))

(defn- execute-indexing [since ctx]
  (let [;; unpack dependent functions
        get-asyncapi-fn             (:get-asyncapi-fn ctx)
        get-openapi-fn              (:get-openapi-fn ctx)
        beholder-config-provider-fn (:beholder-config-provider-fn ctx)
        srv-provider-fn             (:srv-provider-fn ctx)
        srv-configs-provider-fn     (:srv-configs-provider-fn ctx)
        changed-k8s-srv-doc-fn      (:changed-k8s-srv-doc-fn ctx)
        ;; get the data
        beholder-conf               (beholder-config-provider-fn)
        update-services             (srv-provider-fn :last-updated since)
        updated-services-ids        (map :id update-services)
        srv-confs                   (srv-configs-provider-fn :ids updated-services-ids)]
    (log/info "Fetching documentation for services" updated-services-ids)
    (->>
     update-services
     (map #(vector beholder-conf % (find-k8s-conf srv-confs %) get-asyncapi-fn get-openapi-fn))
     (map #(apply build-K8SServiceDoc %))
     (map changed-k8s-srv-doc-fn))))

; ----------------------------------

(def ^:private ^:const indexer-job-name "k8s-service-doc-indexer-job")
(def ^:private ^:const indexer-job-interval-sec 60)

(defn start-periodic-indexing! []
  (async/start-job indexer-job-name
                   indexer-job-interval-sec
                   (fn [last-run]
                     (execute-indexing last-run
                                       {:get-asyncapi-fn             doc/fetch-asyncapi-doc!
                                        :get-openapi-fn              doc/fetch-openapi-doc!
                                        :beholder-config-provider-fn es/get-beholder-config!
                                        :srv-provider-fn             es/list-k8s-service!
                                        :srv-configs-provider-fn     es/find-k8s-service-configs!
                                        :changed-k8s-srv-doc-fn      es/save-k8s-service-doc!}))))