(ns lolx-chat.chat
  (:require 
   [compojure.core :refer :all]
   [lolx-chat.store :as store]
   [lolx-chat.jwt :as jwt]
   [lolx-chat.client :as client]
   [ring.util.response :refer :all]
   [clj-time.format :as format]
   [clojure.tools.logging :as log]
   [clj-time.core :refer [after?]]
   [clj-time.format :as format]))

(defn gen-id!
  []
  (str (java.util.UUID/randomUUID)))

(defn- enrich-message
  [msg user-details]
  (assoc msg
         :author (get (get user-details (:author-id msg)) "firstName")))

(defn- enrich
  [chat]
  (let [messages (:messages chat)
        user-ids (distinct (map :author-id messages))
        user-details (client/user-details user-ids)]
    (assoc
      chat
      :messages (reverse (map #(enrich-message % user-details) messages))
     )
    )
  )

(defonce iso-formatter (format/formatter "yyyy-MM-dd'T'HH:mm:ssZ"))

(defn build-messages-for-chat
  [chat]
  (map
   (fn [message]
     (assoc message :type "user")
     )
   (:messages chat)
   )
)

(defn build-messages-for-request-order
  [request-order]
  (if request-order
    (do
      (let [build-init-msg (fn [request-order] {:msg "Wysłano zamówienie" :created (format/parse iso-formatter (get request-order "creationDate")) })
            build-status-change-msg (fn [msg request-order] {:msg msg :created (format/parse iso-formatter (get request-order "statusUpdateDate"))})]
        (map
         #(assoc % :type "userAction" :author-id (get "authorId" request-order))
         (case (get request-order "status")
           "WAITING"  [(build-init-msg request-order)]
           "ACCEPTED" [(build-init-msg request-order) (build-status-change-msg "Właściciel ogłoszenia zaapceptował zamówienie" request-order)]
           "REJECTED" [(build-init-msg request-order) (build-status-change-msg "Właściciel ogłoszenia odrzucił zamówienie" request-order)]
           )
         )
        )
      )
     []
    )
  )

(defn merge-messages
  [chat-messages request-order-messages]
  (sort-by
   :created
   (concat chat-messages request-order-messages))
  )

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
        (let [{type :type msg :msg anounce-id :anounceId} (:body request)
              user-id (jwt/subject token)
              gen-id (gen-id!)]
          (let [anounce-details (client/anounce-details anounce-id)]
            (if anounce-details
              (do
                (store/add gen-id (gen-id!) type anounce-id user-id  (:author-id anounce-details) msg)
                {:body {:id gen-id}}
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
                chat (store/get chat-id user-id)
                request-order (client/request-order (:anounce-id chat) user-id)]
            (when chat
              (store/mark-read-time (:id chat) user-id)
              )
            {:body (enrich {:messages (merge-messages (build-messages-for-chat chat) (build-messages-for-request-order request-order))})}
          ))
        {:status 400}
      )
    )))

(defn find
  [request]
  (if-let [anounce-id (get-in request [:params :anounceId])]
    (let [token (jwt/extract-jwt (:headers request))]
      (if (jwt/ok? token)
        (do
          (let [user-id (jwt/subject token)
                chat (store/get-by-anounce-id anounce-id user-id)
                request-order (client/request-order anounce-id user-id)]
            (when chat
              (store/mark-read-time (:id chat) user-id)
              )
            {:body (enrich {:messages (merge-messages (build-messages-for-chat chat) (build-messages-for-request-order request-order))})}
            ))
        {:status 400}
        )
      )))

(defn count-unread-messages
  [chat user-id]
  (let [read-time (get-in chat [:read user-id])
        opponent-messages (filter #(not (= (:author-id %) user-id)) (:messages chat))]
    (if read-time
      (count (filter #(after? read-time (:created %)) opponent-messages))
      (count opponent-messages)
      )
    )
  )

(defn find-status
  [request]
  (let [user-id (extract-jwt-sub request)]
    (if user-id
      (do
        (let [anounce-id (get-in request [:params :anounceId])
              chat (store/get-by-anounce-id anounce-id user-id)]
          (if chat
            {:body {:id (:id chat)
                    :unread-messages (count-unread-messages chat user-id)}}
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
          (let [{msg :msg} (:body request)
                msg-id (gen-id!)]
            (store/append chat-id msg-id user-id msg)
            {:body {:id msg-id}}
            )
          )
        )
       {:status 401}
      )
    ))

(defn user-chats
  [request]
  (let [user-id (extract-jwt-sub request)]
    (if user-id
      (do
        (let [chats (store/get-by-user-id user-id)]
          (if (not (empty? chats))
            (let [user-details (client/user-details (distinct (reduce #(conj %1 (:author-id %2) (:anounce-author-id %2)) [] chats)))
                  anounce-details (client/anounce-bulk-details (distinct (map #(:anounce-id %) chats)))
                  grouped-chats (group-by :anounce-id chats)]
              {:body (map
                      (fn [[anounce-id chats]]
                        (let [chat (first chats)]
                          {:anounce-id anounce-id
                           :anounce-title (get-in anounce-details [anounce-id "title"])
                           :anounce-author-id (:anounce-author-id chat)
                           :anounce-author-name (get-in user-details [(:anounce-author-id chat) "firstName"])
                           :chats (map
                                   (fn [chat]
                                     (let [first-message (get-in chat [:messages 0 :msg])]
                                       {:id (:id chat)
                                        :created (:created chat)
                                        :author-id (:author-id chat)
                                        :first-message (subs first-message 0 (min (count first-message) 20))
                                        :author-name (get-in user-details [(:author-id chat) "firstName"])}
                                       )
                                     )
                                   chats)
                           }))
                      grouped-chats)}
              )
             {:body []}
            )
        )
        )
      {:status 401}
    )
  ))
