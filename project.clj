(defproject sats/upit "0.0.1-SNAPSHOT"
  :description "Very very simple and basic library to init your app stack."
  :url "https://github.com/sathyavijayan/upit"
  :license {:name "Apache License 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0"}

  :scm {:name "git"
        :url "https://github.com/sathyavijayan/upit.git"}

  :deploy-repositories
  [["releases" {:url "https://maven.pkg.github.com/sathyavijayan/upit"
                :username :env/GH_PACKAGES_USR
                :password :env/GH_PACKAGES_PSW
                :sign-releases false}]]

  :global-vars {*warn-on-reflection* true}

  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/tools.logging "0.3.1"]]

  :release-tasks [["vcs" "assert-committed"]
                  ["change" "version" "leiningen.release/bump-version" "release"]
                  ["vcs" "commit"]
                  ["vcs" "tag" "--no-sign"]
                  ["deploy" "releases"]
                  ["change" "version" "leiningen.release/bump-version"]
                  ["vcs" "commit"]]
  :profiles {:dev
             {:plugins
              [[lein-midje "3.1.1"]]
              :dependencies [[midje "1.9.9"]
                             [com.taoensso/timbre "5.2.1"]]}
             :test
             {:plugins
              [[lein-midje "3.1.1"]]
              :dependencies [[midje "1.9.9"]
                             [com.taoensso/timbre "5.2.1"]]}})
