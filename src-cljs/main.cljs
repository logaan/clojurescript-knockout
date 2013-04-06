(ns hello-cljs.main
  (:use [jayq.core :only [$ css]]))

; Logging

(defn log [output]
  (.log js/console output))

(log "Hello, World!")

; jQuerying

(-> ($ "#copy")
    (css {:background "Blue"}))

