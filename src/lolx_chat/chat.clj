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
  (let [token (jwt/extract-jwt (:headers request))]
    (if (jwt/ok? token)
      (do
        (let [{type :type msg :msg anounce-id :anounceId} (:body request)
              user-id (jwt/subject token)
              gen-id (gen-id!)]
          (let [anounce-details (client/anounce-details anounce-id)]
            (if anounce-details
              (do
                (store/add gen-id (gen-id!) type anounce-id user-id  (:author-id anounce-details) msg)
                {:body {:id gen-id}}
                )
               {:status 400}
              )
            )
          )
        )
      {:status 401}
      )
    )
  )

(defn details
  [request]
  (println "get" request)
  (if-let [chat-id (get-in request [:params :chat-id])]
    (let [token (jwt/extract-jwt (:headers request))]
      (if-let [ok? (jwt/ok? token)]
        (let [user-id (jwt/subject token)
              chat (store/get chat-id user-id)]
          (println chat)
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
      (let [user-id (jwt/subject token)]
        (if-let [chat (store/get chat-id user-id)]
          (let [{msg :msg} (:body request)]
            (store/append chat-id user-id msg)
            {:status 200}
            )
          )
        )
      )
    {:status 400}
    ))
