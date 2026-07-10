# ADR-0006: ThemeOption slots are a named subset of the design-token color roles

Status: accepted · Rollout: [derekwinters/chores-web-docs#11](https://github.com/derekwinters/chores-web-docs/issues/11) · Issues: #13, #14

## Context

This app resolves a runtime `ThemeOption` (9 hex slots) from the backend `/theme` API and
maps it onto a Material 3 `ColorScheme` in `ui/theme/ChoresTheme.kt`. The static design
values now live in `chores-web-design-tokens`, consumed as the Maven artifact
`com.derekwinters.chores:design-tokens` (pinned exact version in `app/build.gradle.kts`).
Two vocabularies for the same colors risked drifting: the runtime slots and the token roles.

## Decision

The token repo's **semantic color-role names are canonical**. `ThemeOption`'s slots are not
a parallel definition — they are the runtime-overridable subset of those roles:

| Token role | API field | `ThemeOption` property | `ColorScheme` slot |
|---|---|---|---|
| `color.background` | `bg` | `background` | `background` |
| `color.surface` | `surface` | `surface` | `surface` |
| `color.surface2` | `surface2` | `surface2` | `surfaceVariant` (M3 has no third neutral tier) |
| `color.primary` | `primary` | `primary` | `primary` |
| `color.secondary` | `secondary` | `secondary` | `secondary` |
| `color.accent` | `accent` | `accent` | `tertiary` |
| `color.success` | `success` | `success` | — (`LocalThemeOption`, no M3 slot) |
| `color.warning` | `warning` | `warning` | — (`LocalThemeOption`, no M3 slot) |
| `color.error` | `error` | `error` | `error` |

Derived roles (`text`, `text-muted`, `on-primary`, `overlay`, `points`) and every
non-color token (spacing, shape, elevation, motion, typography, layout) are **static**
(Tier 1) — never runtime-overridden. The full cross-platform contract, including the web
CSS custom-property names, lives in the token repo's
[`docs/mapping-matrix.md`](https://github.com/derekwinters/chores-web-design-tokens/blob/main/docs/mapping-matrix.md)
— link there, don't duplicate it.

No API or Kotlin renames were needed: `ThemeOption`'s property names already match the
canonical role names (the only spelling difference is the wire field `bg` ↔ role
`background`, handled at the API model).

## Consequences

- The pre-theme default `ColorScheme` is built from the artifact's dark set
  (`tokenDefaultColorScheme()`), matching the web client's `:root` prepaint — the two
  clients show the same baseline until `/theme` resolves.
- Light/dark selection stays `background.luminance() > 0.5` for **runtime** themes (the
  shared cross-platform rule); the token repo models the default light/dark sets
  explicitly (`color.light.*` = backend "paper", `color.dark.*` = backend "dark").
- A palette or design-value change is a token release + a pinned-version bump here —
  values must not be re-hardcoded in this repo (the Iteration 3 sweep, #23, replaces the
  remaining inline literals; android-side tracking: #22).
