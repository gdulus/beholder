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

(defn- get-value-or-default [value default converter]
  (if (str/blank? value)
    default
    (converter value)))

(defprotocol GlobalConfigAware
  (get-namespaces [x])
  (get-openapi-label [x])
  (get-openapi-path [x]))

(s/defrecord BeholderConfig [namespaces :- (s/maybe [s/Str])
                             openApiLabel :- (s/maybe s/Str)
                             openApiPath :- (s/maybe s/Str)]
  GlobalConfigAware

  (get-namespaces [x]
    (get-value-or-default namespaces
                          default-namespace
                          #(str/split % #",")))

  (get-openapi-label [x]
    (get-value-or-default openApiLabel
                          default-openapi-label
                          keyword))
  (get-openapi-path [x]
    (get-value-or-default openApiPath
                          default-openapi-path
                          identity)))

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
