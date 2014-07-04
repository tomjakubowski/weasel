# weasel

WebSocket-connected REPL environment for ClojureScript.

[![Clojars Project](http://clojars.org/weasel/latest-version.svg)](http://clojars.org/weasel)

## why?

The traditional browser-connected REPL shopped with ClojureScript and
Chas Emerick's refactored browser REPL
[Austin](https://github.com/cemerick/austin) are both great ways to
evaluate ClojureScript inside a running browser from a REPL.  But
since their communications go over an `<iframe>` transport via
Closure's `goog.net.xpc` library, they can only be used in contexts
that allow iframes and on pages which are served over the same
protocol as the iframe (`http://`).  This means you can't use these
REPLs to connect to a page served off the filesystem (`file://`), in a
[Chrome extension](https://groups.google.com/forum/#!topic/clojure/lC8me2Gx_B4)
(`chrome-extension://`), or as part of a
[Spotify Application](https://developer.spotify.com/technologies/apps/)
(`sp://`).  Weasel provides a REPL connection to these and similar
environments.

## usage

Weasel is intended to be used with Chas Emerick's
[piggieback](https://github.com/cemerick/piggieback) nREPL middleware.
Once you've set that up, add weasel as a dependency (available from
[Clojars](http://clojars.org)) to your Leiningen `project.clj`:

```clojure
[weasel "0.3.0"]
```

Start up `lein repl` and piggieback the Weasel REPL environment onto
the nREPL session, optionally specifying a port (defaults to 9001) and
an address to bind to (defaults to "127.0.0.1").

```clojure
user> (require 'weasel.repl.websocket)
nil
user> (cemerick.piggieback/cljs-repl
        :repl-env (weasel.repl.websocket/repl-env
                   :ip "0.0.0.0" :port 9001))

<< started Weasel server on ws://0.0.0.0:9001 >>
Type `:cljs/quit` to stop the ClojureScript REPL
nil
```

In your project's ClojureScript, require the Weasel client namespace
and connect to the REPL, optionally specifying verbosity (defaults to
true):

```clojure
(ns my.cljs.core
  (:require [weasel.repl :as ws-repl]))

(ws-repl/connect "ws://localhost:9001" :verbose true)
```

Load the page in your WebSocket-enabled environment (probably a
browser) and start evaluating ClojureScript at the REPL:

```clojure
cljs.user> (= (js/Number. 34) (js/Number. 34))
false
cljs.user> (do (js/alert "Hello world!") 42)
42
```

Note that unless a client is connected to the WebSocket channel,
evaluation will fail:

```clojure
cljs.user> (+ 5 10)
java.io.IOException: No client connected to Websocket
nil
```

Moreover, only a single browser can be connected to the REPL at once.
Attempts to connect from another browser will fail with an error
message in the browser's console.  This may be addressed in future
versions.

## example

An example project is included in the `weasel-example` subdirectory of
this project.

## need help?

If you have any feedback or issues to report, feel free to open an
issue on [GitHub](https://github.com/tomjakubowski/weasel).
Otherwise, ping `dsrx` on freenode and I'll do my best to help you.
