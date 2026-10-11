package com.os11.app.ui.features.settings

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.os11.app.ui.components.OSButton
import com.os11.app.ui.components.OSButtonVariant
import com.os11.app.ui.components.OSIconButton
import com.os11.app.ui.theme.OS11Theme

/**
 * OEM Battery Optimization Guidance Screen (Spike S-8).
 *
 * Educates users on vendor-specific background restrictions that can silently kill
 * reminders on Samsung OneUI, Xiaomi MIUI/HyperOS, OnePlus/Oppo ColorOS, and Huawei devices.
 * Provides one-tap deep links to system settings to whitelist OS11.
 */
@Composable
fun BatteryOptimizationGuidanceScreen(onNavigateBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing
    val type = OS11Theme.type
    val radius = OS11Theme.radius

    var canScheduleExact by remember { mutableStateOf(checkExactAlarmPermission(context)) }
    var isIgnoringBatteryOptimizations by remember {
        mutableStateOf(checkBatteryOptimizationExemption(context))
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                canScheduleExact = checkExactAlarmPermission(context)
                isIgnoringBatteryOptimizations = checkBatteryOptimizationExemption(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val manufacturer = Build.MANUFACTURER.uppercase()
    val model = Build.MODEL

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.surfaceBase)
            .padding(horizontal = spacing.s4)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(spacing.s4),
    ) {
        Spacer(modifier = Modifier.height(spacing.s2))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.s2),
        ) {
            OSIconButton(
                onClick = onNavigateBack,
                contentDescription = "Navigate back",
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = null,
                    tint = colors.textPrimary,
                )
            }
            Text(
                text = "Reliable Reminders",
                style = type.viewTitle.copy(color = colors.textPrimary),
            )
        }

        Text(
            text = "Modern Android phones aggressively shut down background apps to conserve battery. " +
                "To ensure your reminders and recurring tasks trigger on time, please verify the following settings.",
            style = type.meta.copy(color = colors.textSecondary),
        )

        // Exact Alarm Permission Card
        StatusCard(
            title = "Exact Alarm Permission",
            subtitle = if (canScheduleExact) {
                "Granted. OS11 can wake the device at the exact scheduled second."
            } else {
                "Action required. Reminders may be delayed without exact alarm access."
            },
            isResolved = canScheduleExact,
            icon = Icons.Outlined.Notifications,
            actionLabel = if (!canScheduleExact && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                "Allow Exact Alarms"
            } else {
                null
            },
            onAction = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                    context.startActivity(intent)
                }
            },
        )

        // Battery Optimization Exemption Card
        StatusCard(
            title = "Battery Optimization",
            subtitle = if (isIgnoringBatteryOptimizations) {
                "Unrestricted. OS11 will not be terminated while your device is sleeping."
            } else {
                "Action recommended. Android Doze may delay alarms when device is idle."
            },
            isResolved = isIgnoringBatteryOptimizations,
            icon = if (isIgnoringBatteryOptimizations) Icons.Outlined.CheckCircle else Icons.Outlined.Warning,
            actionLabel = if (!isIgnoringBatteryOptimizations) "Disable Optimization" else null,
            onAction = {
                try {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                    context.startActivity(intent)
                } catch (_: Exception) {
                    val fallback = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                    context.startActivity(fallback)
                }
            },
        )

        // Vendor Guidance Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(radius.md))
                .background(colors.surfaceRaised)
                .border(spacing.px, colors.borderSubtle, RoundedCornerShape(radius.md))
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
                    modifier = Modifier.size(spacing.s5),
                )
                Text(
                    text = "Guidance for $manufacturer ($model)",
                    style = type.sectionHeader.copy(color = colors.textPrimary),
                )
            }

            Text(
                text = getVendorGuidance(manufacturer),
                style = type.meta.copy(color = colors.textSecondary),
            )

            OSButton(
                onClick = {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                    context.startActivity(intent)
                },
                variant = OSButtonVariant.Secondary,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = null,
                        tint = colors.textPrimary,
                        modifier = Modifier.size(spacing.s4),
                    )
                },
            ) {
                Text(
                    text = "Open App Settings",
                    style = type.meta.copy(color = colors.textPrimary),
                )
            }
        }

        Spacer(modifier = Modifier.height(spacing.s6))
    }
}

