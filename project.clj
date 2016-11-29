(defproject lolx-auth "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :min-lein-version "2.0.0"
  :main lolx-chat.handler
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [compojure "1.5.1"]
                 [ring/ring-defaults "0.2.1"]
                 [ring/ring-json "0.4.0"]
                 [digest "1.4.4"]
                 [clj-jwt "0.1.1"]
                 [ring/ring-jetty-adapter "1.4.0"]
                 [clj-http "3.3.0"]
                 [environ "1.0.0"]
                 [ring/ring-devel "0.2.0"]
                 [camel-snake-kebab "0.4.0"]
                 [org.clojure/tools.logging "0.3.1"]
                 [ch.qos.logback/logback-classic "1.1.7"]]
  :ring {:handler lolx-chat.handler/app}
  :uberjar-name "lolx-chat-standalone.jar"
  :plugins [[lein-ring "0.9.7"]
            [lein-midje "3.2.1"]
            [lein-environ "1.1.0"]]
  :profiles
  {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                        [ring/ring-mock "0.3.0"]
                        [midje "1.8.3"]]}})
