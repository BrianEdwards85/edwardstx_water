(ns us.edwardstx.service.water.orchestrator
     (:require [com.stuartsierra.component :as component]
               [us.edwardstx.common.events :refer [publish-event]]
               [clojure.tools.logging :as log]
               [manifold.deferred :as d])
     (:import [com.pi4j.wiringpi Gpio]))

(defn value->bool [v]
  (if (boolean? v)
    v
    (not (= v 0))))

(defn write [{:keys [events]} pin val]
  (log/info (format "Wrote value %b to pin %d" val pin))
  (publish-event events "pin.actuated" {:pin pin :value val})
  (Gpio/digitalWrite pin val))

(defn toggle
  ([orchestrator pin val] (toggle orchestrator pin val nil))
  ([orchestrator pin val delay]
   (if (or (nil? delay) (< delay 0))
     (write orchestrator pin val)
     (d/future
       (Thread/sleep delay)
       (write orchestrator pin val)))))

(defn actuate [orchestrator {:keys [pin value duration]}]
  (let [b (value->bool value)]
    (if (and (some? duration) (< 0 duration))
      (toggle orchestrator pin (not b) duration))
    (toggle orchestrator pin b)))

(defrecord Orchestrator [events]
  component/Lifecycle

  (start [this]
    this)

  (stop [this]
    this))

(defn new-orchestrator []
  (component/using
   (map->Orchestrator {})
   [:events]))
