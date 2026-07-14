# ADR-0007: Notification delivery is WorkManager polling, not FCM

Status: accepted · Issues: #43 · Epic: #41 (Notification System) · Contract: [derekwinters/chores-web-backend#39](https://github.com/derekwinters/chores-web-backend/issues/39)

## Context

The Notification System epic gives the app its first notification capability: surface
backend-generated notifications (v1: `chore_due`) as Android system notifications. The
conventional Android transport for this is Firebase Cloud Messaging (FCM) — the OS wakes the
app on a server push, with no client-side polling.

FCM, however, requires a Firebase/Google Cloud project and a `google-services.json` baked into
the app, plus a server component holding FCM credentials to send pushes. chores-web is a
**self-hosted** backend: each household runs its own instance, there is no central Google
project, and asking every self-hoster to provision Firebase is a non-starter. The backend#39
API is a plain REST surface (`GET /v1/notifications`, `POST /v1/notifications/{id}/ack`) with
**server-owned delivery state** — the first time a notification is listed, the server stamps its
`delivered_at`.

## Decision

Deliver notifications by **polling the backend from a periodic WorkManager worker**
(`NotificationPollWorker`), not via FCM.

- A unique `PeriodicWorkRequest` (`notification-poll`, `ExistingPeriodicWorkPolicy.KEEP`,
  `NetworkType.CONNECTED`) is enqueued post-login. The interval defaults to 60 minutes;
  issue #44 makes it user-configurable.
- The worker is a **plain `CoroutineWorker`** that pulls its dependencies from the Hilt graph at
  runtime via a Hilt `@EntryPoint` (`EntryPointAccessors.fromApplication`), **not** `@HiltWorker`
  / `HiltWorkerFactory`. WorkManager's default factory instantiates it from its
  `(Context, WorkerParameters)` constructor and the default `WorkManagerInitializer` is left in
  place — so no `Configuration.Provider` on `ChoresApplication` and no manifest initializer
  changes are needed. This deliberately avoids the `androidx.hilt:hilt-compiler` (hilt-work)
  annotation processor, whose 1.2.0 release cannot read AGP 9's Kotlin 2.x metadata and fails the
  kapt build; the `@EntryPoint` route reuses the existing Dagger-Hilt compiler only.
- **Delivery state stays server-owned**; the client does not try to model it. Because the
  server's `delivered_at` flips on the first *fetch* — and cannot distinguish "fetched" from
  "actually shown to the user" — the client keeps its **own local posted-ids record**
  (`PostedNotificationsStore`) to guarantee each item is posted at most once, even across worker
  re-runs and process restarts.
- **Acknowledgement is a user action**: `POST /v1/notifications/{id}/ack` fires only on
  notification tap (routed through `MainActivity`), never merely because an item was posted.

## Consequences

- **Latency is bounded by the poll interval and WorkManager's 15-minute periodic floor.** A
  `chore_due` notification can be up to one interval late; this is acceptable for chore reminders
  and is the explicit trade for zero Firebase dependency. Reducing the interval (issue #44)
  trades battery for freshness.
- No Firebase project, no `google-services.json`, no push credentials on the self-hosted server.
- `POST_NOTIFICATIONS` is a runtime permission (minSdk 33 → always applies). It is requested
  post-login; denial is non-fatal — polling and ack still run, only the system-notification
  posting is skipped. Items skipped for lack of permission are **not** marked posted, so they
  surface once the permission is later granted.
- Should a hosted/managed offering ever add a Google project, FCM could be layered in later as an
  alternative transport without changing the `getNotifications`/`ackNotification` contract or the
  posted-ids/ack semantics recorded here.
