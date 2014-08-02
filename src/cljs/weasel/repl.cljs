(ns weasel.repl
  (:require [clojure.browser.event :as event :refer [event-types]]
            [clojure.browser.net :as net]
            [cljs.reader :as reader :refer [read-string]]
            [weasel.impls.websocket :as ws]
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
    (net/transmit @ws-connection {:op :print :value (pr-str x)})))

(defn connect
  [repl-server-url & {:keys [verbose on-open on-error on-close] :or {verbose true}}]
  (let [repl-connection (ws/websocket-connection)]
    (swap! ws-connection (constantly repl-connection))
    (swap! wp/print-fn (constantly repl-print))

    (event/listen repl-connection :opened
      (fn [evt]
        (net/transmit repl-connection (pr-str {:op :ready}))
        (when verbose (.info js/console "Opened Websocket REPL connection"))
        (when (fn? on-open) (on-open))))

    (event/listen repl-connection :message
      (fn [evt]
        (let [{:keys [op] :as message} (read-string (.-message evt))
              response (-> message process-message pr-str)]
          (net/transmit repl-connection response))))

    (event/listen repl-connection :closed
      (fn [evt]
        (reset! ws-connection nil)
        (when verbose (.info js/console "Closed Websocket REPL connection"))
        (when (fn? on-close) (on-close))))

    (event/listen repl-connection :error
      (fn [evt]
        (when verbose (.error js/console "WebSocket error" evt))
        (when (fn? on-error) (on-error evt))))

    (net/connect repl-connection repl-server-url)))
