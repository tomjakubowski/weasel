# 0.3.0

## Breaking changes

* The `verbose` option to `weasel.repl/connect` now defaults to `true`.
* When `verbose` is false, the REPL client no longer logs Websocket
  errors to the console.

# 0.2.1

## Bug fixes

* Don't try to call string when no stacktrace available.
* Handle exceptions that are not instances of `js/Error` (e.g. `(throw
  "foo")`)

# 0.2.0

## Changes

* Requires ClojureScript 0.0-2202

## Enhancements

* Client: `weasel.repl/alive?` to determine if REPL is
  connected/connecting or dead. (#5)
* Client: `weasel.repl/connect` now takes optional callbacks
  `:on-open`, `:on-error`, `:on-close`.
