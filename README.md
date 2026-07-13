# cloud-itonami-isco-2521

**Community Database Practice** — the ISCO-08 2521 (Database Designers
and Administrators) actor, an ISCO **Wave 0** occupation per
ADR-2607121000: pure-cognitive work, the LLM-first wave, no robotics
gate.

**Maturity: `:implemented`** — DatabaseAdministrationAdvisor ⊣
DatabaseAdministrationGovernor as a langgraph StateGraph
(`intake → advise → govern → decide → commit/hold`, human-approval
interrupt), modeled on cloud-itonami-isco-4311's bookkeeping actor.
13 tests / 28 assertions green.

The DBA-specific HARD invariant: **backup-before-apply with version
equality** — applying a migration requires a REGISTERED backup of THIS
database whose schema version equals the database's CURRENT version.
A backup of another version is a backup of a different state; version
equality is arithmetic, not reassurance, and "we have backups
somewhere" is never enough at any confidence. Also HARD: invented or
foreign databases, unregistered organization, non-`:propose` effect.

Escalations (always human sign-off): `:apply-migration` (real system
mutation, even with a valid backup), **destructive steps detected by
scanning the step list** (`:drop-table`/`:drop-column`/`:truncate` —
never by trusting the advisor's self-declared stake), low
confidence (< 0.6).

AGPL-3.0-or-later, forkable by any qualified operator. Part of the
[cloud-itonami](https://itonami.cloud) open business fleet.
