(ns sats.defapp
  (:require [sats.defapp.utils :refer [defalias]]
            [sats.defapp.core :as core]
            [clojure.spec.alpha :as s]))

(defalias setup! core/setup!)
(defalias tear-down! core/tear-down!)
(defalias errors core/errors)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;                                                                            ;;
;;                        ----==| M A C R O S |==----                         ;;
;;                                                                            ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(s/def :defapp/map-or-symbol?
  (s/and
    vector?
    (s/cat
      :arg0
      (s/or
        :simple-symbol? simple-symbol?
        :map? map?))))

(s/def :defapp/setup!
  (s/and
    list?
    (s/cat
      :symbol? #{'setup!}
      :args :defapp/map-or-symbol?
      :body (s/* any?))))

(s/def :defapp/tear-down!
  (s/and list?
    (s/cat
      :symbol? #{'tear-down!}
      :args (s/and
              vector?
              (s/cat :arg0? simple-symbol?))
      :body (s/* any?))))

(s/def :defapp/resource
  (s/cat
    :setup! :defapp/setup!
    :tear-down! :defapp/tear-down!))

(def resource-conformer (s/conformer :defapp/resource))

(defn- conform-and-assert-defresource-form
  [form]
  (let [r (s/conform :defapp/resource form)]
    (assert (not (s/invalid? r)) (s/explain-str resource-conformer form))
    r))

(defn build-setup-form
  [resource-name form]
  (let [fn-name (symbol (format "%s-setup!" resource-name))
        [_ setup-args & setup-body] (->> form
                                      (filter #(-> % first (= 'setup!)))
                                      first)]
    `(fn ~fn-name ~setup-args ~@setup-body)))

(defn build-tear-down-form
  [resource-name form]
  (let [fn-name (symbol (format "%s-tear-down!" resource-name))
        [_ tear-down-args & tear-down-body] (->> form
                                              (filter #(-> % first (= 'tear-down!)))
                                              first)]
    `(fn ~fn-name ~tear-down-args ~@tear-down-body)))

(defmacro defresource
  "creates someresource that can be started or stopped"
  [name & body]
  (let [id (keyword name)
        sym (symbol name)
        body' (sort-by (comp {'setup! 0 'tear-down! 1} first) < body)
        explanation (conform-and-assert-defresource-form body')
        setup-form (build-setup-form name body')
        tear-down-form (build-tear-down-form name body')]
    `(def ~sym
       (array-map
         :key ~id
         :setup ~setup-form
         :tear-down ~tear-down-form))))


(defmacro defapp
  [name & {:keys [resources opts]}]
  (let [app-name-sym (symbol name)
        resources' (vec resources)]
    `(let* [dv# (def  ~app-name-sym)]
           (if (.hasRoot dv#)
             (def ~app-name-sym
               (core/update-app ~app-name-sym
                 ~resources'
                 ~opts))
             (def ~app-name-sym
               (core/new-app
                 ~resources'
                 ~opts))))))
