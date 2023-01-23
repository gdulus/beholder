(ns beholder.model
  (:require [clojure.string :as str]
            [schema.core :as s]))

(def ^:private url (s/pred #(re-matches #"(www|http:|https:)+[^\s]+[\w]" %)))
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

(s/defrecord KubernetesService [id :- s/Str
                                name :- s/Str
                                namespace :- s/Str
                                url :- url
                                labels :- (s/maybe {s/Keyword s/Str})
                                openApiEnabled? :- s/Bool
                                asyncApiEnabled? :- s/Bool])

; --------------------------------------------------------------

(defprotocol DefaultValuesAware
  (get-namespaces [x])
  (get-openapi-label [x])
  (get-openapi-path [x])
  (get-asyncapi-label [x])
  (get-asyncapi-path [x]))

(s/defrecord BeholderConfig [namespaces :- (s/maybe [s/Str])
                             openApiLabel :- (s/maybe s/Str)
                             openApiPath :- (s/maybe s/Str)
                             asyncApiLabel :- (s/maybe s/Str)
                             asyncApiPath :- (s/maybe s/Str)]
  DefaultValuesAware

  (get-namespaces [x]
    (let [namespaces (remove str/blank? namespaces)]
      (if (empty? namespaces) default-namespace namespaces)))

  (get-openapi-label [x]
    (if (str/blank? openApiLabel) default-openapi-label (keyword openApiLabel)))

  (get-openapi-path [x]
    (if (str/blank? openApiPath) default-openapi-path openApiPath))

  (get-asyncapi-label [x]
    (if (str/blank? asyncApiLabel) default-asyncpi-label (keyword asyncApiLabel)))

  (get-asyncapi-path [x]
    (if (str/blank? asyncApiPath) default-asyncapi-path asyncApiPath)))

; --------------------------------------------------------------

(s/defrecord ServiceConfig [serviceId :- s/Str
                            openApiPath :- (s/maybe s/Str)
                            asyncApiPath :- (s/maybe s/Str)
                            team :- (s/maybe s/Str)
                            repo :- (s/maybe s/Str)
                            description :- (s/maybe s/Str)
                            openApiCached? :- (s/maybe s/Bool)
                            asyncApiCached? :- (s/maybe s/Bool)])

; --------------------------------------------------------------

(s/defrecord ServiceDocumentation [serviceId :- s/Str
                                   openApiDoc :- (s/maybe s/Str)
                                   asyncApiDoc :- (s/maybe s/Str)])

; --------------------------------------------------------------


(s/defrecord Carrier [serviceId :- s/Str
                      name :- s/Str
                      openApiEnabled? :- s/Bool
                      asyncApiEnabled? :- s/Bool
                      config :- (s/maybe ServiceConfig)])

; --------------------------------------------------------------

(defn get-openapi-url [beholder-config k8s-service-conf service-conf]
  (str (:url k8s-service-conf) "/" (if (not (str/blank? (:openApiPath service-conf)))
                                     (sanitize-path (:openApiPath service-conf))
                                     (sanitize-path (get-openapi-path beholder-config)))))

(defn get-asyncapi-url [beholder-config k8s-service-conf service-conf]
  (str (:url k8s-service-conf) "/" (if (not (str/blank? (:asyncApiPath service-conf)))
                                     (sanitize-path (:asyncApiPath service-conf))
                                     (sanitize-path (get-asyncapi-path beholder-config)))))