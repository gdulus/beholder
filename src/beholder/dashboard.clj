(ns beholder.dashboard
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

(defn list-services! []
  (let [service-configs-map (get-service-configs-map)
        beholder-config (conf/get-beholder-config!)]
    (->>
      (k8s/list-services!)
      (map #(m/->Service (:id %)
                         (:name %)
                         (some? (get-in % [:labels (m/get-openapi-label beholder-config)]))
                         (get service-configs-map (:id %)))))))

