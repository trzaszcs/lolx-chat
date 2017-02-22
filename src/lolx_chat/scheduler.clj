(ns lolx-chat.scheduler
  (:require
   [clojure.tools.logging :as log]
   [lolx-chat.notify :as n]))


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
