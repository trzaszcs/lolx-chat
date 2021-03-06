(ns lolx-chat.store-test
  (:use midje.sweet)
  (:require
   [lolx-chat.store :as store]
   [clj-time.core :refer [now plus minus minutes]]))


(fact "should mark message as read old"
      (reset! store/in-memory-db [])
      (let [recipient-id "recipient-id"
            author-id "author"
            chat-id (store/create! "someType" "anounce-id" recipient-id author-id "anounce-author" "msg")
            get-read-flag (fn [] (:read
                                  (first (get (store/get chat-id author-id) :messages))))]
        (get-read-flag) => false
        (store/count-unread-messages recipient-id) => 1
        (store/mark-read-time! chat-id recipient-id)
        (store/count-unread-messages recipient-id) => 0
        (get-read-flag) => true
        ))


(fact "should lock unread messages"
      (reset! store/in-memory-db [])
      (let [recipient-id "recipient-id"
            author-id "author"
            lock-time (plus (now) (minutes 10))
            created-date-from (plus (now) (minutes 3))
            chat-id (store/create! "someType" "anounce-id" recipient-id author-id "anounce-author" "msg1")
            chats-with-locked-messages (store/find-and-lock-unread-and-not-notified! lock-time created-date-from)]
        (count chats-with-locked-messages) => 1
        (:id (first chats-with-locked-messages)) => chat-id
        ))



(fact "should drop lock  unread messages"
      (reset! store/in-memory-db [])
      (let [recipient-id "recipient-id"
            author-id "author"
            lock-time (plus (now) (minutes 10))
            created-date-from (plus (now) (minutes 3))
            chat-id (store/create! "someType" "anounce-id" recipient-id author-id "anounce-author" "msg1")]
        (store/find-and-lock-unread-and-not-notified! lock-time created-date-from)
        (store/reset-lock! lock-time)
        (let [message (first (get (store/get chat-id author-id) :messages))]
          (:lock-time message) => nil
          (:notified message) => true
          )
        ))


(fact "should not return chats for other user"
      (reset! store/in-memory-db [])
      (store/create! "type" "id" "recipI" "authId" "auth" "msg1")
      (store/get-by-user-id "otherId") => '())
