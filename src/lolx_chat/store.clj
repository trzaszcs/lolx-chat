(ns lolx-chat.store
  (:require [clj-time.core :refer [now before?]]))


(defonce in-memory-db (atom []))

(defn add
  [chat-id msg-id type anounce-id recipient-id author anounce-author msg]
  (try
    (swap!
     in-memory-db
     #(conj
       %
       {:id chat-id
        :type type
        :anounce-id anounce-id
        :recipient-id recipient-id
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
    (if (or (= author (:author-id chat)) (= author (:anounce-author-id chat)) (= author (:recipient-id chat)))
      chat
      nil)))

(defn get-by-anounce-id
  [anounce-id authors]
  (let [in? (fn [col val] (some #(= val %) col))]
    (first
     (filter
      #(and (= anounce-id (:anounce-id %)) (in? authors (:author-id %)) (in? authors (:recipient-id %)))
      @in-memory-db
      ))
    )
  )

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
   #(or (= user-id (:author-id %)) (= user-id (:recipient-id %))))
   @in-memory-db
  )

(defn count-unread-messages
  [chat user-id]
  (let [read-time (get-in chat [:read user-id])
        opponent-messages (filter #(not (= (:author-id %) user-id)) (:messages chat))]
    (if read-time
      (count (filter #(before? read-time (:created %)) opponent-messages))
      (count opponent-messages)
      )
    )
  )

(defn count-unread-messages!
  [user-id]
  (reduce
   +
   (map
    #(count-unread-messages % user-id)
    (get-by-user-id user-id)))
  )
