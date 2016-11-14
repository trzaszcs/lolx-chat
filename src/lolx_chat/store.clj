(ns lolx-chat.store
  (:require [clj-time.core :refer [now]] ))



(defonce in-memory-db (atom []))

(defn add
  [chat-id msg-id type anounce-id author anounce-author msg]
  (try
    (swap! 
     in-memory-db
     #(conj
       %
       {:id chat-id
        :type type
        :anounce-id anounce-id
        :author-id author
        :anounce-author-id anounce-author
        :created (now)
        :messages [{:id msg-id :msg msg :author author :created (now)}]}
      ))
    true
    (catch IllegalStateException e false)))



(defn append
  [chat-id msg-id author msg]
  (try
    (swap! 
     in-memory-db
     (fn [chats]
       (map
        (fn [chat]
          (if (= chat-id (:id chat))
            (assoc chat :messages (conj (:messages chat) {:id msg-id :author-id author :msg msg :created (now)}))
            chat
            )
          )
        chats
        )))
    true
    (catch IllegalStateException e false)))

(defn get
  [chat-id author]
  (let [chat (first
              (filter
               #(= chat-id (:chat-id %))
               @in-memory-db
               ))]

    (when (or (= author (:author-id chat)) (= author (:anounce-author-id chat)))
      chat)))
