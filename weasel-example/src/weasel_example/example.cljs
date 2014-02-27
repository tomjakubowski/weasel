(ns weasel-example.example
  (:require [weasel.repl :as repl]))

(enable-console-print!)

(when-not (repl/alive?)
  (println "Printing to console.log")
  (repl/connect "ws://localhost:9001"
    :verbose true))
