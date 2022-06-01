(ns sats.upit.core-test
  (:require [sats.upit.core :refer :all]
            [midje.sweet :refer :all]))

(let [s (atom nil)
      test-state-defs
      [{:key  :config
        :setup (constantly {:db-url "fake://db:port"})
        :tear-down (constantly nil)}

       {:key :db-pool
        :setup (fn [{:keys [config]}]
                 {:connection-pool (:db-url config)})
        :tear-down (constantly nil)}]]
  (facts "up!"
         (up! s test-state-defs)
         (fact "every 'thing' in the defs is initialised"
               (keys @s) => (contains [:config :db-pool] :in-any-order))
         (fact "'things' in the state defs are initialised in order"
               (get-in @s [:db-pool :connection-pool]) => (get-in @s [:config :db-url])))

  (facts "down!"
         (let [res (down! s test-state-defs)]
           (fact "always returns nil"
                 res => nil)
           (fact "clears the state"
                 @s => empty?))))

(def ^:dynamic *tracker* nil)

(fact "tear-down tears down 'things' in reverse order"
  (binding [*tracker* (atom [])]
    (let [test-state-defs
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
          a (atom nil)]
      (up! a test-state-defs)
      (down! a test-state-defs))
    @*tracker* => (contains [:baz :bar :foo])))
