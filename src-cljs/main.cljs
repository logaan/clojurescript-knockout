(ns hello-cljs.main)

(defn log [output]
  (.log js/console output))

(log "Hello, World!")

