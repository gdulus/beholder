(ns beholder.services.carrier
  (:require [beholder.model :as m]
            [beholder.repositories.es :as es]
            [beholder.services.k8s-service :as k8s]))

(defn- convert-to-key-value [service-config]
  {(:serviceId service-config)
   service-config})

(defn- get-service-configs-map []
  (->>
    (es/list-service-configs!)
    (map convert-to-key-value)
    (into {})))

; ---------------------------------------------------------------------

(defn list-carriers!
  ([]
   (list-carriers! (constantly true)))
  ([filter-criteria]
   (let [service-configs-map (get-service-configs-map)]
     (->>
       (k8s/list-k8s-service!)
       (filter filter-criteria)
       (map #(m/->Carrier (:id %)
                          (:name %)
                          (:openApiEnabled? %)
                          (:asyncApiEnabled? %)
                          (get service-configs-map (:id %))
                          %))))))

