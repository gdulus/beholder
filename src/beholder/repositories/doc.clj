(ns beholder.repositories.doc
  (:require [clj-http.client :as client]
            [clojure.data.json :as json]
            [environ.core :refer [env]]
            [beholder.utils.log :as log]))

(def ^:private url (env :asyncapi-generator-service))

(defn- create-asyncapi-doc [src]
  (->> (str url "/asyncapi?src=" src)
       (log/spy :info)
       (client/post)
       (:body)
       (json/read-str)))

(defn- get-asyncapi-doc [id version file]
  (->> (str url "/asyncapi/file?id=" id "&version=" version "&file=" file)
       (log/spy :info)
       (client/get)
       (:body)
       (json/read-str)))

; -----------------------------------------------------------------

(defn fetch-asyncapi-doc! [src]
  (let [{id      "id"
         version "version"} (create-asyncapi-doc src)]
    (get-asyncapi-doc id version "/index.html")))

(defn fetch-openapi-doc! [src]
  (client/get src))