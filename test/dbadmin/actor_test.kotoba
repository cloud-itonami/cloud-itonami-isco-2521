(ns dbadmin.actor-test
  (:require [clojure.test :refer [deftest is testing]]
            [dbadmin.actor :as actor]
            [dbadmin.store :as store]))

(defn- fresh-store []
  (let [st (store/mem-store)]
    (store/register-client! st {:client-id "client-1" :name "Mise Systems"})
    (store/register-database! st {:db-id "db-1" :client-id "client-1"
                                  :name "orders" :schema-version 7})
    (store/register-backup! st {:backup-id "bk-current" :db-id "db-1" :schema-version 7})
    st))

(deftest commits-a-nondestructive-draft
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:client-id "client-1" :op :draft-migration :stake :low
                 :db-id "db-1" :steps [{:kind :add-column :detail "orders.note"}]}
        result (actor/run-request! graph request {} "thread-1")]
    (is (= :done (:status result)))
    (is (some? (get-in result [:state :record])))
    (is (= 1 (count (store/records-of st "client-1"))))))

(deftest holds-an-apply-with-stale-backup
  (let [st (fresh-store)]
    (store/register-backup! st {:backup-id "bk-old" :db-id "db-1" :schema-version 5})
    (let [graph (actor/build-graph {:store st})
          request {:client-id "client-1" :op :apply-migration :stake :high
                   :db-id "db-1" :backup-id "bk-old"}
          result (actor/run-request! graph request {} "thread-2")]
      (is (= :hold (:disposition (:state result))))
      (is (empty? (store/records-of st "client-1"))))))

(deftest interrupts-then-applies-on-human-approval
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:client-id "client-1" :op :apply-migration :stake :high
                 :db-id "db-1" :backup-id "bk-current"}
        interrupted (actor/run-request! graph request {} "thread-3")]
    (is (= :interrupted (:status interrupted)))
    (is (empty? (store/records-of st "client-1")))
    (let [resumed (actor/approve! graph "thread-3")]
      (is (= :done (:status resumed)))
      (is (= 1 (count (store/records-of st "client-1")))))))
