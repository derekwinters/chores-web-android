# chores-web-android-client

[![PR Checks](https://github.com/derekwinters/chores-web-android-client/actions/workflows/pr.yml/badge.svg)](https://github.com/derekwinters/chores-web-android-client/actions/workflows/pr.yml)

Android client for the chores-web application

## CI/CD

This repo uses five GitHub Actions workflows to build, test, and release the app. APK filenames carry the app version: `chores-<version>-<buildType>.apk` (e.g. `chores-1.0.0-release.apk`), with `<version>` sourced from `gradle.properties`.

| Trigger | Workflow | What it does |
|---------|----------|---------------|
| Pull request | `.github/workflows/pr.yml` | Runs the release-signature gate's Python unit tests and wiring test (`python3 -m unittest discover -s .github/scripts/tests`), runs `./gradlew test`, verifies Roborazzi snapshot goldens (`verifyRoborazziDebug` — see [snapshot testing](docs/snapshot-testing.md)), builds `assembleDebug`, uploads the debug-signed APK (`chores-<version>-debug.apk`) as a workflow artifact named `app-debug-<short-sha>` (short SHA of the PR head commit) |
| Manual (`workflow_dispatch` with a branch `ref` input) | `.github/workflows/record-snapshots.yml` | Records Roborazzi snapshot goldens on CI (`recordRoborazziDebug`) and commits them back to the branch — see [snapshot testing](docs/snapshot-testing.md) |
| Push to `main` | `.github/workflows/release-please.yml` | Runs [Release Please](https://github.com/googleapis/release-please-action) (release-type `simple`) to open/update the release PR and bump the version in `gradle.properties`; when merging the release PR creates a release, builds a release-signed `assembleRelease` in the same run, runs the release-signature gate, and attaches the versioned APK (`chores-<version>-release.apk`) to the GitHub Release |
| `PR Checks` succeeded on this repository's Release Please branch (`workflow_run`), or manual (`workflow_dispatch` with an optional `ref`) | `.github/workflows/release-candidate.yml` | Runs `./gradlew test`, builds a release-signed `assembleRelease`, runs the release-signature gate, uploads the APK (named with the upcoming version) as the `app-release-candidate` workflow artifact |
| Manual (`workflow_dispatch` with a required `tag` input) | `.github/workflows/release.yml` | Backfill/recovery path: builds a release-signed `assembleRelease` at the given tag, runs the release-signature gate, and attaches the APK to that tag's GitHub Release — see the workflow's comments for why tag-push triggers were abandoned |

### Release signing

Release APKs are signed with **one stable release keystore**, so each release installs over the previous one as an in-place update (no uninstall, no data loss). See [ADR-0008](docs/adr/0008-stable-release-signing-keystore.md), which supersedes [ADR-0001](docs/adr/0001-debug-signing-until-play-store-launch.md).

- The keystore lives only in the repository Actions secrets `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS` and `ANDROID_KEY_ALIAS_PASSWORD`; `ANDROID_KEYSTORE_SHA256` holds the expected signing-certificate SHA-256 fingerprint. Nothing is committed.
- `app/build.gradle.kts` signs `release` from the `ANDROID_KEYSTORE_PATH`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS` and `ANDROID_KEY_ALIAS_PASSWORD` environment variables (v1/v2/v3 schemes). Without them the release APK is **unsigned**, never debug-signed, so a local `./gradlew assembleRelease` produces an unsigned APK.
- Every release-producing workflow fails if a secret is missing, and runs `.github/scripts/verify_release_signature.py` (`apksigner verify --print-certs`) before anything is uploaded or attached. The gate (run from a checkout of `main`, so backfilling an old tag still uses the current gate) fails if the APK is not signed by exactly the pinned certificate, and says so explicitly when the build fell back to the debug key.
- No `pull_request`-triggered workflow can reach the keystore secrets. PR APKs stay debug-signed throwaway builds.

> **One-time reinstall.** Releases before the stable keystore were each signed with a random per-run debug key. An install of one of those cannot be updated to the first stable-key release: uninstall it and install the new APK once (this clears the app's saved server URL, sign-in and settings). Every release after that updates in place.
