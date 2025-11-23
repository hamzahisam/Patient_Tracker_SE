package com.example.patienttracker.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

data class LanguageOption(
    val code: String,
    val name: String,
    val nativeName: String
)

@Composable
fun LanguageDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    currentLanguage: String
) {
    val languages = listOf(
        LanguageOption("en", "English", "English"),
        LanguageOption("ur", "Urdu", "اردو")
    )

    var selectedLanguage by remember { mutableStateOf(currentLanguage) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Select Language")
        },
        text = {
            Column {
                languages.forEach { language ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = (language.code == selectedLanguage),
                                onClick = { selectedLanguage = language.code },
                                role = Role.RadioButton
                            )
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (language.code == selectedLanguage),
                            onClick = { selectedLanguage = language.code }
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = language.name,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = language.nativeName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(selectedLanguage)
                    onDismiss()
                }
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}