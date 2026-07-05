package com.derekwinters.chores.ui.common

/**
 * Issue #91 (Auth Log) / #73 (Activity Log): map raw action values (as returned by the backend,
 * e.g. `login_succeeded`, `completed`, `password_changed`) to humanized display labels (e.g.
 * "Login Succeeded", "Completed", "Password Changed"), mirroring chores-web's humanization.
 *
 * Shared by both logs rather than duplicated per-screen — every raw action value chores-web/the
 * API uses is a snake_case (or already-lowercase single-word) identifier, so a single generic
 * title-case-per-word transform covers both logs' known values and any future/unmapped ones.
 */
fun humanizeActionLabel(action: String): String =
    action.split("_").joinToString(" ") { word -> word.replaceFirstChar { it.uppercase() } }
