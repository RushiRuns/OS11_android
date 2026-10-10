package com.os11.app.ui.features.project

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.os11.app.ui.components.OSButton
import com.os11.app.ui.components.OSTextField
import com.os11.app.ui.theme.OS11Theme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSectionDialog(onDismiss: () -> Unit, onConfirm: (name: String) -> Unit) {
    var sectionName by remember { mutableStateOf("") }
    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing
    val type = OS11Theme.type

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(OS11Theme.radius.lg))
                .background(colors.surfaceRaised)
                .padding(spacing.s4),
        ) {
            Text(
                text = "New Section",
                style = type.panelTitle.copy(color = colors.textPrimary),
            )
            Spacer(modifier = Modifier.height(spacing.s3))
            OSTextField(
                value = sectionName,
                onValueChange = { sectionName = it },
                placeholder = "Section name...",
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(spacing.s4))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.s2),
            ) {
                OSButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Cancel")
                }
                OSButton(
                    onClick = {
                        if (sectionName.isNotBlank()) {
                            onConfirm(sectionName.trim())
                            onDismiss()
                        }
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Create")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RenameSectionDialog(initialName: String, onDismiss: () -> Unit, onConfirm: (newName: String) -> Unit) {
    var sectionName by remember { mutableStateOf(initialName) }
    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing
    val type = OS11Theme.type

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(OS11Theme.radius.lg))
                .background(colors.surfaceRaised)
                .padding(spacing.s4),
        ) {
            Text(
                text = "Rename Section",
                style = type.panelTitle.copy(color = colors.textPrimary),
            )
            Spacer(modifier = Modifier.height(spacing.s3))
            OSTextField(
                value = sectionName,
                onValueChange = { sectionName = it },
                placeholder = "Section name...",
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(spacing.s4))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.s2),
            ) {
                OSButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Cancel")
                }
                OSButton(
                    onClick = {
                        if (sectionName.isNotBlank()) {
                            onConfirm(sectionName.trim())
                            onDismiss()
                        }
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Save")
                }
            }
        }
    }
}
