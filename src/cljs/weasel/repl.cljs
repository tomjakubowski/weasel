(ns weasel.repl
  (:require [clojure.browser.event :as event :refer [event-types]]
            [clojure.browser.net :as net]
            [cljs.reader :as reader :refer [read-string]]
            [weasel.impls.websocket :as ws]))

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
                               ("No stacktrace available."))}))}))

(defn- repl-print
  [x]
  (if-let [conn @ws-connection]
    (net/transmit @ws-connection {:op :print :value (pr-str x)})))

(defn enable-repl-print!
  "Set *print-fn* to print in the connected REPL"
  []
  (set! *print-fn* repl-print))

(defn connect
  [repl-server-url & {:keys [verbose]}]
  (let [repl-connection (ws/websocket-connection)]
    (swap! ws-connection (constantly repl-connection))

    (event/listen repl-connection :opened
      (fn [evt]
        (net/transmit repl-connection (pr-str {:op :ready}))
        (enable-repl-print!)
        (when verbose (.info js/console "Opened Websocket REPL connection"))))

    (event/listen repl-connection :message
      (fn [evt]
        (let [{:keys [op] :as message} (read-string (.-message evt))
              response (-> message process-message pr-str)]
          (net/transmit repl-connection response))))

    (event/listen repl-connection :closed
      (fn [evt]
        (reset! ws-connection nil)
        (when verbose (.info js/console "Closed Websocket REPL connection"))))

    (event/listen repl-connection :error
      (fn [evt] (.error js/console "WebSocket error" evt)))

    (net/connect repl-connection repl-server-url)))
