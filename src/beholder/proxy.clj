(ns beholder.proxy
  (:require [etaoin.api :as api]
            [clj-http.client :as client]
            [clojure.string :as s]
            [jsoup.soup :as jsoup])
  (:import java.util.Base64))

(defn- encode [to-encode]
  (.encodeToString (Base64/getEncoder) (.getBytes to-encode)))

(defn- decode [to-decode]
  (String. (.decode (Base64/getDecoder) to-decode)))

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

(defn request-page! [doc]
  (let [url (:url doc)
        url-encoded (encode url)
        content-path (str "/proxy/static/" url-encoded)]
    (clean-page url content-path)))


(defn request-content! [encoded-url resource]
  (let [url (str (decode encoded-url) "/" resource)
        content (client/get url)]
    {:header {"Content-Type" (get-in content [:header "Content-Type"])}
     :body   (:body content)}))






;(def ^:private driver (api/firefox {:headless true}))
;
;(defn get-content! [url]
;  (do
;    (api/go driver url)
;    (api/wait-visible driver [{:class :title}])
;    (api/get-source driver)))
;
;
;





