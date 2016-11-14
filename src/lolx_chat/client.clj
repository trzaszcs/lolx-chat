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
  (let [response (client/get (str (env :backend-url) "/anounces/" anounce-id))
        anounce (as-json (:body response))]
    {:author-id (get anounce "ownerId")}
  ))

(defn user-details
  [user-ids]
  (let [response (client/get (str (env :auth-url) "/users")
                             {:query-params {:ids user-ids}
                              :throw-exceptions false})]
    (when (= 200 (:status response))
      (as-json (:body response))
    )))
