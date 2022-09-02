(ns beholder.model
  (:require [schema.core :as s]))

(def ^:private url (s/pred #(re-matches #"(www|http:|https:)+[^\s]+[\w]" %)))
(def ^:private doc-type (s/enum "swagger"))
(def ^:private doc-status (s/enum "updated" "new"))


(s/defrecord KubernetesService [name :- s/Str
                                namespace :- s/Str
                                url :- url
                                labels :- (s/maybe {s/Keyword s/Str})])