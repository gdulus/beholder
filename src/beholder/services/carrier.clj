(ns beholder.services.carrier
  (:require [beholder.model :as m]
            [beholder.repositories.config :as conf]
            [beholder.repositories.k8s :as k8s]))

(defn- convert-to-key-value [service-config]
  {(:serviceId service-config)
   service-config})

(defn- get-service-configs-map []
  (->>
    (conf/list-service-configs)
    (map convert-to-key-value)
    (into {})))

; ---------------------------------------------------------------------

(defn list-carriers! []
  (let [service-configs-map (get-service-configs-map)
        beholder-config (conf/get-beholder-config!)
        openapi-label (m/get-openapi-label beholder-config)
        asyncapi-label (m/get-asyncapi-label beholder-config)]
    (->>
      (k8s/list-services!)
      (map #(m/->Carrier (:id %)
                         (:name %)
                         (contains? (:labels %) openapi-label)
                         (contains? (:labels %) asyncapi-label)
                         (get service-configs-map (:id %)))))))

