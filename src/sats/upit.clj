(ns sats.upit
  (:require [sats.upit.utils :refer [defalias]]
            [sats.upit.core :as core]))

(defalias up! core/up!)

(defalias down! core/down!)
