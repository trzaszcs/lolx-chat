(ns lolx-auth.authentication-test
  (:use midje.sweet)
  (:require
   [lolx-auth.authentication :refer :all]
   [lolx-auth.dao :as dao]
   [lolx-auth.jwt :as jwt]
   [digest :as digest]))

(defn request
  [email password]
  {:body {:email email :password password}}
)

(fact "should return '401' if wrong credentials"
  (let [email "deer@wp.pl"
        password "pass"]
    (auth (request email password)) => {:status 401}
    (provided
     (dao/find-by-email email) => nil)))

(fact "should return '200' with jwt token if user gave correct credentials"
  (let [email "deer@wp.pl"
        password "pass"
        user-id "usrId"
        jwt "someJwt"]
    (auth (request email password)) => {:body {:jwt jwt :userId user-id}}
    (provided
     (jwt/produce "frontend" user-id) => jwt
     (dao/find-by-email email) => {:id user-id :password (digest/sha-256 password)})))
