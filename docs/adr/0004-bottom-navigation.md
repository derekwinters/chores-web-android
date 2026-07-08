# Return to bottom navigation for primary destinations

## Context

Primary navigation has changed shape twice already: it started as a bottom bar, moved to a sidebar/drawer (issue #10) once the destination count grew past what a bottom bar comfortably holds, then moved again to an expand-under-the-top-bar panel (issue #145) with shared-axis transitions (issue #146). None of these transitions were recorded in an ADR — the reasoning lives only in scattered code comments, which makes it hard to tell whether returning to a bottom bar (issue #167) is a considered decision or repeating a discarded approach.

It isn't repeating a discarded approach: the constraint that motivated moving *away* from a bottom bar in issue #10 (too many top-level destinations) no longer applies. Settings is graduating from a dropdown-only admin destination into a primary tab that also absorbs Preferences, and Users/Log/Chores/Home round out a fixed set of exactly five. Five is within the comfortable range for Material3's `NavigationBar`, so the original constraint is gone.

## Decision

Primary navigation moves to a Material3 `NavigationBar` (bottom bar) with five fixed items: Home (renamed label for the existing Dashboard/Board destination — route unchanged), Chores, Users (admin-only, hidden for non-admins), Log, Settings.

Settings' top-level `adminOnly` flag is removed — it becomes visible to all users, since the existing per-user Preferences screen (`ThemePreferenceScreen`, previously reachable only via the avatar dropdown) is folded into it as a section. Admin-only content within Settings (General/Auth/Chores config, Theme *admin* config, Data) stays gated at the section level inside `SettingsMenuContent`, not at the tab level. `Users` remains a separately-gated tab, hidden entirely for non-admins — so non-admins see 3 of 5 items, admins see all 5; this size difference is accepted rather than padded out with placeholder tabs.

The Chores tab carries a numeric Material3 `Badge` showing the signed-in user's own "due now" chore count (reusing the Dashboard's existing per-person `dueNowCount` logic, not a household-wide count), sourced from a small Activity-scoped ViewModel with its own polling loop, decoupled from `DashboardViewModel`'s lifecycle.

The hamburger toggle and expand-under-top-bar panel (issue #145's mechanism) are removed entirely rather than left dormant. The avatar/top-bar dropdown shrinks to identity + logout only, since Preferences and Settings no longer live there.

## Consequences

- **Pro**: Removes a layer of indirection (panel expand/collapse) in favor of always-visible primary destinations, standard for mobile apps with a small fixed destination count.
- **Pro**: Consolidates all user-facing "theme/config" entry points under one Settings tab instead of splitting them between a dropdown item (Preferences) and a separate admin destination (Settings).
- **Con**: Non-admin and admin users now see different numbers of bottom-nav items (3 vs. 5), which was avoided by the previous panel/dropdown design since a variable-length list is less visually jarring than a variable-width fixed bar.
- **Con**: First introduction of a live-polling badge outside Dashboard; the new Activity-scoped ViewModel is a case of divergent state ownership that future dashboard-badge features should either reuse or explicitly justify not reusing.
