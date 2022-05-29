(ns beholder.bootstrap
  (:require [beholder.repository :as r]
            [beholder.model :as m]))

(defn init-elastic []
  (do
    (r/delete-indexes!)
    (r/create-indexes!)
    (r/save-documentation! (m/->Documentation nil nil nil "EasyBill" "https://www.easybill.de/api" "new" "swagger"))
    (r/save-documentation! (m/->Documentation nil nil nil "Petstore" "https://petstore.swagger.io" "new" "swagger"))))