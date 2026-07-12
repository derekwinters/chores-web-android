# CLAUDE.md — Developer and AI Agent Reference

This repo is the Android client for chores-web (Kotlin, Jetpack Compose).
The backend lives in `derekwinters/chores-web-backend`; user-facing docs and
the API contract live in `derekwinters/chores-web-docs`. Domain terms are
defined canonically in chores-web's `CONTEXT.md` — see this repo's own
`CONTEXT.md` for the pointer and any Android-only terms.

## Releases

Versioning is automated with release-please (config under
`.github/release-please/`, workflow `.github/workflows/release-please.yml`).
On push to `main`, release-please parses commit history to decide the next
version, writes it into `gradle.properties` (the
`x-release-please-version` marker line), and opens/updates a release PR.
Merging that PR creates a GitHub Release and tag, and the same workflow run
builds a debug-signed `assembleRelease` APK and attaches it to the release.

**The squash-merged PR title IS the conventional commit that release-please
parses.** There is no separate commit message step — whatever title is on
the squash-merge button is the exact string release-please reads to decide
the version bump and to write the changelog entry. Merging a PR whose title
is not a valid Conventional Commit produces a commit release-please cannot
categorize: it will not trigger the version/changelog it should, and it
silently sits unreleased. This is not hypothetical — a prior orchestrating
session squash-merged PRs in sibling repos using non-conventional titles
verbatim, and those changes shipped with no version bump and no changelog
entry. Treat the squash-merge title as a release-please input, not free text.

## Conventional Commits Are Required

Every commit that lands on `main` — and especially every squash-merge PR
title — must be a valid Conventional Commit:

```
type(scope): description
```

`type` must be one of: `feat`, `fix`, `chore`, `ci`, `docs`, `build`,
`refactor`, `test`, `perf`, `revert`.

Pick `type` based on the actual semver/changelog impact of the change, not
by copying whatever title the PR already has:

- A new user-facing capability is `feat`, even if the same PR also fixes a
  bug along the way.
- Don't default to `chore` or omit the type because the "real" type is
  unclear — figure out the impact first.
- Never take a non-conventional PR title (e.g. "Fix login bug", "Update
  navigation") and use it as-is for the squash-merge commit message. Rewrite
  it to a conventional form before merging.

## Delegate Commit-Authoring Work

An orchestrating/main Claude Code session must not author commits, write
commit messages, or open PRs directly. All of that — implementing the
change, writing the commit message, opening the PR — is delegated to an
implementation agent (subagent/task run). The orchestrating session's job is
to delegate the work, review CI results, and merge — and when it merges, it
still owns choosing a Conventional-Commits-formatted squash-merge title per
the rule above, even though it didn't write the underlying commits.

## Repo Orientation

- Single Gradle module (`:app`), Kotlin + Jetpack Compose.
- CI/CD is five workflows under `.github/workflows/` — `pr.yml` (test +
  Roborazzi snapshot verification + debug build), `record-snapshots.yml`
  (manual snapshot golden recording), `release-please.yml` (versioning +
  release build/attach, described above), `release-candidate.yml` (build
  APK for the open release PR), `release.yml` (manual backfill/recovery
  build for an existing tag). See `README.md` for the full trigger table.
- All CI-built APKs are debug-signed (see
  `docs/adr/0001-debug-signing-until-play-store-launch.md`); real release
  signing is deferred until a Play Store launch is planned. There is no
  Maven/GitHub Packages publish target for this app itself — it *consumes*
  a design-tokens Maven artifact (`com.derekwinters.chores:design-tokens`)
  from GitHub Packages, configured in `settings.gradle.kts`.
- Snapshot testing (Roborazzi) is documented in `docs/snapshot-testing.md`.
