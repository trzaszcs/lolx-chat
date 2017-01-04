(ns lolx-chat.details
  (:require 
   [compojure.core :refer :all]
   [lolx-chat.store :as store]
   [lolx-chat.jwt :as jwt]
   [lolx-chat.client :as client]
   [ring.util.response :refer :all]
   [clj-time.format :as format]
   [clojure.tools.logging :as log]
   [clj-time.core :refer [after?]]
   [clj-time.format :as format]))

(defn- enrich-message
  [msg user-details]
  (assoc msg
         :author (get (get user-details (:author-id msg)) "nick")))

(defn- enrich
  [chat]
  (let [messages (:messages chat)
        user-ids (distinct (map :author-id messages))
        user-details (client/user-details user-ids)]
    (assoc chat
           :messages
           (sort-by
            :created
            after?
            (map #(enrich-message % user-details) messages))
     )
    )
  )

(defn build-messages-for-chat
  [chat]
  (map
   (fn [message]
     (assoc message :type "user")
     )
   (:messages chat)
   )
  )

(defn decorate-with-chat
  [anounce-user-history chat]
  (if chat
    (assoc anounce-user-history
              :id (:id chat)
              :author-id (:author-id chat)
              :anounce-id (:anounce-id chat)
              :messages (concat (:messages anounce-user-history)(build-messages-for-chat chat)))
    anounce-user-history
    ))

(defn find-and-decorate-by-chat-id!
  [chat-id user-id]
  (let [chat (store/get chat-id user-id)]
    (when chat
      (store/mark-read-time (:id chat) user-id)
      )
    (enrich
     (->
      {:messages []}
      (decorate-with-chat chat)
      ))
   ))

(defn find-and-decorate-by-anounce-id!
  [anounce-id user-id]
  (let [chat (store/get-by-anounce-id anounce-id user-id)]
    (when chat
      (store/mark-read-time (:id chat) user-id)
    )
    (enrich
     (->
      {:anounce-id anounce-id :messages []}
      (decorate-with-chat chat)
      ))
   ))
