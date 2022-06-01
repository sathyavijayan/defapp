# upit

Very very simple library to initialise your app stack.

`upit` is pronounced 'up-it' (something that helps to start your app
up). This is also an attempted word-play on a very popular, polarising South Indian dish called ["Upittu" (whoop-it-two)](https://en.wikipedia.org/wiki/Upma).

## Rationale

Contains functions to initialize and tear-down application
state. Every application has objects or functions that share runtime
state which includes things like:

- application configuration (loaded from a database or an external system)
- database connections/connection pool
- embedded http server
- threadpool executors
- application level cache (stored in an atom or similar)
- HTTP client connection pools
- metrics reporters

These 'things' also have dependencies. For eg., configuration must be loaded before database connection pool can be initialised and the database connection pool must be passed as an argument to the ring handler which is required to start the http server.

There are many clojure libraries that allow managing runtime state
like:
- https://github.com/stuartsierra/component
- https://github.com/tolitius/mount
- https://github.com/weavejester/integrant

While these libraries provide a robust mechanisms to handle runtime
state in complex applications, they also bring in unwanted
complexity. A much simpler way would be to just create the components
in the order required, collect them into a single hash map and pass it
around.  However, during development, especially while working with
the REPL, it is possible to end up with dirty state, for eg., if the
http server started ok, but the metrics reporter failed, the state so
far is lost. The REPL has to be restated in-order reclaim the HTTP
port to which the ghosted http server is now bound.'upit' solves this
problem by tracking intermediate states in an atom while keeping the
application simple.

## Usage

To use this library add the following github packages repository to your `project.clj`.

```
:repositories
[["sats" {:url "https://maven.pkg.github.com/sathyavijayan/upit"
          :username :env/GH_PACKAGES_USR
          :password :env/GH_PACKAGES_PSW}]]
```

and the following to `:dependencies`.
```
[sats/upit "0.0.1-SNAPSHOT"]
```

The 'things' that make up the runtime state can be defined as a list
of maps.

```clojure
(def app-def
 [{:key  :configuration
   :setup (fn [state-so-far]
           ;; load and return config
           configuration)
   ;; just return nil to remove from the state
   :tear-down (fn [configuration] nil)}
   {:key :db-connection-pool
    :setup (fn [{:keys [configuration] :as state-so-far}]
             (db/create-connection-pool (:db configuration)
    :tear-down (fn [db-connection-pool]
                 (.close db-connection-pool))}])
```

Then simply do:

```clojure
(require 'sats.upit)
;; an atom to hold the runtime state
(def app {})
;; `up!` will initialise each runtime 'thing' in the order in which
;; it is defined. This function can be called multiple times as the
;; 'things' which are already started, will be skipped.
(up! app app-def)
```

To stop eveything do:

```clojure
(down! app)
```

If `up!` or `down!` fails midway, the function can be called again to
retry the operation.

## Examples and Recipes
Coming soon.

## License

Copyright © 2022 - Sathya Vittal - Distributed under the [Apache License v2.0](http://www.apache.org/licenses/LICENSE-2.0)
