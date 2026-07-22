(ns dbadmin.advisor
  "DatabaseAdministrationAdvisor — proposes a DBA operation (draft a
  migration, apply a migration, tune an index) for a registered
  organization. Swappable mock/llm; the advisor ONLY proposes —
  `dbadmin.governor` checks backup-version equality and scans steps
  for destructive kinds independently. Modeled on
  cloud-itonami-isco-4311's advisor.

  A proposal: {:op :draft-migration|:apply-migration|:tune-index
               :effect :propose :db-id str :backup-id str-or-nil
               :steps [{:kind kw :detail str} ...]
               :stake kw :confidence n :rationale str}"
  (:require #?(:clj  [clojure.edn :as edn]
               :cljs [cljs.reader :as edn])))

(defprotocol Advisor
  (-advise [advisor store request] "request -> proposal map"))

(defn- infer [_store {:keys [op stake db-id backup-id steps] :as request}]
  {:op op
   :effect :propose
   :db-id db-id
   :backup-id backup-id
   :steps (vec steps)
   :stake (or stake :low)
   :confidence (case (or stake :low) :high 0.7 :medium 0.85 :low 0.95)
   :rationale (str "proposed " (name op) " for client " (:client-id request))})

(defn mock-advisor []
  (reify Advisor
    (-advise [_ store request] (infer store request))))

(def ^:private system-prompt
  "You are a database administration advisor. Given a request, propose
   an :op, the :db-id, the :backup-id (for applies), honest :steps with
   :kind keywords, an honest :confidence and a :stake. Never claim a
   backup exists — the governor checks version equality.")

(defn- parse-proposal [content]
  (try
    (let [p (edn/read-string content)]
      (if (map? p)
        (assoc p :effect :propose)
        {:op :unknown :effect :propose :confidence 0.0 :stake :high
         :rationale "unparseable LLM response"}))
    (catch #?(:clj Exception :cljs js/Error) _
      {:op :unknown :effect :propose :confidence 0.0 :stake :high
       :rationale "LLM response parse failure"})))

(defn llm-advisor
  [chat-model model-generate-fn gen-opts]
  (reify Advisor
    (-advise [_ _store request]
      (let [msgs [{:role :system :content system-prompt}
                  {:role :user :content (str "operation request: " (pr-str request))}]
            resp (model-generate-fn chat-model msgs gen-opts)]
        (parse-proposal (:content resp))))))
