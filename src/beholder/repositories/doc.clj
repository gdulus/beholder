(ns beholder.repositories.doc
  (:require [clj-http.client :as client]
            [clojure.data.json :as json]
            [environ.core :refer [env]]
            [beholder.utils.log :as log]))

(def ^:private url (env :asyncapi-generator-service))
(def ^:private timeout-ms (* 10 60 1000))

(defn- create-asyncapi-doc [src]
  (-> (str url "/asyncapi?src=" src)
      (client/post {:socket-timeout timeout-ms :connection-timeout timeout-ms})
      (get :body)
      (json/read-str)))

(defn- get-asyncapi-doc [id version file]
  (->> (str url "/asyncapi/file?id=" id "&version=" version "&file=" file)
       (client/get)
       (:body)
       (json/read-str)))

; -----------------------------------------------------------------

(defn fetch-asyncapi-doc! [src]
  (log/info "Fetching AsyncAPI doc from URL" src)
  (let [{id      "id"
         version "version"} (create-asyncapi-doc src)
        index-file (get-asyncapi-doc id version "/index.html")]
    (get index-file "body")))

(defn fetch-openapi-doc! [src]
  (log/info "Fetching OpenAPI doc from URL" src)
  (-> (client/get src)
      :body))