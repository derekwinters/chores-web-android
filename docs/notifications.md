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
NotificationScheduler ──enqueues──▶ NotificationPollWorker (periodic, plain CoroutineWorker)
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

`notifications/NotificationPollWorker.kt` is a plain `CoroutineWorker` (see "Hilt worker wiring"
below for why it isn't `@HiltWorker`). Each run:

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

It is called post-login from the app shell (`ui/ChoresApp.kt`).

### Configurable interval (issue #44)

The interval is no longer hardcoded: `NotificationScheduler` reads it from
`NotificationSettingsStore` (resolved from the Hilt graph via an `@EntryPoint`, the same pattern
the worker uses) so it stays a plain `object` callable from composition.

- `schedule(context)` (post-login) uses `ExistingPeriodicWorkPolicy.KEEP` — re-invoking on each
  login is a no-op if already scheduled.
- `reschedule(context)` (after the user changes the interval, from
  `SettingsNotificationsViewModel` via `NotificationRescheduler`) uses
  `ExistingPeriodicWorkPolicy.UPDATE` — it applies the new interval to the **same** unique work
  in place, so no duplicate request is ever created.

The chosen interval is coerced to WorkManager's **15-minute** periodic floor before enqueue.

## Settings (issue #44)

The `Settings ▸ Notifications` screen (`ui/settings/SettingsNotificationsScreen.kt`,
`…ViewModel.kt`) makes the notification behavior configurable. It sits in the `settings/*` sub-nav
graph (ADR-0003) and — unlike the admin-gated household config forms — is reachable by **every**
user, because its contents are per-user (aligned with how Theme preferences are exposed, ADR-0005).

**Storage split (deliberate):**

| Setting | Scope | Storage |
|---|---|---|
| Poll interval | Device | Local `NotificationSettingsStore` (SharedPreferences) |
| Offline-alert enabled + threshold days | Device | Local `NotificationSettingsStore` |
| Per-type enable/disable (`{"chore_due": true}`) | Account | Server, `GET/PUT /v1/notifications/preferences` |

Poll cadence and the offline threshold are **device** concerns (a phone on cellular may poll less
often than a tablet at home), so they live on the device. The per-type map is an **account**
concern — stored server-side so the backend can skip generating disabled types (backend#38, an
absent preference row = enabled) — round-tripped via `NotificationRepository.getPreferences()` /
`updatePreferences()`. The server per-type controls fail-soft: if that fetch fails the device-local
controls stay usable (important precisely when offline) and a non-blocking error banner is shown.

- **Poll interval**: a bounded picker offering only choices ≥ 15 min
  (15 / 30 min, 1 / 3 / 6 / 12 / 24 h); default 60 min. Changing it persists locally and re-arms
  the worker (`UPDATE`, above).
- **Notification types**: a toggle per server-returned type (v1: "Chore due"), persisted via `PUT`
  with success/error feedback (`SettingsBanner`).
- **Offline alert**: an on/off toggle (default on) plus a threshold-days field (default 3).

## Offline alert (issue #44)

When the app hasn't successfully reached the backend in a configurable number of days, the poll
worker posts a **local** "not connected" warning instead of silently doing nothing. It is computed
in `NotificationPollWorker` from #43's `ConnectionStatusStore.lastSuccessfulContact()` timestamp —
there is no backend interaction: this notification has no server id and is never acked.

- It is checked on a **failed** poll (the failure path is the point) and fires when the alert is
  enabled and `now − lastSuccessfulContact ≥ threshold`.
- **No pre-login firing**: if no successful contact was ever recorded, it does not fire.
- It uses its own stable notification id (`OFFLINE_ALERT_NOTIFICATION_ID`) on a separate
  **"Connection alerts"** channel, so re-posts *update* the one warning rather than stacking, and
  it is posted **at most once per breach** (latched via `NotificationSettingsStore.offlineAlertPosted`).
- The next **successful** contact cancels the notification and clears the latch, so a future breach
  can warn again.
- Like chore posts, it is skipped (without latching) when `POST_NOTIFICATIONS` isn't granted, so it
  can still fire once the permission is granted.

## In-app notification log (issue #45)

Beyond the system tray, the app has an **in-app Notification Log** (`ui/notifications/`) listing
the signed-in user's notifications from `GET /v1/notifications`. It is reached from a **top-bar
bell action** (present on every authenticated screen) rather than a bottom-nav tab — the bottom bar
is a fixed five (ADR-0004) and global actions live in the top bar (ADR-0005). Tapping the bell
navigates to the `notifications` route.

- **Read history is retained.** Unlike the tray posts (which skip acknowledged/dismissed items),
  the log fetches with `include_dismissed=false` and **keeps acknowledged items**, rendering them
  as read history visually distinguished from unread ones. Dismissed items stay server-filtered
  out; there is no dismissed-management UI in v1.
- **Unread = unacknowledged.** A row is "unread" when `acknowledged_at == null`
  (`Notification.isUnread`). Unread rows carry a leading accent bar + translucent fill + an unread
  dot and a **"Mark as read"** action; read rows render plain.
- **Acknowledge from the screen** calls the same `POST /v1/notifications/{id}/ack` the tray tap
  uses (one semantic, two entry points), via `NotificationRepository.acknowledge(id)`.
  `NotificationLogViewModel` applies it **optimistically** (the row flips to read immediately and
  reverts if the PUT fails), mirroring the Notifications settings screen's per-type toggle.

### Unread badge

The bell carries an **unread-count badge** sourced by `NotificationBadgeViewModel` — an
Activity-scoped sibling of `NavBadgeViewModel` (the due-now Chores badge) following the same
decoupling rationale: its **own 60-second polling loop**, its own fetch, and it exposes the **raw
notification list** rather than a pre-computed count. The count is derived at the call site via
`unreadNotificationCount(...)` (unread = `acknowledged_at == null`; empty/unloaded yields 0, so the
badge simply doesn't render until there's something to show). Because the badge polls independently
of the log screen's ViewModel, a screen-side acknowledge is reflected in the badge on its next tick
rather than instantly — an intentional consequence of the decoupling.

### Design tokens

All new badge/log-row **dimensions** come from the design-tokens **notification component group**
(design-tokens 0.4.0, bumped from 0.3.0 in `app/build.gradle.kts`) via a `NotificationTokens`
accessor in `ui/theme/Tokens.kt` (mirroring `PillBadgeTokens`) — no hardcoded color/dimension
literals in the new UI. Colors stay **theme-driven** (`LocalThemeOption`'s `primary` for the unread
accent, with a `MaterialTheme` fallback), the same split the Activity Log's badges use. New states
(unread/read mix, empty, bell badge) are locked in with Roborazzi goldens
(`ComponentSnapshotTest`, see `snapshot-testing.md`).

## Hilt worker wiring

The worker is a **plain `CoroutineWorker`**, not `@HiltWorker`. It resolves its Hilt dependencies
at runtime through an `@EntryPoint` (`NotificationPollWorker.Deps`) via
`EntryPointAccessors.fromApplication(...)`, exposing exactly `NotificationRepository`,
`ConnectionStatusStore`, and `PostedNotificationsStore`.

This avoids the `androidx.hilt:hilt-work` / `androidx.hilt:hilt-compiler` annotation processor:
its 1.2.0 release cannot read AGP 9's Kotlin 2.x metadata and breaks the kapt build. The
`@EntryPoint` approach reuses the existing Dagger-Hilt compiler only, needs no extra processor,
and lets WorkManager's **default** factory/initializer instantiate the worker from its
`(Context, WorkerParameters)` constructor — so `ChoresApplication` stays a plain `Application`
(no `Configuration.Provider`) and the manifest keeps WorkManager's default initializer.

A test seam (`NotificationPollWorker.depsProvider`, reset via `resetDepsProvider()`) lets unit
tests inject fakes without standing up a Hilt test component.

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
- `GET /v1/notifications/preferences` → `{type: bool}` (issue #44) — a bare JSON object, not a
  wrapper, so the client models it as a `Map<String, Boolean>` (`NotificationPreferencesDto`).
- `PUT /v1/notifications/preferences` with the same map shape.

Both ride the existing interceptor stack (`BaseUrlInterceptor` rewrites to the user-entered
backend, `AuthInterceptor` attaches the bearer token) via relative `v1/…` paths — no interceptor
changes. See `data/network/dto/NotificationDtos.kt` and `data/model/Notification.kt`.
