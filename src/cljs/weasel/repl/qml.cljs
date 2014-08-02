(ns weasel.repl.qml
  (:require
   [cljs.reader :as reader :refer [read-string]]
   [weasel.impls.print :as wp]))

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

(defn connect [qml-parent repl-server-url & {:keys [verbose on-open on-error on-close]}]
  "Connects to a Weasel REPL socket at the provided repl-server-url. The underlying implementation
depends on Qt.WebSockets (found in Qt 5.3 or higher). Because this is a Qml object it requires
a non-nil parent object passed in as qml-parent."
  (let [repl-connection
        (.createQmlObject
         js/Qt
         (str "import Qt.WebSockets 1.0; WebSocket {url: \"" repl-server-url "\"}")
         ;;"import Qt.WebSockets 1.0; WebSocket {}"
         qml-parent
         "weasel.repl.qml.websocket")]
    (swap! ws-connection (constantly repl-connection))
    (swap! wp/print-fn (constantly repl-print))
    (.connect (.-onStatusChanged repl-connection)
              (fn [status]
                (let [status (ws-status status)]
                  (cond
                   (= status :Open)
                   (do
                     (.sendTextMessage repl-connection (pr-str {:op :ready}))
                     (when verbose (.info js/console "Opened Websocket REPL connection"))
                     (when (fn? on-open) (on-open)))

                   (= status :Closed)
                   (do
                     (reset! ws-connection nil)
                     (when verbose (.info js/console "Closed Websocket REPL connection"))
                     (when (fn? on-close) (on-close)))

                   (= status :Error)
                   (do
                     (.error js/console "WebSocket error" (.-errorString repl-connection))
                     (when (fn? on-error) (on-error (.-errorString repl-connection))))))))
    (.connect (.-onTextMessageReceived repl-connection)
              (fn [msg]
                (when verbose (.log js/console (str "Received: " msg)))
                (let [{:keys [op] :as message} (read-string msg)
                      response (-> message process-message pr-str)]
                  (when verbose (.log js/console (str "Sending: " response)))
                  (.sendTextMessage repl-connection response))))
    (set! (.-active repl-connection) true)
    repl-connection))
