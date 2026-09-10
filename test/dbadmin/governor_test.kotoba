(ns dbadmin.governor-test
  (:require [clojure.test :refer [deftest is testing]]
            [dbadmin.store :as store]
            [dbadmin.governor :as governor]))

(defn- fresh-store []
  (let [st (store/mem-store)]
    (store/register-client! st {:client-id "client-1" :name "Mise Systems"})
    (store/register-database! st {:db-id "db-1" :client-id "client-1"
                                  :name "orders" :schema-version 7})
    (store/register-backup! st {:backup-id "bk-current" :db-id "db-1" :schema-version 7})
    (store/register-backup! st {:backup-id "bk-old" :db-id "db-1" :schema-version 5})
    st))

(def ^:private req {:client-id "client-1"})

(deftest ok-on-nondestructive-draft
  (let [st (fresh-store)
        v (governor/check req {} {:op :draft-migration :effect :propose :db-id "db-1"
                                  :steps [{:kind :add-column :detail "orders.note"}]
                                  :confidence 0.9 :stake :low} st)]
    (is (:ok? v))))

(deftest hard-on-unregistered-client
  (let [st (fresh-store)
        v (governor/check {:client-id "nobody"} {}
                          {:op :tune-index :effect :propose :db-id "db-1"
                           :confidence 0.9 :stake :low} st)]
    (is (:hard? v))
    (is (some #(= :no-client (:rule %)) (:violations v)))))

(deftest hard-on-no-actuation-violation
  (let [st (fresh-store)
        v (governor/check req {} {:op :tune-index :effect :direct-write :db-id "db-1"
                                  :confidence 0.9 :stake :low} st)]
    (is (:hard? v))
    (is (some #(= :no-actuation (:rule %)) (:violations v)))))

(deftest hard-on-invented-database
  (let [st (fresh-store)
        v (governor/check req {} {:op :tune-index :effect :propose :db-id "db-ghost"
                                  :confidence 0.9 :stake :low} st)]
    (is (:hard? v))
    (is (some #(= :unknown-database (:rule %)) (:violations v)))))

(deftest hard-on-foreign-database
  (let [st (fresh-store)]
    (store/register-client! st {:client-id "client-2" :name "Other"})
    (let [v (governor/check {:client-id "client-2"} {}
                            {:op :tune-index :effect :propose :db-id "db-1"
                             :confidence 0.9 :stake :low} st)]
      (is (:hard? v))
      (is (some #(= :database-wrong-client (:rule %)) (:violations v))))))

(deftest hard-on-apply-without-backup
  (testing "no safety net, no apply"
    (let [st (fresh-store)
          v (governor/check req {} {:op :apply-migration :effect :propose :db-id "db-1"
                                    :backup-id nil :confidence 0.95 :stake :high} st)]
      (is (:hard? v))
      (is (some #(= :no-backup (:rule %)) (:violations v))))))

(deftest hard-on-stale-backup-version
  (testing "a backup of another schema version is a backup of a different
            state — version equality is arithmetic, not reassurance"
    (let [st (fresh-store)
          v (governor/check req {} {:op :apply-migration :effect :propose :db-id "db-1"
                                    :backup-id "bk-old" :confidence 0.99 :stake :high} st)]
      (is (:hard? v))
      (is (some #(= :backup-version-mismatch (:rule %)) (:violations v))))))

(deftest apply-with-current-backup-escalates-not-holds
  (testing "a properly backed apply is still a real mutation: human sign-off"
    (let [st (fresh-store)
          v (governor/check req {} {:op :apply-migration :effect :propose :db-id "db-1"
                                    :backup-id "bk-current" :confidence 0.9 :stake :high} st)]
      (is (not (:hard? v)))
      (is (:escalate? v)))))

(deftest destructive-draft-escalates-by-step-scan
  (testing "detected by scanning :steps, not by the advisor's self-declared stake"
    (let [st (fresh-store)
          v (governor/check req {} {:op :draft-migration :effect :propose :db-id "db-1"
                                    :steps [{:kind :add-column :detail "a"}
                                            {:kind :drop-table :detail "legacy_logs"}]
                                    :confidence 0.9 :stake :low} st)]
      (is (not (:hard? v)))
      (is (:escalate? v)))))

(deftest escalates-low-confidence
  (let [st (fresh-store)
        v (governor/check req {} {:op :tune-index :effect :propose :db-id "db-1"
                                  :confidence 0.3 :stake :low} st)]
    (is (not (:hard? v)))
    (is (:escalate? v))))
