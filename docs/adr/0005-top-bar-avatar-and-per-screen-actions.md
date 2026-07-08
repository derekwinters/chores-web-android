# Top bar avatar-left placement and per-screen actions

## Context

Issue #177 investigated a FAB/list-content overlap bug on the Chores screen (a short list's last action row can end up directly beneath the fixed `ExtendedFloatingActionButton` with no scroll affordance to reveal it) and, separately, validated a collapsible-search design for `ChoreFilterIconRow`. While debugging PR #179's overlap fix, the FAB itself turned out to be the better thing to remove rather than work around: `ChoreListScreen` doesn't need a floating Add-Chore control if Add Chore instead lives in the top app bar, alongside the existing user avatar/identity menu.

That relocation raises two questions this ADR settles:

1. Where does the avatar go once Add Chore needs the `actions` (right) slot? Material convention places a navigation/back icon on the left and actions on the right; this app has no back stack at the top-bar level (drill-in screens use their own screen-level back handling within `NavHost`, not the shared `TopAppBar`), so the left slot (`navigationIcon`) is otherwise empty on every top-level destination.
2. The top bar (`ChoresAuthenticatedScaffold`'s `TopAppBar`) is shared across every screen (Dashboard, Chores, Users, Settings) via a single `Scaffold`. Add Chore only makes sense on the Chores screen. This is the first time the shared top bar needs to show a screen-specific action rather than the same global content everywhere.

## Decision

**Avatar moves to `navigationIcon`.** The colored initial circle (no inline username text — `navigationIcon` is sized for a single compact icon, not icon+text) becomes the top bar's left-side element and remains the tap target that opens the identity/logout dropdown (`userMenuTrigger` testTag unchanged). This is an intentional, acknowledged deviation from the nav-icon-means-back convention: this app's flat five-tab bottom-nav structure has no top-bar-level back stack for that slot to conflict with, so the deviation reads as a simple icon placement rather than a misleading back affordance. The username, previously shown inline next to the avatar, moves into the dropdown itself as a new non-clickable "Signed in as {username}" header above Logout, so the identity information isn't lost, just relocated one tap deeper.

**`actions` becomes conditional and screen-aware.** The top bar's `actions` slot now renders per-current-destination content instead of a fixed set of icons: a plus-icon "Add Chore" button appears only when the Chores list screen is the current destination, wired directly to `navController.navigate("chores/new")` (no new callback threading through the `NavHost` content lambdas, since `navController` is already in scope inside `ChoresAuthenticatedScaffold`). Visibility is gated on an **exact match** against the chores-list route pattern (`"chores?assignee={assignee}&dueWithin={dueWithin}"`), not the existing `isCurrent()` prefix-match helper used for bottom-nav highlighting — `isCurrent()` intentionally matches `"chores/new"` and `"chores/{choreId}/edit"` too (so the Chores tab stays highlighted while drilled into those screens), which would incorrectly show the Add-Chore button on the create/edit screens themselves if reused here.

This establishes a precedent: the top bar's `actions` slot is now a per-screen extension point, not a single global set of icons. Future screen-specific top-bar actions should follow the same exact-route-match pattern rather than growing a new prefix-based or ad hoc check per screen.

**FAB removed entirely.** `ChoreListScreen`/`ChoreListContent` drop the `ExtendedFloatingActionButton` and `onAddChore` param outright now that Add Chore lives in the top bar. This also removes the FAB/list-content overlap problem investigated under #177 by construction — no FAB, no overlap, no `contentPadding`/bottom-padding workaround needed for it.

**Search collapses into the filter icon row** (carried forward unchanged from #177's already-validated direction): the always-visible search `OutlinedTextField` above `ChoreFilterIconRow` is removed; a search icon becomes the first entry in `ChoreFilterIconRow` itself. Tapping it morphs the row into a full-width text field + back/collapse icon, hiding the other filter icons (Assignee/State/Due-within/Tune) while expanded. Collapsing preserves the query — the filter stays active, surfaced via the existing "Showing N of M chores" count and `Clear filters` affordance — and the field starts expanded if a query is already active on first composition (e.g. returning to the screen with a filter carried over).

## Consequences

- **Pro**: Removes the FAB/overlap bug class entirely rather than patching around it (no more fixed-footprint `contentPadding` bookkeeping in `ChoreListContent`).
- **Pro**: Establishes a reusable, exact-route-matched pattern for screen-specific top-bar actions, rather than every future screen needing its own bespoke mechanism.
- **Pro**: Frees horizontal space in the Chores screen's filter row for the common case (filtering, not searching), since search only claims the full row while actively in use.
- **Con**: Avatar-on-the-left is a deliberate, documented deviation from Material's back-icon convention for `navigationIcon` — acceptable here only because this app's top bar never hosts a real back action, and would need re-evaluation if that ever changes.
- **Con**: The username is no longer visible at a glance in the top bar itself (moved into the dropdown as a "Signed in as" header); this trades a small amount of always-on visibility for the space Add Chore (and future per-screen actions) needs.
- **Follow-up (explicitly out of scope here)**: moving the chore list's Filters button into the top bar too, to free further list space — noted as a natural extension of the per-screen-action pattern this issue establishes, not bundled into it.
