(ns lolx-chat.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [compojure.handler :refer [site]]
<<<<<<< HEAD:src/lolx_chat/handler.clj
            [lolx-chat.chat :refer [create append get]]
=======
            [lolx-auth.user :refer [details 
                                    register 
                                    update-account 
                                    change-password 
                                    reset-password 
                                    change-password-after-reset]]
            [lolx-auth.authentication :refer [auth auth-facebook]]
>>>>>>> 028f004d19e548e5b8e5c219cab158c4082b4ac2:src/lolx_auth/handler.clj
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.middleware.json :refer [wrap-json-response wrap-json-body]]
            [ring.adapter.jetty :as jetty]
            [environ.core :refer [env]]))

(defroutes app-routes
<<<<<<< HEAD:src/lolx_chat/handler.clj
  (GET "/" [] "Lolx Chat")
  (POST "/chat" []  create)
  (PUT "/chat/:chat-id" []  append)
  (GET "/chat/:chat-id" [] get)
=======
  (GET "/" [] "Hello World")
  (POST "/users" []  register)
  (PUT  "/users/reset-password" [] reset-password)
  (PUT  "/users/reset-password/:reset-ref-id/change-password" [] change-password-after-reset)
  (GET  "/users/:user-id" [] details)
  (PUT  "/users/:user-id" [] update-account)
  (PUT  "/users/:user-id/change-password" [] change-password)
  (POST "/auth" [] auth)
  (POST "/auth-facebook" [] auth-facebook)
>>>>>>> 028f004d19e548e5b8e5c219cab158c4082b4ac2:src/lolx_auth/handler.clj
  (route/not-found "Not Found"))

(def app
  (-> app-routes
      (wrap-json-response)
      (wrap-json-body {:keywords? true :bigdecimals? true})
      (wrap-defaults (assoc-in site-defaults [:security] {:anti-forgery false}))))

(defn -main [& [port]]
  (let [port (Integer. (or port (env :port) 5000))]
    (jetty/run-jetty (site #'app) {:port port :join? false})))
