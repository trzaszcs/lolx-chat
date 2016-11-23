(ns lolx-chat.chat-test
  (:use midje.sweet)
  (:require
   [lolx-chat.chat :as chat]
   [lolx-chat.store :as store]
   [lolx-chat.jwt :as jwt]))


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
