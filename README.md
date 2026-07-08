# chores-web-android-client

[![PR Checks](https://github.com/derekwinters/chores-web-android-client/actions/workflows/pr.yml/badge.svg)](https://github.com/derekwinters/chores-web-android-client/actions/workflows/pr.yml)

Android client for the chores-web application

## CI/CD

This repo uses four GitHub Actions workflows to build and release the app. All CI-built APKs are signed with the Android debug key — see [ADR 0001](docs/adr/0001-debug-signing-until-play-store-launch.md) for why real release signing is deferred until Play Store launch is planned.

| Trigger | Workflow | What it does |
|---------|----------|---------------|
| Pull request | `.github/workflows/pr.yml` | Runs `./gradlew test`, builds `assembleDebug`, uploads the APK as a workflow artifact named `app-debug-<short-sha>` (short SHA of the PR head commit) |
| Push to `main` | `.github/workflows/release-please.yml` | Runs [Release Please](https://github.com/googleapis/release-please-action) (release-type `simple`) to open/update the release PR and bump the version in `gradle.properties` |
| Release Please PR | `.github/workflows/release-candidate.yml` | Runs `./gradlew test`, builds `assembleRelease` (debug-signed), uploads the APK as a workflow artifact |
| Tag push (`v*`, created when a Release Please PR is merged) | `.github/workflows/release.yml` | Builds `assembleRelease` (debug-signed) and attaches the APK to the GitHub Release |
