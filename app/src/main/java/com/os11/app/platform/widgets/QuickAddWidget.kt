package com.os11.app.platform.widgets

import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.os11.app.MainActivity
import com.os11.app.R
import com.os11.app.platform.shortcuts.AppShortcutManager

/**
 * Compact home-screen widget button for triggering Quick Add directly from the launcher.
 */
@Suppress("MagicNumber")
class QuickAddWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val bgSurface = ColorProvider(Color(0xFF1C1C1C))
            val accentColor = ColorProvider(Color(0xFF4DA3FF))
            val textPrimary = ColorProvider(Color(0xFFE6E6E6))

            val quickAddIntent = Intent(context, MainActivity::class.java).apply {
                action = AppShortcutManager.ACTION_QUICK_ADD
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(bgSurface)
                    .cornerRadius(16.dp)
                    .clickable(actionStartActivity(quickAddIntent))
                    .padding(8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = GlanceModifier
                            .size(36.dp)
                            .background(ColorProvider(Color(0xFF262626)))
                            .cornerRadius(10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            provider = ImageProvider(R.drawable.ic_quick_add),
                            contentDescription = "Quick Add",
                            modifier = GlanceModifier.size(20.dp),
                        )
                    }

                    Spacer(modifier = GlanceModifier.width(8.dp))

                    Column {
                        Text(
                            text = "Quick Add",
                            style = TextStyle(
                                color = textPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                        )
                        Text(
                            text = "New task",
                            style = TextStyle(
                                color = accentColor,
                                fontSize = 11.sp,
                            ),
                        )
                    }
                }
            }
        }
    }
}
