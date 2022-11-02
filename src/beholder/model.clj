(ns beholder.model
  (:require [clojure.string :as str]
            [schema.core :as s]))

(def ^:private url (s/pred #(re-matches #"(www|http:|https:)+[^\s]+[\w]" %)))
(def ^:private ^:const default-openapi-label :openapi)
(def ^:private ^:const default-openapi-path "/api/openapi.yml")
(def ^:private ^:const default-namespace ["default"])

; --------------------------------------------------------------

(s/defrecord KubernetesService [id :- s/Str
                                name :- s/Str
                                namespace :- s/Str
                                url :- url
                                labels :- (s/maybe {s/Keyword s/Str})])

; --------------------------------------------------------------

(defprotocol GlobalConfigAware
  (get-namespaces [x])
  (get-openapi-label [x])
  (get-openapi-path [x]))

(s/defrecord BeholderConfig [namespaces :- (s/maybe [s/Str])
                             openApiLabel :- (s/maybe s/Str)
                             openApiPath :- (s/maybe s/Str)]
  GlobalConfigAware

  (get-namespaces [x]
    (let [namespaces (remove str/blank? namespaces)]
      (if (empty? namespaces) default-namespace namespaces)))

  (get-openapi-label [x]
    (if (str/blank? openApiLabel) default-openapi-label openApiLabel))

  (get-openapi-path [x]
    (if (str/blank? openApiPath) default-openapi-path openApiPath)))

; --------------------------------------------------------------

(s/defrecord ServiceConfig [serviceId :- s/Str
                            openApiPath :- (s/maybe s/Str)
                            team :- (s/maybe s/Str)
                            repo :- (s/maybe s/Str)
                            description :- (s/maybe s/Str)])

; --------------------------------------------------------------

(s/defrecord Service [id :- s/Str
                      name :- s/Str
                      openApiEnabled? :- s/Bool
                      config :- (s/maybe ServiceConfig)])
