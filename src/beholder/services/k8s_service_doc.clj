(ns beholder.services.k8s-service-doc
  (:require
   [beholder.services.async-job :as async]))

(def ^:private ^:const indexer-job-name "documentation-indexer-job")
(def ^:private ^:const indexer-job-interval-sec (* 60 1))

(defn- trigger-inexing [since ctx]
  (let [beholder-config-provider (:beholder-config-provider ctx)
        service-config-provider (:service-config-provider ctx)
        services-provider (:services-provider ctx)
        ; ----------------------------------
        beholder-config (beholder-config-provider)
        update-services (services-provider since)]
    (->
     update-services
     )
    
    )
  
   ;; - get all recently updated services
   ;; - for each service get OpenaAPI doc and save it
   ;; - for each service get AsyncAPI doc and save it
   ;; - mark
   ;;
  )

(defn start-indexing []
  (async/start-job indexer-job-name
                   indexer-job-interval-sec
                   (fn [last-run])))
