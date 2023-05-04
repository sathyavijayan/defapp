(ns sats.defapp.core
  (:require [clojure.tools.logging :as log]
            [clojure.tools.logging :as log]
            [clojure.pprint :as pp]
            [clojure.spec.alpha :as s]))

(defn- setup-one!
  [app-state {k :key sfn :setup :as state-def}]
  (if (get app-state k)
    (do
      (log/warnf "skipping %s, already started" k)
      app-state)
    (do
      (log/infof "starting %s" k)
      (-> app-state
        (assoc k (sfn app-state))))))

(defn- tear-down-one!
  [app-state {k :key sfn :tear-down :as state-def}]
  (if (get app-state k)
    (do
      (log/infof "stopping %s" k)
      (when sfn (sfn app-state))
      (dissoc app-state k))
    (do
      (log/warnf "skipping %s, already stopped" k)
      app-state)))

(prefer-method print-method clojure.lang.IPersistentMap clojure.lang.IDeref)
(prefer-method print-method java.util.Map clojure.lang.IDeref)
(prefer-method pp/simple-dispatch
  clojure.lang.IPersistentMap clojure.lang.IDeref)
(prefer-method print-method  clojure.lang.IRecord
  clojure.lang.IDeref)

(defprotocol IApp
  (setup! [this])
  (tear-down! [this])
  (errors [this]))

(defprotocol IResource
  (update-resources [this resources]))

;; deref / @ will be the default way to get app state.
;; should we allow block wait ?
(defrecord App [app-agent resources]
  ;;-- AppState functions
  IApp
  (setup! [this]
    (when (agent-error app-agent)
      (restart-agent app-agent @app-agent))
    (doseq [r resources]
      (send-off app-agent setup-one! r))
    this)

  (tear-down! [this]
    (when (agent-error app-agent)
      (restart-agent app-agent @app-agent))
    (doseq [r (reverse resources)]
      (send-off app-agent tear-down-one! r))
    this)

  (errors [this]
    (agent-error app-agent))

  IResource
  (update-resources [this resources-to-update]
    (if (not= resources-to-update (:resources this))
      (let [updated (assoc this :resources resources-to-update)]
        (println "tearing down")
        (tear-down! this)
        (setup! updated)
        updated)
      this))

  clojure.lang.IDeref
  (deref [this]
    (await app-agent)
    @app-agent)

  clojure.lang.IBlockingDeref
  (deref [this timeout-ms timeout-val]
    (if (await-for timeout-ms app-agent)
      @app-agent
      timeout-val)))

(defn new-app
  ([resources]
   (->App (agent {}) resources)))




;; alternative thinking
(comment

  ;; stack, items ?
  ;; app, resources

  ;; `resource` - Something that is available for use or that can be
  ;; used for support or help.  This is much better naming - an app is
  ;; composed of resources.  resources can be passed to functions that
  ;; can 'use' them to achieve specific tasks.  a 'resource' can be
  ;; anything - a connection pool, a static map, reference to a
  ;; thread, a function, etc., and it will still fit the definition.
  ;; `setup`/`tear-down` are better names compared to start/stop
  ;; because it yields to the notion of a `resource` rather than a
  ;; component.

  #_(defapp my-app
      [{:key :xxx
        :setup (fn [x])
        :tear-down (fn [y])}])

  #_(defresource my-resource
      (setup      [{:keys [xx]}])
      (tear-down  [me]))

  ;; agree with BB that adding a form with dispatch can be a cognitive
  ;; load. Skip that form for now.

  ;; additional destructuring needs a bit of thought. Clojure style
  ;; destructuring is super useful at this state. So lets leave that feature out?

  ;; rename project to 'appy' / 'appdef' / 'defapp' ?
  ;; I think 'defapp' sounds very professional and nice compared to defapp ?

  ;; (defmulti..)
  ;; (defmethod.)

  ;; should the defs and the agent be in the same defrecord?

  )
