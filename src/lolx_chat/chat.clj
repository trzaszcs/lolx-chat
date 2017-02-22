(ns lolx-chat.chat
  (:require
   [compojure.core :refer :all]
   [lolx-chat.store :as store]
   [lolx-chat.jwt :as jwt]
   [lolx-chat.details :refer [find-and-decorate-by-chat-id! find-and-decorate-by-anounce-id!]]
   [lolx-chat.client :as client]
   [ring.util.response :refer :all]
   [clojure.tools.logging :as log]
   [clj-time.core :refer [before?]]))


(defn- extract-jwt-sub
  [request]
  (if-let [token (jwt/extract-jwt (:headers request))]
    (if (jwt/ok? token)
      (jwt/subject token)
      nil
      )
    ))

(defn create
  [request]
  (let [token (jwt/extract-jwt (:headers request))]
    (if (jwt/ok? token)
      (do
        (let [{type :type msg :msg anounce-id :anounceId opponent :opponent} (:body request)
              user-id (jwt/subject token)]
          (if anounce-id
            (if-let [anounce-details (client/anounce-details anounce-id)]
              (do
                {:body {:id (store/create! type anounce-id opponent user-id  (:author-id anounce-details) msg)}}
                )
              {:status 400}
              )
            )
          )
        )
      {:status 401}
      )
    )
  )

(defn details
  [request]
  (if-let [chat-id (get-in request [:params :chat-id])]
    (let [token (jwt/extract-jwt (:headers request))]
      (if (jwt/ok? token)
        (do
          (let [user-id (jwt/subject token)
                enriched-chat (find-and-decorate-by-chat-id! chat-id user-id)]
            (if enriched-chat
              {:body enriched-chat}
              {:status 404}
            )
          ))
        {:status 400}
      )
    )))

(defn find
  [request]
  (let [params (:params request)
        anounce-id (:anounceId params)
        opponent (:opponent params)]
    (let [token (jwt/extract-jwt (:headers request))]
      (if (jwt/ok? token)
        (do
          (let [user-id (jwt/subject token)
                enriched-chat (find-and-decorate-by-anounce-id! anounce-id user-id opponent)]
            (if enriched-chat
              {:body enriched-chat}
              {:status 404}
              )
            ))
        {:status 400}
        )
      )))

(defn find-status
  [request]
  (let [user-id (extract-jwt-sub request)]
    (if user-id
      (do
        (let [params (:params request)
              anounce-id (:anounceId params)
              opponent (:opponent params)
              chat (store/get-by-anounce-id anounce-id [user-id opponent])]
          (if chat
            {:body {:id (:id chat)
                    :unread-messages (store/count-unread-messages chat user-id)}}
            {:status 404}
            )
          )
        )
      {:status 401}
      )
    )
  )

(defn append
  [request]
  (let [chat-id (get-in request [:params :chat-id])
        token (jwt/extract-jwt (:headers request))]
    (if (jwt/ok? token)
      (let [user-id (jwt/subject token)]
        (if-let [chat (store/get chat-id user-id)]
          (let [{msg :msg} (:body request)]
            {:body {:id (store/append! chat-id user-id msg)}}
            )
          )
        )
       {:status 401}
      )
    ))

(defn user-chats
  [request]
  (let [user-id (extract-jwt-sub request)
        params (:params request)
        page (Integer/parseInt (or (:page params) "0"))
        items-per-page (Integer/parseInt (or (:itemsPerPage params) "20"))]
    (if user-id
      (do
        (let [user-chats (store/get-by-user-id user-id)
              total-count (count user-chats)
              chats (drop (* page items-per-page) (take items-per-page user-chats))]
          (let [user-details (client/user-details (distinct (reduce #(conj %1 (:author-id %2) (:anounce-author-id %2) (:recipient-id %2)) [] chats)))
                  anounce-details (client/anounce-bulk-details (distinct (map #(:anounce-id %) chats)))
                  grouped-chats (group-by :anounce-id chats)]
              {:body
               {
                :chats (map
                        (fn [chat]
                          (let [anounce-id (:anounce-id chat)
                                first-message (:msg (first (:messages chat)))]
                            {:anounce-id anounce-id
                             :anounce-title (get-in anounce-details [anounce-id "title"])
                             :anounce-author-id (:anounce-author-id chat)
                             :id (:id chat)
                             :created (:created chat)
                             :author-id (:author-id chat)
                             :first-message (subs first-message 0 (min (count first-message) 20))
                             :author-name (get-in user-details [(:author-id chat) "firstName"])
                             :recipient-id (:recipient-id chat)
                             :recipient-name (get-in user-details [(:recipient-id chat) "firstName"])
                             :unread-messages (store/count-unread-messages chat user-id)
                             }))
                        chats)
                :total-count total-count
                }
               }
              )
        ))
      {:status 401}
    )
    ))


(defn count-unread-messages
  [request]
  (let [user-id (extract-jwt-sub request)]
    (if user-id
      (do
        (let [unread-messages (store/count-unread-messages user-id)]
          {:body {:count unread-messages}}
        ))
      {:status 401}
    )
  ))
