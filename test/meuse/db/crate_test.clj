(ns meuse.db.crate-test
  (:require [meuse.config :refer [config]]
            [meuse.db :refer [database]]
            [meuse.db.crate :refer :all]
            [meuse.fixtures :refer :all]
            [meuse.message :as message]
            [mount.core :as mount]
            [clojure.java.jdbc :as jdbc]
            [clojure.test :refer :all])
  (:import clojure.lang.ExceptionInfo))

(use-fixtures :once db-fixture)
(use-fixtures :each table-fixture)

(deftest ^:integration new-crate-test
  (let [request {:database database}
        crate {:metadata {:name "test1"
                          :vers "0.1.3"
                          :yanked false}}]
    (new-crate request crate)
    (is (thrown-with-msg? ExceptionInfo
                          #"already exists$"
                          (new-crate request crate)))
    (let [crate-db (get-crate-version database "test1" "0.1.3")]
      (is (uuid? (:crate-id crate-db)))
      (is (uuid? (:version-id crate-db)))
      (is (inst? (:version-created-at crate-db)))
      (is (inst? (:version-updated-at crate-db)))
      (are [x y] (= x y)
        "test1" (:crate-name crate-db)
        "0.1.3" (:version-version crate-db)
        false (:version-yanked crate-db)
        nil (:version-description crate-db)
        (:crate-id crate-db) (:version-crate-id crate-db)))
    (new-crate request (assoc-in crate [:metadata :vers] "2.0.0"))
    (let [crate-db (get-crate-version database "test1" "2.0.0")]
      (is (uuid? (:crate-id crate-db)))
      (is (uuid? (:version-id crate-db)))
      (is (inst? (:version-created-at crate-db)))
      (is (inst? (:version-updated-at crate-db)))
      (are [x y] (= x y)
        "test1" (:crate-name crate-db)
        "2.0.0" (:version-version crate-db)
        false (:version-yanked crate-db)
        nil (:version-description crate-db)
        (:crate-id crate-db) (:version-crate-id crate-db)))))

(deftest ^:integration update-yank-test
  (let [request {:database database}
        crate {:metadata {:name "test1"
                          :vers "0.1.3"
                          :yanked false}}]
    (new-crate request crate)
    (let [crate-db (get-crate-version database "test1" "0.1.3")]
      (is (uuid? (:crate-id crate-db)))
      (is (uuid? (:version-id crate-db)))
      (is (inst? (:version-created-at crate-db)))
      (is (inst? (:version-updated-at crate-db)))
      (are [x y] (= x y)
        "test1" (:crate-name crate-db)
        "0.1.3" (:version-version crate-db)
        false (:version-yanked crate-db)
        nil (:version-description crate-db)
        (:crate-id crate-db) (:version-crate-id crate-db)))
    (update-yank request "test1" "0.1.3" true)
    (let [crate-db (get-crate-version database "test1" "0.1.3")]
      (is (uuid? (:crate-id crate-db)))
      (is (uuid? (:version-id crate-db)))
      (is (inst? (:version-created-at crate-db)))
      (is (inst? (:version-updated-at crate-db)))
      (are [x y] (= x y)
        "test1" (:crate-name crate-db)
        "0.1.3" (:version-version crate-db)
        true (:version-yanked crate-db)
        nil (:version-description crate-db)
        (:crate-id crate-db) (:version-crate-id crate-db)))
    (update-yank request "test1" "0.1.3" false)
    (let [crate-db (get-crate-version database "test1" "0.1.3")]
      (is (uuid? (:crate-id crate-db)))
      (is (uuid? (:version-id crate-db)))
      (is (inst? (:version-created-at crate-db)))
      (is (inst? (:version-updated-at crate-db)))
      (are [x y] (= x y)
        "test1" (:crate-name crate-db)
        "0.1.3" (:version-version crate-db)
        false (:version-yanked crate-db)
        nil (:version-description crate-db)
        (:crate-id crate-db) (:version-crate-id crate-db)))
    (is (thrown-with-msg? ExceptionInfo
                          (re-pattern (str "crate state is already "
                                           (message/yanked?->msg false)))
                          (update-yank request "test1" "0.1.3" false)))))
