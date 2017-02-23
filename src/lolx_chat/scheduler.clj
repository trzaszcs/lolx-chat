(ns lolx-chat.scheduler
  (:require
   [clojure.tools.logging :as log]
   [lolx-chat.notify :as n]
   [environ.core :refer [env]]))


(def continue (atom nil))


(defn- core
  [sleep-time]
  (while @continue
    (do
      (log/info "scheduler loop")
      (try
        (n/notify)
        (catch Exception e (log/warn "exception thrown from notify" e)))
      (Thread/sleep sleep-time)
      )
    ))

(defn start
  [sleep-time]
  (reset! continue true)
  (.start (Thread. (fn [] (core sleep-time))))
  )

(defn stop
  []
  (reset! continue false))


(defonce thread-started (atom false))

(defn delay-notification!
  []
  (let [sleep-in-sec (Integer/parseInt (env :delay-in-sec))
        mark-thread-as-stoped! (fn [] (reset! thread-started false))]
    (when (not @thread-started)
      (log/info (str "wait " sleep-in-sec " seconds"))
      (.start (Thread. (fn
                         []
                         (try
                           (Thread/sleep (* sleep-in-sec 1000))
                           (n/notify)
                           (mark-thread-as-stoped!)
                           (catch Exception e (log/warn "problem with delayed thread" e))
                           )
                         (mark-thread-as-stoped!))
      ))
      )
    )
)


