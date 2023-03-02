(ns beholder.repositories.asyncapi
  (:require [beholder.model :as m]
            [clj-http.client :as client]
            [clojure.data.json :as json]
            [environ.core :refer [env]]
            [schema.core :as s]
            [beholder.utils.log :as log])
  (:import (beholder.model AsyncApiDocumentation)))

(def ^:private url (env :asyncapi-generator-service))

(defn- create-documentation [src]
  (->> (str url "/asyncapi?src=" src)
       (log/spy :info)
       (client/post)
       (:body)
       (json/read-str)))

(defn- get-file [id version file]
  (->> (str url "/asyncapi/file?id=" id "&version=" version "&file=" file)
       (log/spy :info)
       (client/get)
       (:body)
       (json/read-str)))

; -----------------------------------------------------------------

(defn get-index-file [src]
  (let [{id "id" version "version"} (create-documentation src)]
    (as-> (get-file id version "/index.html") v
      (get v "body")
      {:version version :body v}
      (m/map->AsyncApiDocumentation v)
      (s/validate AsyncApiDocumentation v))))
