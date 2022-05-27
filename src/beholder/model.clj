(ns beholder.model
  (:require [schema.core :as s]))

(def ^:private doc-url s/Str)
(def ^:private doc-type (s/enum "swagger"))

(s/defrecord Documentation [name :- s/Str
                            url :- doc-url
                            type :- doc-type])
