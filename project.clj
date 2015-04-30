(defproject weasel "0.7.0-SNAPSHOT"
  :description "websocket REPL environment for ClojureScript"
  :url "http://github.com/tomjakubowski/weasel"
  :license {:name "Unlicense"
            :url "http://unlicense.org/UNLICENSE"
            :distribution :repo}

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2850"]
                 [org.clojure/google-closure-library "0.0-20140718-946a7d39" :scope "provided"]
                 [http-kit "2.1.18"]]

  :repositories [["releases" {:url "https://clojars.org/repo"
                              :creds :gpg}]]

  :pom-addition [:developers [:developer
                              [:name "Tom Jakubowski"]
                              [:email "tom@crystae.net"]
                              [:url "https://github.com/tomjakubowski"]]]
  :profiles {:dev {:dependencies [[com.cemerick/piggieback "0.2.0"]]}}
  :source-paths ["src/clj" "src/cljs"]
  :scm {:name "git"
        :url "https://github.com/tomjakubowski/weasel"})
