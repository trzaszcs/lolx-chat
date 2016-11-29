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
    )
  )

(defn anounce-bulk-details
  [anounce-ids]
  (let [response (client/get (str (env :backend-url) "/anounces/" ) {:query-params {"id" anounce-ids}})
        anounces-map (as-json (:body response))]
    anounces-map
    )
  )


(defn user-details
  [user-ids]
  (let [response (client/get (str (env :auth-url) "/users/bulk")
                             {:query-params {"userId" user-ids}})]
    (as-json (:body response))
    ))

