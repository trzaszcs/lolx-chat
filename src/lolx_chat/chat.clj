(ns lolx-chat.chat
  (:require 
   [compojure.core :refer :all]
   [lolx-chat.store :as store]
   [lolx-chat.jwt :as jwt]
   [lolx-chat.client :as client]
   [ring.util.response :refer :all]
   [clj-time.format :as format]
   [digest :as digest]))

(defn gen-id!
  []
  (str (java.util.UUID/randomUUID)))

(defn serialize
  [chat]
  (let [iso-formatter (format/formatters :date-time)]
    (let [chat-updated (update chat :created #(format/unparse iso-formatter %))]
      (update chat-updated :messages (fn [messages]
                               (map
                                #(update % :created (fn [created] (format/unparse iso-formatter created)))
                                messages
                                )
                               ))
      ))
  )

(defn- enrich-message
  [msg user-details]
  (assoc msg
         :author (get (get user-details (:author-id msg)) "firstName")))

(defn- enrich
  [chat]
  (let [user-ids [(:author-id chat) (:anounce-author-id chat)]
        user-details (client/user-details user-ids)]
    (println "-->" user-details)
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
  (if-let [chat-id (get-in request [:params :chat-id])]
    (let [token (jwt/extract-jwt (:headers request))]
      (if (jwt/ok? token)
        (do
          (let [user-id (jwt/subject token)
              chat (store/get chat-id user-id)]
          {:body (serialize (enrich chat))}
          ))
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
