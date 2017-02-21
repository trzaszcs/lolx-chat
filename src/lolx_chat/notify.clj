(ns lolx-chat.notify
  (:require
   [clj-time.core :refer [plus now minutes]]
   [clojure.tools.logging :as log]
   [lolx-chat.store :as store]
   [lolx-chat.client :as client]
   [environ.core :refer [env]]))


(defn- group-unread-messages
  [chat]
  (group-by
   :author-id
   (filter
    (fn [message]
      (and (not (:read message)) (not (:notified message)))
      )
    (:messages chat)))
  )

(defn extract-unread-stats
  [chat]
  (into
   {}
   (map
    (fn [[author-id messages]]
      {(if (= author-id (:author-id chat))
         (:recipient-id chat)
         (:author-id chat)
         ) (count messages)}
      )
    (group-unread-messages chat))
   ))

(defn send
  [chat]
  (let [unread-stats (extract-unread-stats chat)
        anounce (client/anounce-details (:anounce-id chat))
        user-details (client/user-details (keys unread-stats))]
    (doseq [[user-id messages-count] unread-stats]
      (let [email (get-in user-details [user-id "email"])]
        (log/info "sending email to " email)
        (client/send-unread-message-notification
         email
         {:url (str (env :front-url) "/#!/chat?chatId=" (:id chat))
          :author (get-in user-details [user-id "nick"])
          :anounceTitle (:title anounce)
          :count messages-count
          }
         )
        )
      )
    ))


(defn notify
  []
  (let [lock-time (plus (now) (minutes 5))
        create-time-from (plus (now) (minutes (env :create-time-from-in-minutes)))
        locked-chats (store/find-and-lock-unread-and-not-notified! lock-time create-time-from)]
    (when (not (empty? locked-chats))
      (log/info (count locked-chats) ":chats locked for 5 minutes")
      (doseq [chat locked-chats]
        (send chat))
      (store/reset-lock! lock-time)
      )
    )
  )
