(ns beholder.services.carrier
  (:require [beholder.model :as m]
            [beholder.repositories.es :as es]
            [beholder.repositories.k8s :as k8s]))

(defn- convert-to-key-value [service-config]
  {(:serviceId service-config)
   service-config})

(defn- get-service-configs-map []
  (->>
    (es/list-service-configs)
    (map convert-to-key-value)
    (into {})))

; ---------------------------------------------------------------------

(defn list-carriers! []
  (let [service-configs-map (get-service-configs-map)]
    (->>
      (k8s/list-services!)
      (map #(m/->Carrier (:id %)
                         (:name %)
                         (:openApiEnabled? %)
                         (:asyncApiEnabled? %)
                         (get service-configs-map (:id %)))))))

