(ns beholder.model
  (:require [clojure.string :as str]
            [environ.core :refer [env]]
            [schema.core :as s]))

(def ^:private url (s/pred #(re-matches #"(www|http:|https:)+[^\s]+[\w]" %)))
(def ^:private timestamp (s/pred inst?))
(def ^:private ^:const default-namespace ["default"])
(def ^:private ^:const default-openapi-label :openapi)
(def ^:private ^:const default-openapi-path "api/openapi.yml")
(def ^:private ^:const default-asyncpi-label :asyncapi)
(def ^:private ^:const default-asyncapi-path "api/asyncapi.yml")

(defn- sanitize-path [path]
  (-> (str "/" path)
      (str/replace #"/+" "/")
      (subs 1)))

; --------------------------------------------------------------
; AsyncJobRun
; --------------------------------------------------------------

(s/defrecord AsyncJobRun [name :- s/Str
                          lastRun :- timestamp])
; --------------------------------------------------------------
; BeholderConfig
; --------------------------------------------------------------

(defprotocol DefaultValuesAware
  (get-namespaces [_])
  (get-openapi-label [_])
  (get-openapi-path [_])
  (get-asyncapi-label [_])
  (get-asyncapi-path [_]))

(s/defrecord BeholderConfig [namespaces :- (s/maybe [s/Str])
                             openApiLabel :- (s/maybe s/Str)
                             openApiPath :- (s/maybe s/Str)
                             asyncApiLabel :- (s/maybe s/Str)
                             asyncApiPath :- (s/maybe s/Str)]
  DefaultValuesAware

  (get-namespaces [_]
    (let [namespaces (remove str/blank? namespaces)]
      (if (empty? namespaces) default-namespace namespaces)))

  (get-openapi-label [_]
    (if (str/blank? openApiLabel) default-openapi-label (keyword openApiLabel)))

  (get-openapi-path [_]
    (if (str/blank? openApiPath) default-openapi-path openApiPath))

  (get-asyncapi-label [_]
    (if (str/blank? asyncApiLabel) default-asyncpi-label (keyword asyncApiLabel)))

  (get-asyncapi-path [_]
    (if (str/blank? asyncApiPath) default-asyncapi-path asyncApiPath)))

; -------------------------------------------------------------
; K8SService
; --------------------------------------------------------------

(s/defrecord K8SService [id :- s/Str
                         name :- s/Str
                         namespace :- s/Str
                         url :- url
                         labels :- (s/maybe {s/Keyword s/Str})
                         openApiEnabled? :- s/Bool
                         asyncApiEnabled? :- s/Bool
                         resourceVersion :- s/Int
                         lastUpdated :- (s/maybe timestamp)])

(s/defrecord K8SServiceConfig [serviceId  :- s/Str
                               openApiPath :- (s/maybe s/Str)
                               asyncApiPath :- (s/maybe s/Str)
                               team :- (s/maybe s/Str)
                               repo :- (s/maybe s/Str)
                               description :- (s/maybe s/Str)])

(s/defrecord K8SServiceDoc [serviceId :- s/Str
                            openApiDoc :- (s/maybe s/Str)
                            asyncApiDoc :- (s/maybe s/Str)])

; --------------------------------------------------------------

(s/defrecord Carrier [serviceId :- s/Str
                      name :- s/Str
                      openApiEnabled? :- s/Bool
                      asyncApiEnabled? :- s/Bool
                      config :- (s/maybe K8SServiceConfig)
                      k8sService :- (s/maybe K8SService)])

; --------------------------------------------------------------

(defn get-openapi-url [beholder-config k8s-service k8s-service-conf]
  (let [app-name     (:name k8s-service)
        override-url (env (keyword app-name))
        url          (or override-url (:url k8s-service))]
    (str url "/" (if (not (str/blank? (:openApiPath k8s-service-conf)))
                   (sanitize-path (:openApiPath k8s-service-conf))
                   (sanitize-path (get-openapi-path beholder-config))))))

(defn get-asyncapi-url [beholder-config k8s-service k8s-service-conf]
  (str (:url k8s-service) "/" (if (not (str/blank? (:asyncApiPath k8s-service-conf)))
                                     (sanitize-path (:asyncApiPath k8s-service-conf))
                                     (sanitize-path (get-asyncapi-path beholder-config)))))