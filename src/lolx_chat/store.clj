(ns lolx-chat.store
  (:require [clj-time.core :refer [now before?]]))


(defonce in-memory-db (atom []))


(defn- gen-id!
  []
  (str (java.util.UUID/randomUUID)))

(defn- message
  [msg-id author msg]
  {:id msg-id :msg msg :author-id author :created (now) :read false})

(defn create!
  [type anounce-id recipient-id author anounce-author msg]
  (let [chat-id (gen-id!)]
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
        :messages [(message (gen-id!) author msg)]}
       ))
    chat-id
    )
  )

(defn append!
  [chat-id author msg]
  (let [msg-id (gen-id!)]
    (swap!
     in-memory-db
     (fn [chats]
       (map
        (fn [chat]
          (if (= chat-id (:id chat))
            (assoc chat :messages (conj (:messages chat) (message (gen-id!) author msg)))
            chat
            )
          )
        chats
        )))
    msg-id))

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

(defn mark-read-time!
  [chat-id user-id]
  (swap!
   in-memory-db
   (fn [chats]
     (map
      (fn [chat]
        (if (= chat-id (:id chat))
          (update
           chat
           :messages
           (fn [messages]
             (map
              (fn [message]
                (if (not (= (:author-id message) user-id))
                  (assoc message :read true)
                  message
                  )
                )
              messages
              )
             )
           )
           chat
          )
        )
      chats))))

(defn get-by-user-id
  [user-id]
  (filter
   #(or (= user-id (:author-id %)) (= user-id (:recipient-id %))))
   @in-memory-db)

(defn count-unread-messages
  ([chat user-id]
   (count
    (filter
     #(and (not (:read %))(not (= user-id (:author-id %))))
     (:messages chat)))
   )
  ([user-id]
   (reduce
    +
    (map
     #(count-unread-messages % user-id)
     (get-by-user-id user-id))))
  )

(defn find-and-lock-unread-and-not-notified!
  [lock-time]
  (let [altered-chats (swap!
                       in-memory-db
                       (fn [chats]
                         (map
                          (fn [chat]
                            (update
                             chat
                             :messages
                             (fn [messages]
                               (map
                                (fn [message]
                                  (if (and
                                       (not (:read message))
                                       (not (:notified message))
                                       (or
                                        (nil? (:lock-time message))
                                        (before? (:lock-time message) (now))))
                                    (assoc message :lock-time lock-time)
                                    message
                                    )
                                  )
                                messages
                              ))
                             )
                          )
                          chats)))
        ]
    (filter
     (fn [chat]
       (some
        (fn [message]
          (= lock-time (:lock-time message))
          )
        (:messages chat))
       )
     altered-chats)))

(defn reset-lock-time!
  [lock-time]
  (swap!
   in-memory-db
   (fn [chats]
     (map
      (fn [chat]
        (update
           chat
           :messages
           (fn [messages]
             (map
              (fn [message]
                (if (= (:lock-time message) lock-time)
                  (dissoc (assoc message :notified true) :lock-time)
                  message
                  )
                )
              messages
              ))
           )
        )
      chats))))
