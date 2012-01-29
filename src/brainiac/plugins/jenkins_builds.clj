(ns brainiac.plugins.jenkins-builds
  (:require [brainiac.plugin :as brainiac]
            [clojure.xml :as xml]
            [clojure.zip :as zip]
            [clojure.contrib.zip-filter.xml :as zf]))

(defn parse-project [node]
  (let [project-name (zf/xml1-> node (zf/attr :name))
        status (zf/xml1-> node (zf/attr :lastBuildStatus))]
    (assoc {}
      :name project-name
      :status status)))

(defn filter-status [builds status]
  (filter #(= (:status %) status) builds))

(defn build-status-string [build-data]
  (let [fail-count (count (filter-status build-data "Failure"))]
    (cond
      (> fail-count 0) (format "%d Failing Builds" fail-count)
      :default "All builds passing")))

(defn transform [stream]
  (let [xml (zip/xml-zip (xml/parse stream))
        build-data (zf/xml-> xml :Project parse-project)]
    (assoc {}
      :name "jenkins-builds"
      :type "list"
      :title (str "Jenkins: " (build-status-string build-data))
      :data (map #(:name %) (filter-status build-data "Failure")))))

(defn configure [{:keys [url username password program-name]}]
  (brainiac/schedule
    60000
    (brainiac/simple-http-plugin
      {:method :get :url url :basic-auth [username password]}
      transform program-name)))
