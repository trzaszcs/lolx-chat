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
            token "JWT"
            anounce-id "ANID"]
        (chat/find-status {:params {:anounceId anounce-id} :headers {"authorization" (str "Bearer " token)}}) => {:status 404} 
        (provided
         (jwt/ok? token) => true
         (jwt/subject token) => user-id
         (store/get-by-anounce-id anounce-id user-id) => nil)))

(fact "should return status by anounceId"
      (let [user-id "234"
            token "JWT"
            anounce-id "ANID"
            chat-id "chatId"]
        (chat/find-status {:params {:anounceId anounce-id} :headers {"authorization" (str "Bearer " token)}})
          => {:body {:id chat-id :unread-messages 3}} 
        (provided
         (jwt/ok? token) => true
         (jwt/subject token) => user-id
         (store/get-by-anounce-id anounce-id user-id) => {:id chat-id :messages [1 2 3]})))


(fact "should return user anounces"
      (let [user-id "234"
            token "JWT"
            chat-author-id "1"
            chat-author-name "NAME"
            anounce-author-id "2"
            anounce-author-name "NAME2"
            chat {:id "chatId" :author-id chat-author-id :anounce-author-id anounce-author-id :anounce-id "222" :created (now)}
            anounce-title "some title"]
        (chat/user-chats {:headers {"authorization" (str "Bearer " token)}})
        => {:body
            [{:id (:id chat)
              :anounce-title anounce-title
              :author-id chat-author-id
              :anounce-author-id anounce-author-id
              :author-name chat-author-name
              :anounce-author-name anounce-author-name
              :created (:created chat)}]}
        (provided
         (jwt/ok? token) => true
         (jwt/subject token) => user-id
         (store/get-by-user-id user-id) => [chat]
         (client/user-details [chat-author-id anounce-author-id]) => {chat-author-id {"firstName" chat-author-name} anounce-author-id {"firstName" anounce-author-name}}
         (client/anounce-bulk-details [(:anounce-id chat)]) => {(:anounce-id chat) {"title" anounce-title}}
         )))
