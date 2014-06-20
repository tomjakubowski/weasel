(ns weasel.repl.qml
  (:require
   [cljs.reader :as reader :refer [read-string]]))

(def ^:private ws-connection (atom nil))

(defn alive? []
  "Returns truthy value if the REPL is attempting to connect or is
   connected, or falsy value otherwise."
  (not (nil? @ws-connection)))

(defmulti process-message :op)

(defmethod process-message
  :error
  [message]
  (.error js/console (str "Websocket REPL error " (:type message))))

(defmethod process-message
  :eval-js
  [message]
  (let [code (:code message)]
    {:op :result
     :value (try
              {:status :success, :value (str (js* "eval(~{code})"))}
              (catch js/Error e
                {:status :exception
                 :value (pr-str e)
                 :stacktrace (if (.hasOwnProperty e "stack")
                               (.-stack e)
                               "No stacktrace available.")})
              (catch :default e
                {:status :exception
                 :value (pr-str e)
                 :stacktrace "No stacktrace available."}))}))

(defn repl-print
  [x]
  (if-let [conn @ws-connection]
    (.sendTextMessage @ws-connection (pr-str {:op :print :value (pr-str x)}))))

(def ws-status
  {0 :Connecting
   1 :Open
   2 :Closing
   3 :Closed
   4 :Error})

(defn create-ws [parent]
  (let [ws
        (.createQmlObject
         js/Qt
         "import Qt.WebSockets 1.0; WebSocket {url: \"ws://localhost:9001\"}"
         ;;"import Qt.WebSockets 1.0; WebSocket {}"
         parent
         "weasel.repl.qml.websocket")]
    (.log js/console (ws-status (.-status ws)))
    (.log js/console (ws-status (.-active ws)))
    (.log js/console (.-url ws))
    (.connect (.-onStatusChanged ws)
              (fn [status]
                (let [status (ws-status status)]
                  (.log js/console (str "Status: " status))
                  (cond
                   (= status :Open)
                   (.sendTextMessage ws (pr-str {:op :ready}))

                   (= status :Closed)
                   (set! (.-active ws) true)

                   (= status :Error)
                   (.error js/console "WebSocket error" (.-errorString ws))))))
    (.connect (.-onTextMessageReceived ws)
              (fn [msg]
                (.log js/console (str "Received: " msg))
                (let [{:keys [op] :as message} (read-string msg)
                      response (-> message process-message pr-str)]
                  (.sendTextMessage ws response))))
    (set! (.-active ws) true)
    ws))
