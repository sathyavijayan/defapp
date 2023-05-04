(ns sats.defapp.core-test
  (:require [sats.defapp.core :refer :all]
            [midje.sweet :refer :all]))

(facts "about defapp"
  (let [app (new-app
              [{:key  :config
                :setup (constantly {:db-url "fake://db:port"})
                :tear-down (constantly nil)}

               {:key :db-pool
                :setup (fn [{:keys [config]}]
                         {:connection-pool (:db-url config)})
                :tear-down (constantly nil)}]
              nil)]

    (facts "setup!"
      (setup! app)
      (fact "all resources are initialised"
        (keys @app) => (contains [:config :db-pool] :in-any-order))

      (fact "resources are initialised in order"
        (get-in @app [:db-pool :connection-pool]) => (get-in @app [:config :db-url])))

    (facts "tear-down!"
      (tear-down! app)
      (fact "clears the state"
        @app => empty?))))

(def ^:dynamic *tracker* nil)

(fact "tear-down tears down 'things' in reverse order"
      (binding [*tracker* (atom [])]
        (let [app (new-app
                   [{:key :foo
                     :setup (constantly {:val :foo})
                     :tear-down
                     (fn [app-state]
                       (swap! *tracker* conj :foo)
                       nil)}

                    {:key :bar
                     :setup (constantly {:val :bar})
                     :tear-down
                     (fn [app-state]
                       (swap! *tracker* conj :bar)
                       nil)}

                    {:key :baz
                     :setup (constantly {:val :baz})
                     :tear-down
                     (fn [app-state]
                       (swap! *tracker* conj :baz)
                       nil)}]
                   nil)]
          @(setup! app)
          @(tear-down! app))
        @*tracker* => (contains [:baz :bar :foo])))
