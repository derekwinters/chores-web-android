# Context

This repo is an Android client for [chores-web](https://github.com/derekwinters/chores-web). Domain terms (Chore, Completion, Assignee, Completer, Credit, Amendment, Reassignment, Points Log, Activity Log, etc.) are defined canonically in [chores-web's CONTEXT.md](https://github.com/derekwinters/chores-web/blob/main/CONTEXT.md) — this repo does not maintain a separate copy, to avoid drift between the two.

Add terms here only when they are specific to this Android client and have no equivalent in chores-web (e.g. UI-only or platform-only concepts).

## Android-only terms

- **Poll Interval** — the device-local cadence at which the WorkManager `NotificationPollWorker`
  fetches notifications from the backend (issue #44). A per-device setting (stored in
  `NotificationSettingsStore`), configurable in `Settings ▸ Notifications`, with a 15-minute floor
  (WorkManager's periodic minimum) and a 60-minute default. Distinct from any server concept — the
  backend never sees it.
- **Offline Alert** — a local "not connected in X days" system notification the poll worker posts
  when the app hasn't successfully reached the backend within a configurable threshold (issue #44),
  computed from the last-successful-contact timestamp (`ConnectionStatusStore`). It has **no**
  server-side notification identity: no server id, never acknowledged, cleared on the next
  successful contact.
