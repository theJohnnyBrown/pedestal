; Copyright 2014 Cognitect, Inc.

; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0)
; which can be found in the file epl-v10.html at the root of this distribution.
;
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
;
; You must not remove this notice, or any other, from this software.

(ns io.pedestal.http.secure-headers
  "Secure header settings applied in interceptors"
  (:require [io.pedestal.interceptor :as interceptor :refer [definterceptorfn after]]))

(def header-names
  {:hsts "Strict-Transport-Security"
   :frame-options "X-Frame-Options"
   :content-type "X-Content-Type-Options"
   :xss-protection "X-XSS-Protection"})

(def header-names-vec
  ["Strict-Transport-Security" "X-Frame-Options" "X-Content-Type-Options" "X-XSS-Protection"])

(defn hsts-header
  "Create a max-age (and optionally include subdomains) Strict-Transport header
  No arg version sets age at 1 year (31536000 seconds) and includes subdomains.
  You may want to use 1 hour (3600 secs), 1 day (86400 secs), 1 week (604800 secs),
  or 1 month (2628000 secs)"
  ([]
   "max-age=31536000; includeSubdomains")
  ([max-age-secs]
   (str "max-age=" max-age-secs))
  ([max-age-secs include-subdomains?]
   (str "max-age=" max-age-secs (if include-subdomains? "; includeSubdomains" ""))))

(defn frame-options-header
  "Create a custom polic value for Frame-Options header.
  No arg version returns most secure setting: DENY"
  ([]
   "DENY")
  ([policy]
   {:pre [(#{"DENY" "SAMEORIGIN"} policy)]}
   policy)
  ([allow-from-policy origin]
   {:pre [(= "ALLOW-FROM" allow-from-policy)]}
   (str allow-from-policy " " origin)))

(defn content-type-header
  "Create a custom value for content-type options.
  No arg version returns most secure setting: nosniff"
  ([]
   "nosniff")
  ([value]
   ;; Do not check currently
   (str value)))

(defn xss-protection-header
  "Create a custom value (and optionally mode) XSS-Protection header.
  No arg version returns the most secure setting: 1; block."
  ([]
   "1; mode=block")
  ([value]
   {:pre [(#{0 "0" 1 "1"} value)]}
   (str value))
  ([value mode]
   {:pre [(#{0 "0" 1 "1"} value)
          (#{"block"} mode)]}
   (str value "; mode=" mode)))

(defn create-headers
  ([]
   (create-headers (hsts-header) (frame-options-header) (content-type-header) (xss-protection-header)))
  ([hsts-settings frame-options-settings content-type-settings xss-protection-settings]
   (zipmap header-names-vec
           [hsts-settings frame-options-settings content-type-settings xss-protection-settings])))

(definterceptorfn secure-headers
  "Options are header values, which can be generated by the helper functions here"
  ([] (secure-headers {}))
  ([options]
   (let [{:keys [hsts-settings frame-options-settings
                 content-type-settings xss-protection-settings]
          :or {hsts-settings (hsts-header)
               frame-options-settings (frame-options-header)
               content-type-settings (content-type-header)
               xss-protection-settings (xss-protection-header)}} options
         sec-headers (create-headers hsts-settings
                                     frame-options-settings
                                     content-type-settings
                                     xss-protection-settings)]
     (after ::secure-headers
       (fn [{response :response :as context}]
         (assoc-in context [:response :headers] (merge sec-headers (:headers response))))))))

