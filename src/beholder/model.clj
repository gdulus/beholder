(ns beholder.model
  (:require [schema.core :as s]))

(def ^:private url-pattern s/Str)
(def ^:private doc-type (s/enum "swagger"))
(def ^:private doc-status (s/enum "updated" "new"))

(s/defrecord Documentation [id :- (s/maybe s/Str)
                            created :- (s/maybe s/Num)
                            updated :- (s/maybe s/Num)
                            name :- s/Str
                            url :- url-pattern
                            status :- doc-status
                            type :- doc-type])

(s/defrecord Render [name :- s/Str
                     url :- url-pattern
                     content :- s/Str])

(s/defrecord K8SService [name :- s/Str
                         namespace :- s/Str
                         url :- url-pattern
                         port :- s/Num])