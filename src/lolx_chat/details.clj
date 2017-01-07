(ns lolx-chat.details
  (:require
   [lolx-chat.store :as store]
   [lolx-chat.client :as client]
   [clojure.tools.logging :as log]
   [clj-time.core :refer [before?]]))


(defn first-unread-message-id
  [chat user-id]
  (let [read-time (get-in chat [:read user-id])
        opponent-messages (filter #(not (= (:author-id %) user-id)) (:messages chat))]
    (if read-time
      (:id
       (first
        (filter #(before? read-time (:created %)) opponent-messages)
        ))
      (:id
       (first opponent-messages)
       )
      )
    )
  )

(defn- enrich-message
  [msg user-details]
  (assoc msg
         :author (get (get user-details (:author-id msg)) "nick")))

(defn- enrich
  [chat requestor-id]
  (let [messages (:messages chat)
        user-ids (distinct (map :author-id messages))
        user-details (client/user-details user-ids)]
    (assoc chat
           :first-unread-message-id (first-unread-message-id chat requestor-id)
           :messages (map #(enrich-message % user-details) messages)
     )
    )
  )

(defn find-and-decorate-by-chat-id!
  [chat-id user-id]
  (when-let [chat (store/get chat-id user-id)]
    (store/mark-read-time (:id chat) user-id)
    (enrich chat user-id)
   ))

(defn find-and-decorate-by-anounce-id!
  [anounce-id user-id]
  (when-let [chat (store/get-by-anounce-id anounce-id user-id)]
    (store/mark-read-time (:id chat) user-id)
    (enrich chat user-id)
  ))
