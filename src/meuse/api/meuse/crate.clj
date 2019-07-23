(ns meuse.api.meuse.crate
  (:require [meuse.api.meuse.http :refer [meuse-api!]]
            [meuse.api.params :as params]
            [meuse.auth.password :as auth-password]
            [meuse.auth.request :as auth-request]
            [meuse.db.category :as category-db]
            [meuse.db.crate :as crate-db]
            [clojure.set :as set]
            [clojure.tools.logging :refer [debug info error]]))

(defn format-crate
  [[crate-id crate-versions]]
  (let [versions (->> (map  #(set/rename-keys % {:version-version :version
                                                 :version-description :description
                                                 :version-yanked :yanked
                                                 :version-created-at :created-at
                                                 :version-updated-at :updated-at})
                            crate-versions)
                      (map #(select-keys %
                                         [:version :description :yanked
                                          :created-at :updated-at])))]
    {:id crate-id
     :name (-> crate-versions first :crate-name)
     :versions versions}))

(defn format-crates
  "Takes a list of crates and versions.
  Groups the versions by crates, and returns the result"
  [crates-versions]
  (->> (group-by :crate-id crates-versions)
       (map format-crate)))

(defmethod meuse-api! :list-crates
  [request]
  (auth-request/admin-or-tech?-throw request)
  (info "list crates")
  (let [crates (crate-db/get-crates-and-versions (:database request))]
    {:status 200
     :body {:crates (format-crates crates)}}))

(defmethod meuse-api! :get-crate
  [request]
  (auth-request/admin-or-tech?-throw request)
  (params/validate-params request ::get)
  (let [crate-name (get-in request [:route-params :name])
        _ (info "get crate" crate-name)
        database (:database request)
        crate (-> (crate-db/get-crate-and-versions database
                                                   crate-name)
                  format-crates
                  first)
        categories (category-db/by-crate-id database (:id crate))]
    {:status 200
     :body (assoc crate :categories categories)}))


