(ns beholder.model
  (:require [schema.core :as s]))

(def ^:private url (s/pred #(re-matches #"(www|http:|https:)+[^\s]+[\w]" %)))


(s/defrecord KubernetesService [id :- s/Str
                                name :- s/Str
                                namespace :- s/Str
                                url :- url
                                labels :- (s/maybe {s/Keyword s/Str})])

(s/defrecord BeholderConfig [namespaces :- (s/maybe [s/Str])
                             openApiLabel :- (s/maybe s/Str)
                             openApiPath :- (s/maybe s/Str)])


(s/defrecord ServiceConfig [serviceId :- s/Str
                            openApiPath :- (s/maybe s/Str)
                            team :- (s/maybe s/Str)
                            repo :- (s/maybe s/Str)])

