(ns csv-zen.dashboard.views
  (:require [hiccup.core :refer [html h]]
            [hiccup.page :as page]))

(defn page []
  (page/html5
    {:lang :en}
    [:head
     [:meta {:charset "utf-8"}]
     [:meta {:http-equiv "X-UA-Compatible"
             :content "IE=edge"}]
     [:meta {:name "viewport"
             :content "width=device-width, initial-scale=1"}]
     ;; the above 3 meta tags must come first
     [:link {:rel "stylesheet"
             :href "https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css"
             :integrity "sha384-BVYiiSIFeK1dGmJRAkycuHAHRg32OmUcww7on3RYdg4Va+PmSTsz/K68vbdEjh4u"
             :crossorigin "anonymous"}]
     [:link {:rel "stylesheet"
             :href "https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap-theme.min.css"
             :integrity "sha384-rHyoN1iRsVXV4nD0JutlnGaslCJuC7uwjduW9SVrLvRYooPp2bWYgmgJQIXwl/Sp"
             :crossorigin "anonymous"}]

     [:title "Dashboard"]]
    [:body
     [:div.container
      [:h1 "Dashboard"]

      [:form
       [:button.btn.btn-default "Click me!"]]
      ]
     [:script {:src "https://code.jquery.com/jquery-3.1.0.min.js"}]

     [:script {:src "https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/js/bootstrap.min.js"
               :integrity "sha384-Tc5IQib027qvyjSMfHjOMaLkfuWVxZxUPnCJA7l2mCWNIpG9mGCD8wGNIcPD7Txa"
               :crossorigin "anonymous"}]]))
