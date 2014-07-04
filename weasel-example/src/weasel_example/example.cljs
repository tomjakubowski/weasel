(ns weasel-example.example
  (:require [weasel.repl :as repl]))

(if-not (repl/alive?)
  (repl/connect "ws://localhost:9001"))
