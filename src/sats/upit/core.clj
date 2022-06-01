(ns sats.upit.core
  (:require [clojure.tools.logging :as log]))

(defn- setup-one
  [app-state {k :key sfn :setup :as state-def}]
  (if (get app-state k)
    (do
      (log/warnf "skipping %s, already started" k)
      app-state)
    (do
      (log/infof "starting %s" k)
      (-> app-state
          (assoc k (sfn app-state))))))


(defn- tear-down-one
  [app-state {k :key sfn :tear-down :as state-def}]
  (if (get app-state k)
    (do
      (log/infof "stopping %s" k)
      (when sfn (sfn app-state))
      (dissoc app-state k))
    (do
      (log/warnf "skipping %s, already stopped" k)
      app-state)))


(defn up!
  [app-state state-defs]
  (try
    (doseq [state-def state-defs]
      (swap! app-state setup-one state-def))
    (catch Throwable t
      (log/error t "Error initialising app-state. app-state may be dirty. Use stop! to clean up.")
      app-state)))


(defn down!
  [app-state state-defs]
  (try
    (doseq [state-def (reverse state-defs)]
      (swap! app-state tear-down-one state-def))
    (catch Throwable t
      (log/error t "Failed to wind-down app-state. app-state may be dirty.")
      app-state)))
