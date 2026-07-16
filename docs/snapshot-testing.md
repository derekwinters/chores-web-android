# Snapshot Testing (Roborazzi)

Issue #15, iteration 5 of the design-token rollout (derekwinters/chores-web-docs#11): a
snapshot catalog of the mapping-matrix components rendered under a dark and a light ("paper")
theme, so pixel drift in the tokenized UI fails PR CI instead of shipping silently.

## Why Roborazzi, not Paparazzi

The rollout issue named Paparazzi with Roborazzi pre-authorized as fallback. Paparazzi does
**not** support `com.android.application` modules
([cashapp/paparazzi#107](https://github.com/cashapp/paparazzi/issues/107)), and this repo is a
single `:app` application module where all composables live. [Roborazzi](https://github.com/takahirom/roborazzi)
runs on the Robolectric + Compose UI-test stack this repo's content tests already use
(Robolectric NATIVE graphics mode), so it needs no module restructuring.

## What is covered

`app/src/test/java/com/derekwinters/chores/ui/snapshots/ComponentSnapshotTest.kt` renders each
component under two `ThemeOption` fixtures built from the backend's built-in dark and paper
palettes. Goldens are PNGs in `app/src/test/snapshots/`, named
`<component>_<variant>_<dark|paper>.png`:

| Component | Snapshot pair |
|---|---|
| PillBadge (Activity Log action/target badge variants) | `pillbadge_variants_*` |
| ChoreRow via `ChoreListContent` (one expanded, one collapsed, accent bars, filter icon row) | `chorerow_list_*` |
| Buttons (filled `Button` + `TextButton`) | `buttons_primarysecondary_*` |
| `OutlinedTextField` (filled + empty-with-label) | `textfield_outlined_*` |
| Theme preference tiles (`ThemePreferenceContent`, selected + unselected) | `themepreference_tiles_*` |
| `AlertDialog` (chore delete confirmation shape) | `alertdialog_deleteconfirm_*` |
| Settings About screen (`SettingsAboutContent`; own-app version, backend version, repo links — issue #35) | `settingsabout_sections_*` |
| Notification Log (`NotificationLogContent`; unread accent bar/fill + read history mix — issue #45) | `notificationlog_mix_*` |
| Notification Log empty state (issue #45) | `notificationlog_empty_*` |
| Notification bell unread badge (`BadgedBox` + `Badge` over the bell icon — issue #45) | `notificationbadge_bell_*` |
| Home (`HomeContent`; the signed-in user's own single Board card — issue #16) | `home_usercard_*` |

`PillBadge`, `ChoreRow`, and `ThemeOptionCard` are private composables; the tests snapshot the
closest public wrapper (`ChoreListContent`, `ThemePreferenceContent`) or an equivalent inline
reconstruction built from the same tokens (`PillBadge`) rather than widening production
visibility for tests.

## Recording goldens (CI-only)

Goldens must be recorded on a CI runner — not a developer machine — so the pixels come from the
same OS/font environment the verify step compares against.

1. Push your branch.
2. GitHub -> Actions -> **Record Snapshots** -> *Run workflow* -> enter your branch name as `ref`.
3. The workflow runs `./gradlew recordRoborazziDebug` and commits any changed/new goldens back
   to your branch as `test: record snapshot goldens [skip ci]`.
4. `git pull`, eyeball the new/changed PNGs, and include them in your PR for review.

Intentional visual changes follow the same flow: re-run the record workflow on the branch and
review the golden diffs in the PR.

## How verify gates PRs

`.github/workflows/pr.yml` runs `./gradlew verifyRoborazziDebug` (step *"Verify snapshots
(design-token guardrail)"*) after unit tests. Any pixel difference against the committed goldens
fails the job; diff images (`*_compare.png`) are uploaded as the `snapshot-diffs` workflow
artifact for inspection.

Note: the plain `./gradlew test` run also executes `ComponentSnapshotTest`, but without the
plugin's record/verify flag `captureRoboImage` is a no-op, so the tests only assert that the
components compose.

## Local runs

Local recording works (`./gradlew recordRoborazziDebug`, needs `gpr.user`/`gpr.key` in
`~/.gradle/gradle.properties` for the design-tokens Maven repo — see `settings.gradle.kts`) but
locally recorded goldens generally will not match CI's font rendering; don't commit them.
Use `./gradlew compareRoborazziDebug` locally to generate diff images without failing.
