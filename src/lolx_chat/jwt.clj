(ns lolx-chat.jwt
  (:require 
   [clj-jwt.core  :refer :all]
   [clj-jwt.key   :refer [private-key public-key]]
   [clj-time.core :refer [now plus days]]
   [clojure.java.io :as io]))


(defn build-claim
  [issuer user-id]
  {:iss issuer
   :exp (plus (now) (days 1))
   :iat (now)
   :sub user-id}
)

(def rsa-prv-key (private-key (io/resource "rsa/key") "password"))
(def rsa-pub-key (public-key (io/resource "rsa/key.pub")))

(defn produce
  [issuer user-id]
  (-> (build-claim issuer user-id) jwt (sign :RS256 rsa-prv-key) to-str))

(defn get-rsa-pub-key
  [issuer] 
  (public-key (io/resource (str "rsa/" issuer ".pub"))))


(defn ok?
  [jwt-token]
  (let [jwt (str->jwt jwt-token)
        issuer (get-in jwt [:claims :iss])]
    (verify jwt (get-rsa-pub-key issuer))))

(defn issuer
  [jwt-token]
  (get-in (str->jwt jwt-token) [:claims :iss]))

(defn extract-jwt
  [headers]
  (let [authorization-header (get headers "authorization")]
    (when (not (nil? authorization-header))
      (clojure.string/replace-first authorization-header #"Bearer " ""))))
