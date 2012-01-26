(ns noir-mongo.views.welcome
  (:require [noir-mongo.views.common :as common]
            [noir.content.pages :as pages])
  (use noir.core
        hiccup.core
        hiccup.page-helpers)
    (:use somnium.congomongo)
    (:use [somnium.congomongo.config :only [*mongo-config*]]))

(defn split-mongo-url [url]
    "Parses the mongodb url from heroku"
    (let [matcher (re-matcher #"^.*://(.*?):(.*?)@(.*?):(\d+)/(.*)$" url)]
        (when (.find matcher)
            (zipmap [:match :user :pass :host :port :db] (re-groups matcher)))))

(defn maybe-init []
    "Checks if connection and collection exist, otherwise initialise."
    (when (not (connection? *mongo-config*))
        (let [mongo-url (get (System/getenv) "MONGOHQ_URL")
              config (split-mongo-url mongo-url)]
              (println "Initializing mongo @" mongo-url)
              (mongo! :db (:db config) :host (:host config) :port (Integer. (:port config)))
              (authenticate (:user config) (:pass config))
              (or (collection-exists? :firstcollection)
                (create-collection! :firstcollection)))))

(defn get-votes [name]
  (:value
   (fetch
   :firstcollection
   :where {:_id name}
   :one? true)))

(defpage "/" []
  (maybe-init)
  (common/layout
   [:p "Emacs or Vim? You decide!"]
   [:p "<a href=\"/emacs\">Vote for Emacs</a> Votes so far: "(get-votes "emacs")]
   [:p "<a href=\"/vim\">Vote for Vim</a> Votes so far: "(get-votes "vim")]))

(defpage "/:name" {:keys [name]}
    (maybe-init)
    (let [counter
        (fetch-and-modify
        :firstcollection ;;In collection named :firstcollection
        {:_id name} ;;find the counter record
        {:$inc {:value 1}} ;;Increment it
        :return-new? true :upsert? true)] ;;Insert if not there
         (common/layout
	  [:p "You voted for " name " which now has " (or (:value counter) 0) " votes"]
	  [:p "<a href=\"/\">Go Back</a>"])))
