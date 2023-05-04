# defapp

Very very simple library to initialise your app stack.

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

These 'things' also have dependencies. For eg., configuration must be
loaded before database connection pool can be initialised and the
database connection pool must be passed as an argument to the ring
handler which is required to start the http server.

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
port to which the ghosted http server is now bound.'defapp' solves this
problem by tracking intermediate states in an agent while keeping the
application simple.

## Terminology

Application state is called `app` and the app contains a list of `resources`. Application state can be `setup` or `tear(ed)-down`.





## Usage

To use this library add the following github packages repository to
your `project.clj`.

```
:repositories
[["sats" {:url "https://maven.pkg.github.com/sathyavijayan/defapp"
          :username :env/GH_PACKAGES_USR
          :password :env/GH_PACKAGES_PSW}]]
```

and the following to `:dependencies`.
Define 'resources' and 'app'.
```clojure
(defresource config
  (setup! [state-so-far]
    ;; load and return config
    configuration)

  (tear-down! [configuration]
    nil))

(defresource db-connection-pool
  (setup! [{:keys [configuration] :as state-so-far}]
    (db/create-connection-pool (:db configuration)))

  (tear-down! [db-connection-pool]
    (.close db-connection-pool)))

(defapp my-app
  :resources [config db-connection-pool]
  :opts {:timeout-ms 120000})
```

To setup the application state, do:
```clojure
(setup! my-app)

;; 'deref' the app to wait for all the resources to initialize and to
;; obtain the app state. The behaviour is similar to 'dereffing' a
;; future - 'deref' will throw any exceptions that occurred during
;; setup (or tear-down). 'deref' waits for `timeout-ms` (default: 2
;; mins) for the actions (setup/tear-down) to complete after which a
;; timeout exception is thrown.

@(setup! my-app) ;;or

(do
  (setup! my-app)
  @my-app)
```

To inspect errors during setup/tear-down do:
```clojure
(errors my-app)
```

To stop everything do:
```clojure
@(tear-down! my-app)
```

If `setup!` or `tear-down!` fails midway, the function can be called
again to retry the operation.

# Notes about nomenclature
As mentioned above, defapp calls the application state `app` which is
made up of a list of `resources`.

I chose `app` over 'stack' to avoid confusion with the data structure
or the notion of a 'software stack'.I also rejected 'system/component'
to avoid confusion with other Clojure libraries that deal with runtime
application state.

> `resource` - Something that is available for use or that can be used
> for support or help.

Resources can be passed to functions that can 'use' them to achieve
specific tasks. A 'resource' can be anything - a connection pool, a
static map, reference to a thread, a function, etc., and it will still
fit the definition.

I chose `setup/tear-down` to denote actions over 'start/stop'
because it works better for all types of resources. For eg., you can
start/stop a connection pool, but not a piece of configuration or a
secret.


## Examples and Recipes
Coming soon.

## License

Copyright © 2022 - Sathya Vittal - Distributed under the [Apache License v2.0](http://www.apache.org/licenses/LICENSE-2.0)
