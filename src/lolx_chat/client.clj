(ns lolx-chat.client
  (:require 
   [clojure.data.json :as json]
   [clj-http.client :as client]
   [clojure.tools.logging :as log]
   [lolx-chat.jwt :as jwt]
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

(defn request-order
  [anounce-id user-id]
  (let [response (client/get (str (env :backend-url) "/request-orders/anounce/" anounce-id) {:headers {"Authorization" (jwt/build-auth-token user-id)}
                                                                                             :throw-exceptions false})]
    (when (= 200 (:status response))
      (as-json (:body response))
      )
    )
  )

(defn anounce-bulk-details
  [anounce-ids]
  (let [response (client/get (str (env :backend-url) "/anounces/bulk" ) {:query-params {"id" anounce-ids}})
        anounces-map (as-json (:body response))]
    (reduce
     (fn [set [key value]]
       (assoc set (str key) value)
       )
     {}
     anounces-map
     )
    )
  )

(defn user-details
  [user-ids]
  (let [response (client/get (str (env :auth-url) "/users/bulk")
                             {:query-params {"userId" user-ids}})]
    (as-json (:body response))
    ))
