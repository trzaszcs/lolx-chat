(ns lolx-chat.notify-test
  (:use midje.sweet)
  (:require
   [lolx-chat.store :as store]
   [lolx-chat.notify :as notify]
   [clj-time.core :refer [now]]))


(fact "should extract unread stats"
      (let [chat {:author "1" :opponent "2" :messages [{:user-id "1" :read true}
                                                       {:user-id "1" :read false :notified true}
                                                       {:user-id "2" :read false}]
                  }]
        (notify/extract-unread-stats chat) => {"1" 1}
        ))
