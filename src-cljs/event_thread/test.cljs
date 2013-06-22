(ns event-thread.test)

(defn test [expected actual]
  (if (not (= expected actual))
    (js/console.log expected actual)
    (js/console.log "Pass")))

