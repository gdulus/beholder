(ns beholder.model
  (:require [schema.core :as s]))

(def ^:private url (s/pred #(re-matches #"(www|http:|https:)+[^\s]+[\w]" %)))

(s/defrecord KubernetesService [id :- s/Str
                                name :- s/Str
                                namespace :- s/Str
                                url :- url
                                labels :- (s/maybe {s/Keyword s/Str})])