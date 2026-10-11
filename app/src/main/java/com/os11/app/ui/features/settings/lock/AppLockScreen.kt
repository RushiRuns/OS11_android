package com.os11.app.ui.features.settings.lock

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.os11.app.ui.components.OSButton
import com.os11.app.ui.components.OSButtonVariant
import com.os11.app.ui.theme.OS11Theme

@Composable
fun AppLockScreen(onUnlockClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing
    val type = OS11Theme.type
    val radius = OS11Theme.radius

    LaunchedEffect(Unit) {
        onUnlockClick()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.surfaceBase)
            .padding(spacing.s6),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(radius.xl))
                .background(colors.surfaceRaised)
                .border(1.dp, colors.borderGlass, RoundedCornerShape(radius.xl))
                .padding(spacing.s6),
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(colors.surfaceGlass)
                    .border(1.dp, colors.borderGlass, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = "Locked",
                    tint = colors.accent,
                    modifier = Modifier.size(32.dp),
                )
            }

            Spacer(modifier = Modifier.height(spacing.s4))

            Text(
                text = "OS11 is Locked",
                style = type.viewTitle.copy(color = colors.textPrimary),
            )

            Spacer(modifier = Modifier.height(spacing.s2))

            Text(
                text = "Biometric authentication or device PIN required to access your workspace.",
                style = type.body.copy(color = colors.textSecondary),
            )

            Spacer(modifier = Modifier.height(spacing.s6))

            OSButton(
                onClick = onUnlockClick,
                variant = OSButtonVariant.Primary,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "Unlock OS11",
                    style = type.chip,
                )
            }
        }
    }
}
