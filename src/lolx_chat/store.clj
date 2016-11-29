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
        :messages [{:id msg-id :msg msg :author-id author :created (now)}]}
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
               #(= chat-id (:id %))
               @in-memory-db
               ))]
    (if (or (= author (:author-id chat)) (= author (:anounce-author-id chat)))
      chat
      nil)))

(defn get-by-anounce-id
  [anounce-id author]
  (let [chat (first
              (filter
               #(= anounce-id (:anounce-id %))
               @in-memory-db
               ))]
    (if (or (= author (:author-id chat)) (= author (:anounce-author-id chat)))
      chat
      nil)))

(defn mark-read-time
  [chat-id user-id]
  (swap! 
   in-memory-db
   (fn [chats]
     (map
      (fn [chat]
        (if (= chat-id (:id chat))
          (assoc-in chat [:read user-id] (now))
          chat
          )
        )
      chats
      )))
  )

(defn get-by-user-id
  [user-id]
  (filter
   #(or (= user-id (:author-id %)) (= user-id (:anounce-author-id %))))
   @in-memory-db
   )
