(ns lolx-chat.handler)

(ns lolx-chat.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [compojure.handler :refer [site]]
            [lolx-chat.chat :refer [create append details find find-status user-chats]]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.middleware.json :refer [wrap-json-response wrap-json-body]]
            [ring.middleware.reload :refer [wrap-reload]]
            [ring.adapter.jetty :as jetty]
            [environ.core :refer [env]]
            [camel-snake-kebab.core :refer :all]
            [camel-snake-kebab.extras :refer [transform-keys]]))

(defroutes app-routes
  (GET "/" [] "Lolx Chat")
  (POST "/chat" []  create)
  (GET "/chat/status" []  find-status)
  (GET "/chat/user" []  user-chats)
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

(def app
  (-> app-routes
      (wrap-json-body {:keywords? true :bigdecimals? true})
      (camel-case-response-converter)
      (wrap-json-response)
      (wrap-defaults (assoc-in site-defaults [:security] {:anti-forgery false}))))

(def server (atom nil))

(defn- run-jetty
  [app port]
    (reset! server (jetty/run-jetty (site #'app) {:port port :join? false})))

(defn start-dev
  []
    (run-jetty (wrap-reload app '(lolx-chat.handler)) 8084))

(defn stop
  []
  (.stop @server))

(defn -main [& [port]]
  (let [port (Integer. (or port (env :port) 5000))]
   (run-jetty app port)))
