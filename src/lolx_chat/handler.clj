(ns lolx-chat.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [compojure.handler :refer [site]]
            [lolx-chat.chat :refer [create append details]]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.middleware.json :refer [wrap-json-response wrap-json-body]]
            [ring.middleware.reload :refer [wrap-reload]]
            [ring.adapter.jetty :as jetty]
            [environ.core :refer [env]]))

(defroutes app-routes
  (GET "/" [] "Lolx Chat")
  (POST "/chat" []  create)
  (PUT "/chat/:chat-id" []  append)
  (GET "/chat/:chat-id" [] details)
  (route/not-found "Not Found"))

(def app
  (-> app-routes
      (wrap-json-response)
      (wrap-json-body {:keywords? true :bigdecimals? true})
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
