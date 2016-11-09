(ns lolx-chat.client
  (:require 
   [clojure.data.json :as json]
   [clj-http.client :as client]
   [environ.core :refer [env]]))

(defn- as-json
  [str]
  (json/read-str str))

(defn anounce-details
  [anounce-id]
  (let [response (client/get (str (env :backend-url) "/anounces" anounce-id)
                             {:throw-exceptions false})]
    (when (= 200 (:status response))
      (let [anounce (as-json (:body response))]
        (:author-id (:ownerId anounce)))
      )
    ))
