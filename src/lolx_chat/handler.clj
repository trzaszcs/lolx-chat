(ns lolx-chat.handler)

(ns lolx-chat.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [compojure.handler :refer [site]]
            [lolx-chat.chat :refer [create append details find find-status user-chats count-unread-messages]]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.middleware.json :refer [wrap-json-response wrap-json-body]]
            [ring.middleware.reload :refer [wrap-reload]]
            [ring.adapter.jetty :as jetty]
            [environ.core :refer [env]]
            [camel-snake-kebab.core :refer :all]
            [camel-snake-kebab.extras :refer [transform-keys]]
            [clj-time.format :as format]
            [lolx-chat.scheduler :as scheduler]))

(defroutes app-routes
  (GET "/" [] "Lolx Chat")
  (POST "/chat" []  create)
  (GET "/chat/status" []  find-status)
  (GET "/chat/user" []  user-chats)
  (GET "/chat/unread" [] count-unread-messages)
  (PUT "/chat/:chat-id" []  append)
  (GET "/chat/status" [] find-status)
  (GET "/chat/:chat-id" [] details)
  (GET "/chat" []  find)
  (route/not-found "Not Found"))

(defn- camel-case
  [map]
  (transform-keys
   ->camelCaseString
   map))

(defn camel-case-response-converter
  [handler]
  (fn [request]
    (let [response (handler request)
          body (:body response)]
      (if (and body (coll? body))
        (assoc response :body (camel-case body))
        response
        )
      )))

(defonce iso-formatter (format/formatters :date-time))

(defn serialize-date
  [obj]
  (if (sequential? obj)
    (map serialize-date obj)
    (if (map? obj)
      (reduce
       (fn [set entry]
         (assoc set (first entry) (serialize-date (last entry)))
         )
       {}
       obj)
      (if (instance? org.joda.time.DateTime obj)
        (format/unparse iso-formatter obj)
        obj
        )
    )
  ))

(defn date-serializer
  [handler]
  (fn [request]
    (let [response (handler request)
          body (:body response)]
      (if (and body (coll? body))
        (assoc response :body (serialize-date body))
        response
        )
      )))

(def app
  (-> app-routes
      (wrap-json-body {:keywords? true :bigdecimals? true})
      (date-serializer)
      (camel-case-response-converter)
      (wrap-json-response)
      (wrap-defaults (assoc-in site-defaults [:security] {:anti-forgery false}))))

(def server (atom nil))

(defn- run-jetty
  [app port]
    (reset! server (jetty/run-jetty (site #'app) {:port port :join? false})))

(defn- start-scheduler
  []
  (scheduler/start (Integer/parseInt (env :period))))

(defn start-dev
  []
  ;(start-scheduler)
  (run-jetty (wrap-reload app '(lolx-chat.handler)) 8084))

(defn stop
  []
  ;(scheduler/stop)
  (.stop @server))

(defn -main [& [port]]
  ;(start-scheduler)
  (let [port (Integer. (or port (env :port) 5000))]
   (run-jetty app port)))
