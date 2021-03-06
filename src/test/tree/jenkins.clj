(ns test.tree.jenkins
  (:require [test.tree :as tree]
            [test.tree.debug :as debug]
            [test.tree.reporter :as report]
            [clojure.zip :as zip]
            [clojure.pprint :refer :all]
            [fn.trace :as trace]))

(defn run-suite
  "Run the suite with tracing, and output a testng result file with
   syntaxhighlighted trace. Also print a report of blockers. You can
   specify a list of namespaces and functions to trace (or not trace).
   Alternatively a function that generates a map of functions to trace
   and how deep to trace them (if both the list and function are
   specified, the list wins)."
  [suite & [{:keys [to-trace do-not-trace trace-depths-fn syntax-highlight-url]
             :or {do-not-trace #{}
                  syntax-highlight-url "/shared/syntaxhighlighter/"}
             :as opts}]]
  (with-redefs [tree/runner (-> tree/execute
                                debug/wrap-tracing
                                tree/wrap-blockers
                                tree/wrap-timer
                                tree/wrap-data-driven)
                trace/tracer trace/thread-tracer]
    (binding [*print-level* 20
              *print-length* 40
              *print-right-margin* 150
              *print-miser-width* 120
              report/syntax-highlight (report/syntax-highlighter syntax-highlight-url)]
      (trace/dotrace-depth (cond to-trace (->> to-trace
                                               trace/all-fns
                                               (remove do-not-trace)
                                               (zipmap (repeat nil)))
                                 trace-depths-fn (trace-depths-fn)
                                 :else [])
                           (let [reports (tree/run-suite suite opts)]
                             (println "----- Blockers -----\n ")
                             (pprint (report/blocker-report reports)))))))
