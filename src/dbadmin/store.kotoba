(ns dbadmin.store
  "SSoT for the ISCO-08 2521 community database-practice actor (itonami
  actor pattern, ADR-2607011000 / CLAUDE.md Actors section). Modeled on
  cloud-itonami-isco-4311's bookkeeping.store.

  Domain:

    client    — a registered organization (:client-id, :name)
    database  — a registered managed database {:db-id :client-id :name
                :schema-version}. The version is the SSoT the governor
                checks backups against.
    backup    — a registered backup {:backup-id :db-id :schema-version}.
                The ONLY admissible safety basis for applying a
                migration: a backup of a DIFFERENT schema version is a
                backup of a different database state.
    record    — a committed operating record (migration draft, applied
                migration, index tuning) — written ONLY via
                commit-record!.
    ledger    — append-only audit trail, commit or hold."
  )

(defprotocol Store
  (client [s client-id])
  (database [s db-id])
  (backup [s backup-id])
  (records-of [s client-id])
  (ledger [s])
  (register-client! [s client])
  (register-database! [s db])
  (register-backup! [s b])
  (commit-record! [s record])
  (append-ledger! [s fact]))

(defrecord MemStore [a]
  Store
  (client [_ client-id] (get-in @a [:clients client-id]))
  (database [_ db-id] (get-in @a [:databases db-id]))
  (backup [_ backup-id] (get-in @a [:backups backup-id]))
  (records-of [_ client-id] (filter #(= client-id (:client-id %)) (:records @a)))
  (ledger [_] (:ledger @a))
  (register-client! [s client]
    (swap! a assoc-in [:clients (:client-id client)] client) s)
  (register-database! [s db]
    (swap! a assoc-in [:databases (:db-id db)] db) s)
  (register-backup! [s b]
    (swap! a assoc-in [:backups (:backup-id b)] b) s)
  (commit-record! [s record]
    (swap! a update :records (fnil conj []) record) s)
  (append-ledger! [s fact]
    (swap! a update :ledger (fnil conj []) fact) s))

(defn mem-store
  ([] (mem-store {}))
  ([seed] (->MemStore (atom (merge {:clients {} :databases {} :backups {}
                                    :records [] :ledger []}
                                   seed)))))
