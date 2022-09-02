(ns beholder.repository
  (:require [qbits.spandex :as e]
            [clojure.tools.logging :as log]
            [schema.core :as s]
            [beholder.model :as m])
  (:import (beholder.model KubernetesService)
           (beholder.model Render)))

(def ^:private c (e/client {:hosts ["http://127.0.0.1:9200"]}))
(def ^:private doc-instance-url [:config :doc-instance])
(def ^:private render-url [:data :_doc])

; ----------------------------------------------------------------

(defn delete-indexes! []
  (try
    (e/request c {:method :delete :url [:config]})
    (e/request c {:method :delete :url [:data]})
    (catch Exception e (log/error "Error while deleting index" e))))

(defn create-indexes! []
  (try
    (e/request c {:method :put :url [:config]})
    (e/request c {:method :put :url [:data] :body {:mappings {:properties {:content {:type :text}}}}})
    (catch Exception e (log/error "Error while crating index" e))))

; ----------------------------------------------------------------
; KubernetesService CRUD
; ----------------------------------------------------------------

;(defn- doc-instance->Documentation [doc-instance]
;  (as-> (:_source doc-instance) v
;        (merge v {:id (:_id doc-instance)})
;        (m/map->Documentation v)
;        (s/validate Documentation v)))
;
;
;(defn save-documentation! [data]
;  (as-> data v
;        (s/validate KubernetesService v)
;        (e/request c {:method :post :url doc-instance-url :body v})))
;
;(defn list-documentation! []
;  (as-> (concat doc-instance-url [:_search]) v
;        (e/request c {:url v})
;        (get-in v [:body :hits :hits])
;        (map #(doc-instance->Documentation %) v)))
;
;(defn get-documentation! [id]
;  (as-> (concat doc-instance-url [:_search]) v
;        (e/request c {:url v :body {:query {:terms {:_id [id]}}}})
;        (get-in v [:body :hits :hits])
;        (map #(doc-instance->Documentation %) v)
;        (first v)))