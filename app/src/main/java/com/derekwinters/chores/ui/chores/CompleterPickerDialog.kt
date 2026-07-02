package com.derekwinters.chores.ui.chores

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.derekwinters.chores.R

/**
 * Issue #5 behavior: "Complete-chore action ... with Completer-picker dialog when
 * current_assignee == null", matching `CompleteWithActorModal.jsx` and the "Completer" term
 * from chores-web's CONTEXT.md. [people] is the chore's `eligible_people` list (usernames).
 */
@Composable
fun CompleterPickerDialog(
    people: List<String>,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var selected by remember { mutableStateOf(people.firstOrNull()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.completer_picker_title)) },
        text = {
            Column {
                people.forEach { person ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = person == selected,
                                onClick = { selected = person }
                            )
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(selected = person == selected, onClick = { selected = person })
                        Text(text = person, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { selected?.let(onConfirm) },
                enabled = selected != null
            ) {
                Text(stringResource(R.string.completer_picker_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