@Composable
private fun StatusCard(
    title: String,
    subtitle: String,
    isResolved: Boolean,
    icon: ImageVector,
    actionLabel: String?,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing
    val type = OS11Theme.type
    val radius = OS11Theme.radius

    val statusTint = if (isResolved) colors.success else colors.warning

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(radius.md))
            .background(colors.surfaceRaised)
            .border(spacing.px, colors.borderSubtle, RoundedCornerShape(radius.md))
            .padding(spacing.s4),
        verticalArrangement = Arrangement.spacedBy(spacing.s3),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.s2),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = statusTint,
                    modifier = Modifier.size(spacing.s5),
                )
                Text(
                    text = title,
                    style = type.sectionHeader.copy(color = colors.textPrimary),
                )
            }

            Icon(
                imageVector = if (isResolved) Icons.Outlined.CheckCircle else Icons.Outlined.Warning,
                contentDescription = null,
                tint = statusTint,
                modifier = Modifier.size(spacing.s4),
            )
        }

        Text(
            text = subtitle,
            style = type.meta.copy(color = colors.textSecondary),
        )

        if (actionLabel != null) {
            OSButton(
                onClick = onAction,
                variant = OSButtonVariant.Primary,
            ) {
                Text(
                    text = actionLabel,
                    style = type.meta.copy(color = colors.surfaceBase),
                )
            }
        }
    }
}

private fun checkExactAlarmPermission(context: Context): Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
    alarmManager?.canScheduleExactAlarms() == true
} else {
    true
}

private fun checkBatteryOptimizationExemption(context: Context): Boolean {
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
    return powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true
}

private fun getVendorGuidance(manufacturer: String): String = when {
    manufacturer.contains("SAMSUNG") -> {
        "Samsung OneUI automatically places apps into 'Sleeping apps' or 'Deep sleeping apps'. " +
            "To ensure reliable reminders:\n\n" +
            "1. Go to App info -> Battery -> select 'Unrestricted'.\n" +
            "2. Open Settings -> Device care -> Battery -> Background usage limits, " +
            "and ensure OS11 is in 'Never auto-sleeping apps'."
    }
    manufacturer.contains("XIAOMI") || manufacturer.contains("REDMI") || manufacturer.contains("POCO") -> {
        "Xiaomi MIUI / HyperOS heavily restricts background processes:\n\n" +
            "1. Go to App info -> enable 'Autostart'.\n" +
            "2. Set Battery saver to 'No restrictions'.\n" +
            "3. In Recent Apps, long-press OS11 and tap the Padlock icon to lock it in memory."
    }
    manufacturer.contains("OPPO") || manufacturer.contains("ONEPLUS") || manufacturer.contains("REALME") -> {
        "ColorOS / OxygenOS limits background wakeup:\n\n" +
            "1. Go to App info -> Battery usage -> enable 'Allow background activity' and 'Allow auto-launch'.\n" +
            "2. In Settings -> Battery -> More settings -> Optimize battery use, set OS11 to 'Don't optimize'."
    }
    manufacturer.contains("HUAWEI") || manufacturer.contains("HONOR") -> {
        "EMUI / MagicOS stops background tasks:\n\n" +
            "1. Go to Settings -> Battery -> App launch -> find OS11 and switch to 'Manage manually'.\n" +
            "2. Ensure 'Auto-launch', 'Secondary launch', and 'Run in background' are all enabled."
    }
    else -> {
        "Ensure battery usage is set to 'Unrestricted' in App info -> App battery usage so Android " +
            "does not defer alarms when the device enters Doze mode."
    }
}
