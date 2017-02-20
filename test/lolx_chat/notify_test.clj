(ns lolx-chat.notify-test
  (:use midje.sweet)
  (:require
   [lolx-chat.store :as store]
   [lolx-chat.notify :as notify]
   [clj-time.core :refer [now]]))


(fact "should extract status for unread message"
      (let [recipient-id "recipientId"
            author-id "authorId"
            chat-id (store/create! "type" "anounce-id" recipient-id author-id "anounce-author" "msg")
            chat (store/get chat-id author-id)]
        (notify/extract-unread-stats chat) => {recipient-id 1}
        )
      )

(fact "should extract empty status for read message"
      (let [recipient-id "recipientId"
            author-id "authorId"
            chat-id (store/create! "type" "anounce-id" recipient-id author-id "anounce-author" "msg")]
        (store/mark-read-time! chat-id recipient-id)
        (notify/extract-unread-stats (store/get chat-id author-id)) => {}
        )
      )
