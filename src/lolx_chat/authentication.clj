(ns lolx-auth.authentication
  (:require 
   [compojure.core :refer :all]
   [lolx-auth.dao :as dao]
   [lolx-auth.facebook :as fb]
   [lolx-auth.jwt :as jwt]
   [ring.util.response :refer :all]
   [digest :as digest]))


(defn- build-jwt-response!
  [user-id]
  {:body {:jwt (jwt/produce "frontend" user-id) :userId user-id }})

(defn auth
  [request]
  (let [{email :email password :password} (:body request)
        user (dao/find-by-email email)
        user-id (:id user)]
    (if (or (nil? user) (not (= (:password user) (digest/sha-256 password))))
      {:status 401}
      (build-jwt-response! user-id)
      )))

(defn auth-facebook
  [request]
  (let [{code :code} (:body request)
        access-token (fb/access-token! code)
        details (fb/user-details! access-token)]
    (if details
      (do
        ; check if user is currently created
        (if-let [user (dao/find-by-fb-id (details :id))]
          (build-jwt-response! (user :id))
          ; check if email exists and link fb-account
          (if-let [user (dao/find-by-email (details :email))]
            (do
              (dao/link-fb-account  (:id user) (:id details))
              (build-jwt-response! (user :id))
              )
            ; create user
            (do
              (dao/add-fb-user (details :id) (details :first-name) (details :last-name) (details :email) (details :location))
              (build-jwt-response! (details :id))
              ))))
      {:status 407}
      )))
