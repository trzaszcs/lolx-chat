(ns lolx-chat.chat-test
  (:use midje.sweet)
  (:require
   [lolx-chat.chat :as chat]
   [lolx-chat.store :as store]
   [lolx-chat.jwt :as jwt]
   [lolx-chat.client :as client]
   [clj-time.core :refer [now]]))


(fact "should  '404' when no chat for anounceId"
      (let [user-id "234"
            opponent "3445"
            token "JWT"
            anounce-id "ANID"]
        (chat/find-status {:params {:anounceId anounce-id :opponent opponent} :headers {"authorization" (str "Bearer " token)}}) => {:status 404} 
        (provided
         (jwt/ok? token) => true
         (jwt/subject token) => user-id
         (store/get-by-anounce-id anounce-id [user-id opponent]) => nil)))

(fact "should return status by anounceId"
      (let [user-id "234"
            opponent "3445"
            token "JWT"
            anounce-id "ANID"
            chat-id "chatId"]
        (chat/find-status {:params {:anounceId anounce-id :opponent opponent} :headers {"authorization" (str "Bearer " token)}})
          => {:body {:id chat-id :unread-messages 3}}
        (provided
         (jwt/ok? token) => true
         (jwt/subject token) => user-id
         (store/get-by-anounce-id anounce-id [user-id opponent]) => {:id chat-id :messages [1 2 3]})))


(fact "should return user chats"
      (let [user-id "234"
            token "JWT"
            chat-author-id "1"
            chat-author-name "NAME"
            anounce-author-id "2"
            anounce-author-name "NAME2"
            anounce-id "ann-id"
            first-message "msg"
            chat {:id "chatId" :author-id chat-author-id :recipient anounce-author-id :anounce-author-id anounce-author-id :anounce-id anounce-id :created (now) :messages [{:msg first-message}]}
            anounce-title "some title"]
        (chat/user-chats {:headers {"authorization" (str "Bearer " token)}})
        => {:body
            {
             :chats [{
                      :anounce-id anounce-id
                      :anounce-title anounce-title
                      :anounce-author-id anounce-author-id
                      :anounce-author-name anounce-author-name
                      :id (:id chat)
                      :author-id chat-author-id
                      :author-name chat-author-name
                      :first-message first-message
                      :created (:created chat)
                      }]
             :total-count 1
             }
            }
        (provided
         (jwt/ok? token) => true
         (jwt/subject token) => user-id
         (store/get-by-user-id user-id) => [chat]
         (client/user-details [chat-author-id anounce-author-id]) => {chat-author-id {"firstName" chat-author-name} anounce-author-id {"firstName" anounce-author-name}}
         (client/anounce-bulk-details [(:anounce-id chat)]) => {(:anounce-id chat) {"title" anounce-title}}
         )))
