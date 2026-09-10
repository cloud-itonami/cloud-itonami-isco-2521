(ns dbadmin.governor
  "DatabaseAdministrationGovernor — the independent safety/traceability
  layer for the ISCO-08 2521 community database-practice actor (itonami
  actor pattern, ADR-2607011000 / CLAUDE.md Actors section). Modeled on
  cloud-itonami-isco-4311's bookkeeping.governor. DBA-specific twist:
  applying a migration requires a backup whose schema version EQUALS
  the database's current version — a deterministic equality the
  advisor's 'we have backups somewhere' claim never satisfies — and
  destructive steps are detected by scanning the step list, not by
  trusting the advisor's risk self-assessment.

  HARD invariants (:hard? true, ALWAYS :hold, never overridable):
    1. client provenance — the organization must be registered.
    2. no-actuation      — proposal :effect must be :propose.
    3. database basis    — the cited database must be REGISTERED and
                           belong to this client (no invented systems).
    4. backup-before-apply — an :apply-migration must cite a REGISTERED
                           backup of THIS database whose
                           :schema-version equals the database's
                           CURRENT :schema-version. A backup of another
                           version is a backup of a different state;
                           version equality is arithmetic, not
                           reassurance.
  ESCALATION invariants (:escalate? true, human sign-off):
    5. :op :apply-migration (real system mutation — always human).
    6. destructive steps — any step whose :kind is :drop-table/
                           :drop-column/:truncate in a :draft-migration
                           (detected by scanning :steps, never by the
                           advisor's self-declared stake).
    7. low confidence (< `confidence-floor`)."
  (:require [dbadmin.store :as store]))

(def confidence-floor 0.6)
(def destructive-kinds #{:drop-table :drop-column :truncate})

(defn- destructive? [proposal]
  (and (= :draft-migration (:op proposal))
       (some #(contains? destructive-kinds (:kind %)) (:steps proposal))))

(defn- hard-violations [{:keys [request proposal]} client-record db backup-record]
  (let [{:keys [op db-id backup-id]} proposal
        db-op? (contains? #{:draft-migration :apply-migration :tune-index} op)
        apply? (= :apply-migration op)]
    (cond-> []
      (nil? client-record)
      (conj {:rule :no-client :detail "未登録 client"})

      (not= :propose (:effect proposal))
      (conj {:rule :no-actuation :detail "effect は :propose のみ許可（直接書込禁止）"})

      (and db-op? (nil? db-id))
      (conj {:rule :no-database :detail "対象 database の引用が必須"})

      (and db-op? db-id (nil? db))
      (conj {:rule :unknown-database :detail (str "未登録 database: " db-id)})

      (and db-op? db (not= (:client-id db) (:client-id request)))
      (conj {:rule :database-wrong-client :detail "database が別 client のもの"})

      (and apply? db (nil? backup-id))
      (conj {:rule :no-backup
             :detail "migration 適用はバックアップの引用が必須（安全網なき適用は承認不可）"})

      (and apply? db backup-id (nil? backup-record))
      (conj {:rule :unknown-backup :detail (str "未登録 backup: " backup-id)})

      (and apply? db backup-record (not= (:db-id backup-record) db-id))
      (conj {:rule :backup-wrong-database :detail "backup が別 database のもの"})

      (and apply? db backup-record (= (:db-id backup-record) db-id)
           (not= (:schema-version backup-record) (:schema-version db)))
      (conj {:rule :backup-version-mismatch
             :detail (str "backup schema-version " (:schema-version backup-record)
                          " ≠ 現行 " (:schema-version db)
                          "（別版のバックアップは別状態のバックアップ — 取り直すこと）")}))))

(defn check
  "Assess a proposal against `request`/`context`/`proposal` and a
  `store` implementing `dbadmin.store/Store`. Pure — never mutates the
  store."
  [request context proposal store]
  (let [client-record (store/client store (:client-id request))
        db (some->> (:db-id proposal) (store/database store))
        backup-record (some->> (:backup-id proposal) (store/backup store))
        hard (hard-violations {:request request :proposal proposal}
                              client-record db backup-record)
        hard? (boolean (seq hard))
        conf (or (:confidence proposal) 0.0)
        low? (< conf confidence-floor)
        risky-op? (or (= :apply-migration (:op proposal))
                      (destructive? proposal))]
    {:ok? (and (not hard?) (not low?) (not risky-op?))
     :violations hard
     :confidence conf
     :hard? hard?
     :escalate? (and (not hard?) (or low? risky-op?))}))
