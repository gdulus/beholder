(ns beholder.services.indexer
  (:require
   [beholder.model :as m]
   [beholder.repositories.asyncapi :as asyncapi]
   [beholder.repositories.es :as es]
   [beholder.services.carrier :as carrier]
   [chime.core :refer [chime-at periodic-seq]]
   [taoensso.timbre :as log])
  (:import
   (java.time Duration Instant)))


(defn- generate-asyncapi-url [c config]
  {:carrier c
   :doc-url (m/get-asyncapi-url config (:k8sService c) (:config c))})

(defn- fetch-asyncapi-index-file [c]
  (assoc c
         :index-file (asyncapi/get-index-file (:doc-url c))))

(defn- save-service-doc [c]
  (assoc c
         :service-doc (es/save-service-documentation! (m/map->ServiceDocumentation {:serviceId (get-in c [:carrier :serviceId])
                                                                                   :asyncApiDoc (:index-file c)}))))
(defn- mark-service-conf-as-cached [c]
  (assoc (get-in c [:carrier :config]) :asyncApiCached? true))


(defn- generate-asyncapi-doc []
  (let [global-config (es/get-beholder-config!)]
    (->>
        ;; get all carrier models with async api enabled
        (carrier/list-carriers! :asyncApiEnabled?)
        ;; generate url underwhich async api doc can be found
        (map #(generate-asyncapi-url % global-config))
        ;; load index file of the asyncapi documentation
        (map fetch-asyncapi-index-file)
        ;;
        (map save-service-doc)
        (map mark-service-conf-as-cached)
        (map es/save-service-config!))))


(defn start []
  (let [now (Instant/now)
        period (Duration/ofSeconds 10)
        new-binding period]
    (log/info "Setting up indexer. First execution will happen on " now " and will repeat every " new-binding)
    (chime-at (periodic-seq now new-binding)
              (fn [time]
                (log/info "Triggering docu generation" time)
                (generate-asyncapi-doc)))))
