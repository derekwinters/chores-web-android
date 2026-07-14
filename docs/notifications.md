# Notifications

How the Android client turns backend notifications into Android system notifications.
Introduced in issue #43 (Notification System epic, #41); the transport decision is
[ADR-0007](adr/0007-notification-polling-via-workmanager.md). This document is written to be
extended by the follow-up issues (#44 settings/offline alert, #45 in-app log + unread badge).

## Overview

The app **polls** the self-hosted backend on a schedule (no FCM — see ADR-0007) and posts each
new notification to the system tray exactly once. Tapping a notification opens the app and
acknowledges that item on the server.

```
NotificationScheduler ──enqueues──▶ NotificationPollWorker (periodic, @HiltWorker)
                                          │
                       GET /v1/notifications (un-dismissed)
                                          │
                    skip acked / dismissed / already-posted
                                          │
                    NotificationManagerCompat.notify(...)  ──tap──▶ MainActivity
                                          │                            │
                        record posted id + last-contact      POST /v1/notifications/{id}/ack
```

## The worker

`notifications/NotificationPollWorker.kt` is a `@HiltWorker CoroutineWorker`. Each run:

1. **Fetch** un-dismissed notifications via `NotificationRepository.getNotifications()`
   (`include_dismissed = false`). On failure it returns `Result.retry()` and records nothing.
2. **Record last-successful-contact** — `ConnectionStatusStore.recordSuccessfulContact(now)`,
   only on a successful poll (consumed by #44's "not connected in X days" offline alert).
3. **Post** each item that is not acknowledged, not dismissed, and not already locally recorded
   as posted, on the `chores` channel; then record its id.

Acknowledgement is **never** sent here — posting is not acking.

### Once-per-item guarantee

Delivery state is **server-owned**: the first `GET /v1/notifications` that returns a row stamps
its `delivered_at` server-side. That flag marks "fetched", which is not the same as "shown to the
user", so the client keeps its **own** record of which ids it has posted:

- `PostedNotificationsStore` (`isPosted` / `markPosted`), backed by plain `SharedPreferences`
  (`SharedPrefsConnectionStatusStore`, shared with `ConnectionStatusStore`).
- An id is recorded as posted only when it was actually posted. Items skipped because
  `POST_NOTIFICATIONS` is not granted are **not** recorded, so they still appear once the
  permission is later granted.

This survives worker re-runs and full process restarts.

### Channel

`notifications/NotificationChannels.kt` creates a single channel, id `chores` ("Chores due"),
idempotently (`ensureChannels` is safe to call before every post). minSdk is 33, so the
`NotificationChannel` API is always available.

## Scheduling

`NotificationScheduler.schedule(context)` enqueues a unique `PeriodicWorkRequest`:

- name `notification-poll`, `ExistingPeriodicWorkPolicy.KEEP` (re-invoking on each login is a
  no-op if already scheduled),
- default interval **60 minutes** (`DEFAULT_INTERVAL_MINUTES`) — WorkManager clamps periodic
  work to a 15-minute floor regardless,
- `NetworkType.CONNECTED` constraint.

It is called post-login from the app shell (`ui/ChoresApp.kt`). Issue #44 makes the interval
user-configurable and re-arms the work.

## Hilt worker wiring

- `ChoresApplication` implements `androidx.work.Configuration.Provider`, injecting
  `HiltWorkerFactory`, so `@HiltWorker` workers can be constructor-injected.
- The default `WorkManagerInitializer` is removed in `AndroidManifest.xml`
  (`androidx.startup.InitializationProvider` with a `tools:node="remove"` meta-data), so
  WorkManager uses the app's on-demand, Hilt-aware configuration instead of its default.

## Permission behavior

`POST_NOTIFICATIONS` is declared in the manifest and, because minSdk is 33, is always a runtime
permission. It is requested **post-login** from `ui/ChoresApp.kt` via the Activity Result API.
Denial is **non-fatal**: polling and tap-to-ack keep working; only the actual system-notification
posting is skipped (silently) until the permission is granted.

## Tap → acknowledge

Each posted notification carries a `PendingIntent` into `MainActivity` with the extra
`EXTRA_ACK_NOTIFICATION_ID`. On receiving it (`onCreate` / `onNewIntent`), `MainActivity` calls
`NotificationRepository.acknowledge(id)` — `POST /v1/notifications/{id}/ack`. The call is
fire-and-forget and idempotent server-side, so a re-delivered intent is harmless.

## API contract

Consumes chores-web-backend#39 (shape source of truth: the golden `openapi.json` in
`chores-web-docs`):

- `GET /v1/notifications?since=&include_dismissed=` → `NotificationDto[]`
- `POST /v1/notifications/{id}/ack`

Both ride the existing interceptor stack (`BaseUrlInterceptor` rewrites to the user-entered
backend, `AuthInterceptor` attaches the bearer token) via relative `v1/…` paths — no interceptor
changes. See `data/network/dto/NotificationDtos.kt` and `data/model/Notification.kt`.
