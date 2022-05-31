(ns beholder.proxy
  (:require [clj-http.client :as client]
            [clojure.string :as s]
            [jsoup.soup :as jsoup])
  (:import java.util.Base64))

(defn- encode [to-encode]
  (->> to-encode
       (.getBytes)
       (.encodeToString (Base64/getEncoder))))

(defn- decode [to-decode]
  (->> to-decode
       (.decode (Base64/getDecoder))
       (String.)))

(defn- get-clean-uri [element attr]
  (-> (.attr element attr)
      (s/replace "./" "")))

(defn- clean-references [document attr prefix]
  (->> document
       (jsoup/select (str "*[" attr "]"))
       (map #(.attr % attr (str prefix "/" (get-clean-uri % attr))))
       (last)
       (.ownerDocument)))

(defn- clean-page [url prefix]
  (-> (jsoup/get! url)
      (clean-references "href" prefix)
      (clean-references "src" prefix)
      (.html)))

; -----------------------------------------------------------------------------------------

(defn request-page! [resource-uri doc]
  (let [url (:url doc)
        url-encoded (encode url)
        content-path (str resource-uri url-encoded)]
    (clean-page url content-path)))

(defn request-content! [encoded-url resource]
  (let [url (str (decode encoded-url) "/" resource)
        content (client/get url)]
    {:header {"Content-Type" (get-in content [:header "Content-Type"])}
     :body   (:body content)}))