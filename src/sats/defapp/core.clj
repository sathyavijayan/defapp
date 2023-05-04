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
  (errors [this])
  (update-app [this resources opts]))

;; deref / @ will be the default way to get app state.
;; should we allow block wait ?
(defrecord App [app-agent resources opts]
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

  (update-app [this resources-to-update opts-to-update]
    (when (agent-error app-agent)
      (restart-agent app-agent @app-agent))

    (if (or (not= resources-to-update (:resources this))
            (and opts-to-update (not= opts-to-update opts)))
      (let [updated (cond-> (assoc this :resources resources-to-update)
                      opts-to-update (assoc :opts opts-to-update))]
        (when (not-empty @app-agent)
          (tear-down! this)
          (setup! updated))
        updated)
      this))

  clojure.lang.IDeref
  (deref [this]
    (if (await-for (:timeout-ms opts) app-agent)
      (if-let [err (agent-error app-agent)]
        (throw err)
        @app-agent)
      (throw (ex-info "Timed out waiting for app state change."
                      {:reason :timed-out
                       :errros (agent-error app-agent)})))))

(defn new-app
  ([resources opts]
   (->App (agent {}) resources
          (or opts {:timeout-ms (* 2 60 1000)}))))



(comment
  "When the user tries to deref the app, it should throw an
exception if the initialization failed. In an ideal world,"

  (defapp myapp
    :resources [a b c]
    :opts {})

  )
