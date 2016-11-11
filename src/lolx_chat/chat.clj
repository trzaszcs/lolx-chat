(ns lolx-chat.chat
  (:require 
   [compojure.core :refer :all]
   [lolx-chat.store :as store]
   [lolx-chat.jwt :as jwt]
   [lolx-chat.client :as client]
   [ring.util.response :refer :all]
   [digest :as digest]))

(defn gen-id!
  []
  (str (java.util.UUID/randomUUID)))


(defn- enrich-message
  [msg user-details]
  (assoc msg
         :author (get (get user-details (:user-id msg)) "firstName")))

(defn- enrich
  [chat]
  (let [user-ids [(:author-id chat) (:anounce-author-id chat)]
        user-details (client/user-details user-ids)]
    (assoc
      chat
      :messages (map #(enrich-message % user-details) (:messages chat))
     )
    )
  )

(defn create
  [request]
  (let [token (jwt/extract-jwt (:headers request))
        gen-id (gen-id!)]
    (if-let [ok? (jwt/ok? token)]
      (let [{type :type msg :msg anounce-id :anounceId} (:body request)
            issuer (jwt/issuer token)]
        (if-let [anounce-details (client/anounce-details)]
          (store/add gen-id (gen-id!) type anounce-id issuer  (:author-id anounce-details) msg)
          {:body {:id gen-id}}
          )
        )
      )
    {:status 400}
    ))

(defn get
  [request]
  (if-let [chat-id (get-in request (:param :chat-id))]
    (let [token (jwt/extract-jwt (:headers request))]
      (if-let [ok? (jwt/ok? token)]
        (let [issuer (jwt/issuer token)
              chat (store/get chat-id issuer)]
          {:body (enrich chat)}
        )
      {:status 400}
      )
    )))

(defn append
  [request]
  (let [chat-id (get-in request (:param :chat-id))
        token (jwt/extract-jwt (:headers request))]
    (when (jwt/ok? token)
      (let [issuer (jwt/issuer token)]
        (if-let [chat (store/get chat-id issuer)]
          (let [{msg :msg} (:body request)]
            (store/append chat-id issuer msg)
            {:status 200}
            )
          )
        )
      )
    {:status 400}
    ))
