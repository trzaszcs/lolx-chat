(ns lolx-auth.user
  (:require 
   [compojure.core :refer :all]
   [lolx-auth.validation :as validation]
   [lolx-auth.dao :as dao]
   [lolx-auth.jwt :as jwt]
   [clj-time.format :as format]
   [camel-snake-kebab.core :refer :all]
   [camel-snake-kebab.extras :refer [transform-keys]]
   [digest :as digest]
   [ring.util.response :refer :all]
   [clj-http.client :as http]
   [environ.core :refer [env]]
   [clojure.data.json :as json]))

(defn- camel-case
  [map]
  (transform-keys 
     ->camelCaseString
     map))

(defn- get-user
  [user-id jwt]
  (let [details (dao/find-by-id user-id)]
     (if (and jwt (jwt/ok? jwt user-id))
       (dissoc details :password) 
       (dissoc details :password :email :last-name :city :state) 
       )))

(defn serialize
  [user]
  (let [iso-formatter (format/formatters :date-time)]
    (assoc user :created (format/unparse iso-formatter (:created user)))
    ))

(defn gen-id!
  []
  (str (java.util.UUID/randomUUID)))

(defn details
  [request]
  (let [user-id (:user-id (:params request))
        jwt (jwt/extract-token (:headers request))]
    (if (nil? user-id)
      {:status 400}
      {:body (camel-case (serialize (get-user user-id jwt)))})))

(defn- send-reset-pass!
  [to id]
  (let [back-url (str (env :front-addr) "/#!/resetPassword?id=" id)]
    (http/post
     (str (env :notification-addr) "/notify") 
     {:content-type :json
      :headers (jwt/build-header "lolx_auth" to)
      :body (json/write-str {:type "reset" :email to :context {:url back-url}})})))

(defn register
  [request]
  (let [{first-name :firstName 
         last-name :lastName 
         email :email 
         password :password 
         location :location
         phone :phone} (:body request)]
    (if (not (validation/registration-valid? first-name last-name email phone password location))
      {:status 400}
      (do
       (if (dao/add-user (gen-id!) first-name last-name email phone location (digest/sha-256 password))
         {:status 200}
         {:status 409}
         )))))

(defn update-account
  [request]
  (let [user-id (get-in request [:params :user-id])
        jwt (jwt/extract-token (:headers request))
        {email :email
         phone :phone
         first-name :firstName 
         last-name :lastName
         location :location} (:body request)]
    (if (not (validation/update-account-valid? email phone first-name last-name location))
      {:status 400}
      (do
       (if (and jwt (jwt/ok? jwt user-id))
         (do 
           (dao/update user-id email phone first-name last-name location)
           {:status 200})
         {:status 401}
       )))))

(defn change-password
  [request]
  (let [user-id (get-in request [:params :user-id])
        jwt (jwt/extract-token (:headers request))
        {new-password :newPassword 
         old-password :oldPassword} (:body request)]
    (if (not (validation/change-password-valid? new-password old-password))
      {:status 400}
      (do 
        (if (and jwt (jwt/ok? jwt user-id))
          (do 
            (let [user (dao/find-by-id user-id)]
              (if (= (digest/sha-256 old-password) (:password user)) 
                (do 
                  (dao/change-password user-id (digest/sha-256 new-password))
                  {:status 200}
                  )
                 {:status 409}
                )
             ))
           {:status 401}
          )))))

(defn reset-password
  [request]
  (let [{email :email} (:body request)
        user (dao/find-by-email email)
        ref-id (gen-id!)]
    (if user
          (do 
            (dao/reset-password (:id user) ref-id)
            (send-reset-pass! email  ref-id)
            {:status 200})
          {:status 401})))

(defn change-password-after-reset
  [request]
  (let [{pass :password} (:body request)
        reset-ref-id (get-in request [:params :reset-ref-id])
        user (dao/find-by-reset-ref-id reset-ref-id)]
    (if user
      (do
        (dao/change-password-after-reset reset-ref-id (digest/sha-256 pass))
        {:status 200}
        )
      {:status 409}
      )))
