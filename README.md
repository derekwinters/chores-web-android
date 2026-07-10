# chores-web-android-client

[![PR Checks](https://github.com/derekwinters/chores-web-android-client/actions/workflows/pr.yml/badge.svg)](https://github.com/derekwinters/chores-web-android-client/actions/workflows/pr.yml)

Android client for the chores-web application

## CI/CD

This repo uses five GitHub Actions workflows to build, test, and release the app. All CI-built APKs are signed with the Android debug key — see [ADR 0001](docs/adr/0001-debug-signing-until-play-store-launch.md) for why real release signing is deferred until Play Store launch is planned. APK filenames carry the app version: `chores-<version>-<buildType>.apk` (e.g. `chores-1.0.0-release.apk`), with `<version>` sourced from `gradle.properties`.

| Trigger | Workflow | What it does |
|---------|----------|---------------|
| Pull request | `.github/workflows/pr.yml` | Runs `./gradlew test`, verifies Roborazzi snapshot goldens (`verifyRoborazziDebug` — see [snapshot testing](docs/snapshot-testing.md)), builds `assembleDebug`, uploads the debug APK (`chores-<version>-debug.apk`) as a workflow artifact named `app-debug-<short-sha>` (short SHA of the PR head commit) |
| Manual (`workflow_dispatch` with a branch `ref` input) | `.github/workflows/record-snapshots.yml` | Records Roborazzi snapshot goldens on CI (`recordRoborazziDebug`) and commits them back to the branch — see [snapshot testing](docs/snapshot-testing.md) |
| Push to `main` | `.github/workflows/release-please.yml` | Runs [Release Please](https://github.com/googleapis/release-please-action) (release-type `simple`) to open/update the release PR and bump the version in `gradle.properties`; when merging the release PR creates a release, builds `assembleRelease` (debug-signed) in the same run and attaches the versioned APK (`chores-<version>-release.apk`) to the GitHub Release |
| Release Please PR | `.github/workflows/release-candidate.yml` | Runs `./gradlew test`, builds `assembleRelease` (debug-signed), uploads the APK (named with the upcoming version) as a workflow artifact |
| Manual (`workflow_dispatch` with a required `tag` input) | `.github/workflows/release.yml` | Backfill/recovery path: builds `assembleRelease` (debug-signed) at the given tag and attaches the APK to that tag's GitHub Release — see the workflow's comments for why tag-push triggers were abandoned |
