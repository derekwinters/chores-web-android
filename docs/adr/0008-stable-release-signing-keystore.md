# ADR-0008: Release APKs are signed with one stable keystore

Status: accepted · Issues: #52 · Supersedes: [ADR-0001](0001-debug-signing-until-play-store-launch.md)

## Context

Household devices install this app by sideloading the APK attached to each GitHub Release; there
is no Play Store listing. Android only installs an update when the new APK is signed by the
**same certificate** as the installed one. Anything else is refused with
`INSTALL_FAILED_UPDATE_INCOMPATIBLE`, and the only way through is to uninstall first, which
deletes the app's local data (saved backend URL, credentials, notification settings, version-check
cache).

[ADR-0001](0001-debug-signing-until-play-store-launch.md) signed every CI build with the Android
debug key. On paper that meant "one debug key"; in practice no workflow pinned, restored or cached
a keystore, so every ephemeral GitHub Actions runner generated a **fresh random**
`~/.android/debug.keystore` and signed the release with it. Every release was signed by a
different key, and every update needed an uninstall. That blocks every Play-Store-free update
path: installing over the top, updater apps such as Obtainium, and the in-app updater planned in
#54.

## Decision

**Release builds are signed with one real, stable keystore, created once and kept.** The process
mirrors `derekwinters/Interval-trainer-android` (same stack) and `derekwinters/lucas-doggiehood`.

- **The keystore lives only in repository Actions secrets.** Nothing is committed.

  | Secret | Holds |
  | --- | --- |
  | `ANDROID_KEYSTORE_BASE64` | the keystore file, base64-encoded |
  | `ANDROID_KEYSTORE_PASSWORD` | the keystore password |
  | `ANDROID_KEY_ALIAS` | the alias of the signing key inside it |
  | `ANDROID_KEY_ALIAS_PASSWORD` | that key's password |
  | `ANDROID_KEYSTORE_SHA256` | the expected SHA-256 fingerprint of the signing **certificate** |

- **Gradle reads the signing inputs from the environment only**: `ANDROID_KEYSTORE_PATH` (the
  decoded keystore file), `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`,
  `ANDROID_KEY_ALIAS_PASSWORD`. When any of them is missing, the `release` build type gets **no
  signing config at all** and the APK comes out unsigned. It never falls back to the debug key:
  an unsigned APK fails visibly, while a debug-signed one looks fine until the *next* update fails
  on a device. A local `./gradlew assembleRelease` without those variables therefore produces an
  unsigned APK, which is intended. Debug builds are unchanged.
- **The v1, v2 and v3 signature schemes are all enabled explicitly.** A sideloaded APK passes
  through installers AGP knows nothing about, and a scheme set nobody states is one a toolchain
  upgrade can change without anyone noticing.
- **Every release-producing workflow** (`release-please.yml`'s `build-and-attach`, the
  `release.yml` backfill, and `release-candidate.yml`) fails before building if any of the five
  secrets is empty. It decodes the keystore into `$RUNNER_TEMP` (never logged, removed in an
  `if: always()` step), builds the signed APK, and runs the **release-signature gate** before
  anything is uploaded or attached.
- **The gate** is `.github/scripts/verify_release_signature.py`. It runs
  `apksigner verify --print-certs` (no `--verbose`) over each APK, requires exactly one signing
  certificate and requires its SHA-256 to equal `ANDROID_KEYSTORE_SHA256` (compared case- and
  colon-insensitively, so the `keytool` and `apksigner` forms are both accepted), and names a
  debug-key fallback (`CN=Android Debug`) explicitly. It fails closed on output it cannot read: any
  unrecognized line, or a signer block without its DN or digest, fails the release. Its unit
  tests use apksigner output captured with exactly those flags. The workflows run the copy of the gate checked out from `main`,
  so backfilling an old tag still runs the current gate. The gate prints each APK's actual digest;
  that value is public (it ships inside every APK) and could later be committed as a pin.
- **The release key is never reachable from a pull request.** No `pull_request`-triggered
  workflow references the keystore secrets; `pr.yml` keeps building a debug-signed throwaway APK
  and runs the gate's unit tests plus a wiring test that enforces this rule.
  `release-candidate.yml` moves from `pull_request` to `workflow_run` (after `PR Checks` succeeds
  on a same-repository `release-please--*` branch) plus `workflow_dispatch`. A `workflow_run` job
  runs in this repository's own trust context, which is GitHub's documented safe pattern for
  needing secrets in a job timed by pull-request activity.

## Alternatives considered

- **Keep debug signing, but cache or commit a debug keystore.** That would make the key stable, but
  a debug key is a well-known, unprotected credential, and a committed one is a signing key anyone
  can use. Rejected.
- **Wait for a Play Store launch and use Play App Signing.** That leaves every release until then
  forcing an uninstall, and blocks the auto-update chain (#53, #54, #55). Rejected; Play App
  Signing can still adopt this key as its upload key later.

## Consequences

- **One-time migration break.** Installs signed with the old per-run random debug keys cannot
  update to the first stable-key release. Users uninstall and reinstall **once**, losing local
  app data that one time. This is called out in the README and must be called out in the first
  stable-key release's notes. Every later release installs as an in-place update.
- **The keystore must not be lost.** A sideloaded app has no key recovery. If the keystore or its
  passwords are lost, the next release is signed by a new key and every user has to uninstall
  once again. The owner keeps an offline backup of the keystore outside GitHub.
- **A mis-wired secret fails the release, not the user.** Missing secrets fail the job before the
  build. A wrong key, a debug fallback, or an `ANDROID_KEYSTORE_SHA256` that is not actually the
  certificate fingerprint fails the gate before any APK is attached.
- **Release candidates can only be dispatched from `main`.** GitHub reads `workflow_dispatch` and
  `workflow_run` triggers from the default branch, so the first dispatched candidate is possible
  only after this change merges.
- **Out of scope:** `versionCode` derivation (#53), the in-app updater (#54), install docs (#55),
  APK naming (#18), and Play Store publishing.
