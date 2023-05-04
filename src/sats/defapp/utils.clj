(ns sats.defapp.utils)

(defmacro defalias
  "Create a local var with the same value of a var from another namespace"
  [dest src]
  `(do
     (def ~dest (var ~src))
     (alter-meta! (var ~dest) merge (select-keys (meta (var ~src)) [:doc :arglists]))))
