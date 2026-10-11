package com.os11.app.ui.features.settings

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.os11.app.data.sync.TrackedWriter
import com.os11.app.ui.components.OSButton
import com.os11.app.ui.components.OSButtonVariant
import com.os11.app.ui.components.OSIconButton
import com.os11.app.ui.components.OSSwitch
import com.os11.app.ui.theme.OS11Theme

@Composable
@Suppress("LongMethod", "CyclomaticComplexMethod")
fun SettingsDataScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing
    val type = OS11Theme.type
    val radius = OS11Theme.radius

    // SAF Document Launchers
    val exportJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.exportJson { json ->
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(json.toByteArray(Charsets.UTF_8))
                    out.flush()
                }
            }
        }
    }

    val exportCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.exportCsv { csv ->
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(csv.toByteArray(Charsets.UTF_8))
                    out.flush()
                }
            }
        }
    }

    val importJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        if (uri != null) {
            val content = context.contentResolver.openInputStream(uri)?.use { input ->
                input.bufferedReader().use { it.readText() }
            }
            if (content != null) {
                viewModel.importJson(content) { _, _ -> }
            }
        }
    }

    val pickTreeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
            } catch (@Suppress("SwallowedException") e: SecurityException) {
                // Best effort URI persist
            }
            viewModel.setAutoBackupUri(uri.toString())
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(colors.surfaceBase)
            .padding(horizontal = spacing.s4),
        verticalArrangement = Arrangement.spacedBy(spacing.s4),
    ) {
        item(key = "top_bar") {
            Spacer(modifier = Modifier.height(spacing.s2))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.s2),
            ) {
                OSIconButton(
                    onClick = onNavigateBack,
                    contentDescription = "Navigate back",
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        tint = colors.textSecondary,
                    )
                }
                Text(
                    text = "Data & Storage",
                    style = type.viewTitle.copy(color = colors.textPrimary),
                )
            }
        }

        // Status Feedback Banner
        if (state.statusMessage != null) {
            item(key = "status_banner") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(radius.md))
                        .background(colors.accent.copy(alpha = 0.12f))
                        .border(1.dp, colors.accent.copy(alpha = 0.3f), RoundedCornerShape(radius.md))
                        .padding(horizontal = spacing.s3, vertical = spacing.s2),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = state.statusMessage.orEmpty(),
                        style = type.body.copy(color = colors.accent),
                        modifier = Modifier.weight(1f),
                    )
                    OSIconButton(
                        onClick = { viewModel.clearStatusMessage() },
                        contentDescription = "Dismiss notification",
                        modifier = Modifier.size(28.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }

        // 1. Local Database Overview
        item(key = "database_overview_card") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(radius.lg))
                    .background(colors.surfaceRaised)
                    .border(1.dp, colors.borderGlass, RoundedCornerShape(radius.lg))
                    .padding(spacing.s4),
                verticalArrangement = Arrangement.spacedBy(spacing.s3),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.s2),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint = colors.accent,
                        modifier = Modifier.size(24.dp),
                    )
                    Text(
                        text = "Local Storage (SQLite WAL)",
                        style = type.subViewTitle.copy(color = colors.textPrimary),
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    DataStatPill("Tasks", state.dataCounts.taskCount.toString())
                    DataStatPill("Projects", state.dataCounts.projectCount.toString())
                    DataStatPill("Areas", state.dataCounts.areaCount.toString())
                    DataStatPill("Tags", state.dataCounts.tagCount.toString())
                }
            }
        }

        // 2. Data Portability (Export & Import via SAF)
        item(key = "data_portability_card") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(radius.lg))
                    .background(colors.surfaceRaised)
                    .border(1.dp, colors.borderGlass, RoundedCornerShape(radius.lg))
                    .padding(spacing.s4),
                verticalArrangement = Arrangement.spacedBy(spacing.s3),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.s2),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Share,
                        contentDescription = null,
                        tint = colors.accent,
                        modifier = Modifier.size(24.dp),
                    )
                    Text(
                        text = "Data Portability",
                        style = type.subViewTitle.copy(color = colors.textPrimary),
                    )
                }

                Text(
                    text = "Export and import your entire workspace via system storage. " +
                        "JSON backups support round-trip restore and Desktop OS11 migrations.",
                    style = type.meta.copy(color = colors.textSecondary),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.s2),
                ) {
                    OSButton(
                        onClick = { exportJsonLauncher.launch("os11_backup.json") },
                        variant = OSButtonVariant.Primary,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Export JSON")
                    }

                    OSButton(
                        onClick = { exportCsvLauncher.launch("os11_tasks.csv") },
                        variant = OSButtonVariant.Secondary,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Export CSV")
                    }
                }

                OSButton(
                    onClick = { importJsonLauncher.launch(arrayOf("application/json", "text/*")) },
                    variant = OSButtonVariant.Secondary,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Import JSON Backup")
                }
            }
        }

        // 3. Automated Backup (WorkManager + ZIP)
        item(key = "automated_backup_card") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(radius.lg))
                    .background(colors.surfaceRaised)
                    .border(1.dp, colors.borderGlass, RoundedCornerShape(radius.lg))
                    .padding(spacing.s4),
                verticalArrangement = Arrangement.spacedBy(spacing.s3),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.s2),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DateRange,
                        contentDescription = null,
                        tint = colors.accent,
                        modifier = Modifier.size(24.dp),
                    )
                    Text(
                        text = "Automated Backup",
                        style = type.subViewTitle.copy(color = colors.textPrimary),
                    )
                }

                Text(
                    text = "Automatically creates a daily timestamped ZIP archive containing backup.json " +
                        "and metadata.json in your chosen folder. Keeps up to 14 latest backups.",
                    style = type.meta.copy(color = colors.textSecondary),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "Daily Auto-Backup",
                        style = type.body.copy(color = colors.textPrimary),
                    )
                    OSSwitch(
                        checked = state.backup.autoBackupEnabled,
                        onCheckedChange = { viewModel.setAutoBackupEnabled(it) },
                        contentDescription = "Toggle daily auto-backup",
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Backup Folder",
                            style = type.meta.copy(color = colors.textSecondary),
                        )
                        Text(
                            text = if (!state.backup.autoBackupUri.isNullOrBlank()) {
                                Uri.parse(state.backup.autoBackupUri).lastPathSegment ?: "Folder Selected"
                            } else {
                                "No folder selected"
                            },
                            style = type.mono.copy(color = colors.textPrimary),
                            maxLines = 1,
                        )
                    }
                    OSButton(
                        onClick = { pickTreeLauncher.launch(null) },
                        variant = OSButtonVariant.Secondary,
                    ) {
                        Text("Pick Folder")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text(
                            text = "Last Backup",
                            style = type.meta.copy(color = colors.textSecondary),
                        )
                        Text(
                            text = state.backup.lastBackupTimestamp ?: "Never",
                            style = type.mono.copy(color = colors.textSecondary),
                        )
                    }

                    OSButton(
                        onClick = { viewModel.performBackupNow(context) { _, _ -> } },
                        variant = OSButtonVariant.Primary,
                    ) {
                        Text("Backup Now")
                    }
                }
            }
        }

        // 4. Synced Settings Allowlist
        item(key = "sync_allowlist_card") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(radius.lg))
                    .background(colors.surfaceRaised)
                    .border(1.dp, colors.borderGlass, RoundedCornerShape(radius.lg))
                    .padding(spacing.s4),
                verticalArrangement = Arrangement.spacedBy(spacing.s2),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.s2),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = null,
                        tint = colors.accent,
                        modifier = Modifier.size(24.dp),
                    )
                    Text(
                        text = "Synced Settings Allowlist",
                        style = type.subViewTitle.copy(color = colors.textPrimary),
                    )
                }

                Text(
                    text = "Per SCHEMA.md, exactly 7 settings keys travel across devices during sync. " +
                        "All other preferences are strictly device-local.",
                    style = type.meta.copy(color = colors.textSecondary),
                )

                Spacer(modifier = Modifier.height(spacing.s1))

                TrackedWriter.SYNCED_SETTINGS_ALLOWLIST.forEach { key ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = spacing.s1),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(spacing.s2),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Check,
                            contentDescription = "Synced",
                            tint = colors.success,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = key,
                            style = type.mono.copy(color = colors.textPrimary),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DataStatPill(label: String, value: String) {
    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing
    val type = OS11Theme.type
    val radius = OS11Theme.radius

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(radius.md))
            .background(colors.surfaceGlass)
            .border(1.dp, colors.borderGlass, RoundedCornerShape(radius.md))
            .padding(horizontal = spacing.s3, vertical = spacing.s2),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = value,
            style = type.subViewTitle.copy(color = colors.textPrimary),
        )
        Text(
            text = label,
            style = type.meta.copy(color = colors.textSecondary),
        )
    }
}
