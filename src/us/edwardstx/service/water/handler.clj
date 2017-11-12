(ns us.edwardstx.service.water.handler
  (:require [us.edwardstx.service.water.orchestrator :as orchestrator]
            [us.edwardstx.common.tasks :as tasks]
            [clojure.string :as str]
            [clojure.tools.logging      :as log]
            [com.stuartsierra.component :as component]
            [manifold.stream :as s]
            [manifold.deferred :as d]))


(defn str->map [v]
  (cond
    (map? v) v
    (string? v) (let [v (str/split v #",")]
               (case (count v)
                 2 (let [[pin value] v] {:pin (read-string pin) :value (read-string value)})
                 3 (let [[pin value duration] v] {:pin (read-string pin) :value (read-string value) :duration (read-string duration)})
                 nil))
    :else nil))

(defn actuate-handler [orchestrator {:keys [body response]}]
  (let [v (str->map body)]
    (orchestrator/actuate orchestrator v)
    (d/success! response v)))

(defn create-handlers [orchestrator tasks]
  (let [actuate-handler-stream (tasks/task-subscription tasks "pin.actuate")]
    (s/consume (partial actuate-handler orchestrator) actuate-handler-stream)
    (list actuate-handler-stream)))

(defrecord Handler [orchestrator tasks streams]
  component/Lifecycle

  (start [this]
    (let [streams (create-handlers orchestrator tasks)]
      (log/info "Initilized water service task handlers")
      (assoc this :streams streams)))

  (stop [this]
    (->> this
         :streams
         (map s/close!)
         doall)
    (assoc this :streams nil)))

(defn new-handler []
  (component/using
   (map->Handler {})
   [:orchestrator :tasks]))
