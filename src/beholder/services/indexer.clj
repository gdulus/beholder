(ns beholder.services.indexer
  (:require [beholder.model :as m]
            [beholder.repositories.asyncapi :as asyncapi]
            [beholder.repositories.es :as es]
            [beholder.services.carrier :as carrier]
            [chime.core :refer [chime-at periodic-seq]]
            [taoensso.timbre :as log])
  (:import (java.time Duration Instant)))

; TODO - add cache check
; TODO - add
(defn- generate-asyncapi-doc []
  (let [global-config (es/get-beholder-config!)]
    (as->
      (carrier/list-carriers! :asyncApiEnabled?) v

      (map (fn [c] {:carrier c
                    :doc-url (m/get-asyncapi-url global-config (:k8sService c) (:config c))})
           v)

      (map (fn [c] {:carrier    (:carrier c)
                    :index-file (asyncapi/get-index-file (:doc-url c))})
           v)


      (map (fn [c] {:carrier     (:carrier c)
                    :service-doc (es/save-service-documentation!
                                   (m/map->ServiceDocumentation {:serviceId   (get-in c [:carrier :serviceId])
                                                                 :asyncApiDoc (:index-file c)}))})
           v)

      (map (fn [c] (assoc (get-in c [:carrier :config]) :asyncApiCached? true))
           v)

      (map (fn [c] (es/save-service-config! c))
           v)
      )))

(defn start []
  (let [now (Instant/now)
        period (Duration/ofSeconds 10)]
    (log/info "Setting up indexer. First execution will happen on " now " and will repeat every " period)
    (chime-at (periodic-seq now period)
              (fn [time]
                (log/info "Triggering docu generation" time)
                (generate-asyncapi-doc)))))
