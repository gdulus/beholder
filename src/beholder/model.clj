(ns beholder.model
  (:require [clojure.string :as str]
            [schema.core :as s]))

(def ^:private url (s/pred #(re-matches #"(www|http:|https:)+[^\s]+[\w]" %)))
(def ^:private default-openapi-label :openapi)

; --------------------------------------------------------------

(defprotocol LabelConfigurationAware
  (get-openapi-label [x]))


; --------------------------------------------------------------

(s/defrecord KubernetesService [id :- s/Str
                                name :- s/Str
                                namespace :- s/Str
                                url :- url
                                labels :- (s/maybe {s/Keyword s/Str})])

; --------------------------------------------------------------

(s/defrecord BeholderConfig [namespaces :- (s/maybe [s/Str])
                             openApiLabel :- (s/maybe s/Str)
                             openApiPath :- (s/maybe s/Str)]
  LabelConfigurationAware
  (get-openapi-label [x]
    (if (str/blank? openApiLabel)
      default-openapi-label
      (keyword openApiLabel))))

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
