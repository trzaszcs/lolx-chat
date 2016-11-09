(ns lolx-auth.user-change-pass-test
  (:use midje.sweet)
  (:require
   [lolx-auth.user :refer :all]
   [lolx-auth.dao :as dao]
   [lolx-auth.jwt :as jwt]
   [digest :as digest]))

(defn request 
  [user-id jwt old-password new-password]
  {:params {:user-id user-id}
   :body {:oldPassword old-password :newPassword new-password}
   :headers {"authorization" (str "Bearer " jwt) }})

(fact "should return '401' when bad jwt"
  (let [user-id "234"
        jwt "JWT"
        old-password "old"
        new-password "new"]
    (change-password (request user-id jwt old-password new-password)) => {:status 401} 
    (provided
     (jwt/ok? jwt user-id) => false)))

(fact "should return '200'"
  (let [user-id "234"
        jwt "JWT"
        old-password "old"
        new-password "new"
        new-pass-hash (digest/sha-256 new-password)
        old-pass-hash (digest/sha-256 old-password)]
    (change-password (request user-id jwt old-password new-password)) => {:status 200} 
    (provided
     (jwt/ok? jwt user-id) => true
     (dao/find-by-id user-id) => {:password old-pass-hash}
     (dao/change-password user-id new-pass-hash) => true)))
