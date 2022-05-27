(ns beholder.model
  (:require [schema.core :as s]))

(def ^:private doc-url s/Str)
(def ^:private doc-type (s/enum "swagger"))
(def ^:private doc-status (s/enum "updated" "new"))

(s/defrecord Documentation [id :- (s/maybe s/Str)
                            name :- s/Str
                            url :- doc-url
                            type :- doc-type])
