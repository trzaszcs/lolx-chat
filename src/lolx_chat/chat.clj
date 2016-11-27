(ns lolx-chat.chat
  (:require 
   [compojure.core :refer :all]
   [lolx-chat.store :as store]
   [lolx-chat.jwt :as jwt]
   [lolx-chat.client :as client]
   [ring.util.response :refer :all]
   [clj-time.format :as format]
   [clojure.tools.logging :as log]
   [clj-time.core :refer [after?]]))

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
    (assoc
      chat
      :messages (reverse (map #(enrich-message % user-details) (:messages chat)))
     )
    )
  )

(defn- extract-jwt-sub
  [request]
  (if-let [token (jwt/extract-jwt (:headers request))]
    (if (jwt/ok? token)
      (jwt/subject token)
      nil
      )
    ))

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
            (if chat
              (do
                (store/mark-read-time (:id chat) user-id)
                {:body (serialize (enrich chat))}
                )
              {:status 404}
              )
          ))
        {:status 400}
      )
    )))

(defn find
  [request]
  (if-let [anounce-id (get-in request [:params :anounceId])]
    (let [token (jwt/extract-jwt (:headers request))]
      (if (jwt/ok? token)
        (do
          (let [user-id (jwt/subject token)
                chat (store/get-by-anounce-id anounce-id user-id)]
            (if chat
              (store-mark-read-time (:id chat) user-id)
              {:body (serialize (enrich chat))}
              {:status 404}
              )
            ))
        {:status 400}
        )
      )))

(defn count-unread-messages
  [chat user-id]
  (let [read-time (get-in chat [:read user-id])
        opponent-messages (filter #(not (= (:author-id %) user-id)) (:messages chat))]
    (if read-time
      (count (filter #((after? read-time (:created %)) opponent-messages)))
      (count opponent-messages)
      )
    )
  )

(defn find-status
  [request]
  (let [user-id (extract-jwt-sub request)]
    (if user-id
      (do
        (let [anounce-id (get-in request [:params :anounceId])
              chat (store/get-by-anounce-id anounce-id user-id)]
          (if chat
            {:body {:id (:id chat)
                    :unread-messages (count-unread-messages chat user-id)}}
            {:status 404}
            )
          )
        )
      {:status 401}
      )
    )
  )

(defn append
  [request]
  (let [chat-id (get-in request [:params :chat-id])
        token (jwt/extract-jwt (:headers request))]
    (if (jwt/ok? token)
      (let [user-id (jwt/subject token)]
        (if-let [chat (store/get chat-id user-id)]
          (let [{msg :msg} (:body request)
                msg-id (gen-id!)]
            (store/append chat-id msg-id user-id msg)
            {:body {:id msg-id}}
            )
          )
        )
       {:status 401}
      )
    ))
