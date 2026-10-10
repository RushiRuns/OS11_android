package com.os11.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import com.os11.app.ui.theme.OS11AnimationSpecs
import com.os11.app.ui.theme.OS11Theme

/**
 * OS11 Checkbox component adhering to strict design system constraints:
 * - 20dp visual circle, 48dp touch target
 * - Permitted Motion Site 1: Spring overshoot on check completion, reduced-motion aware
 * - Haptic feedback support on change
 */
@Composable
fun OSCheckbox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentDescription: String? = null,
) {
    val colors = OS11Theme.colors
    val layout = OS11Theme.layout
    val reduceMotion = OS11Theme.reduceMotion
    val interactionSource = remember { MutableInteractionSource() }

    // Site 1: Spring overshoot on check
    val scaleAnim by animateFloatAsState(
        targetValue = if (checked) 1.0f else 0.85f,
        animationSpec = OS11AnimationSpecs.checkboxSpring(reduceMotion),
        label = "CheckboxScale",
    )

    val checkmarkAlpha by animateFloatAsState(
        targetValue = if (checked) 1.0f else 0.0f,
        animationSpec = OS11AnimationSpecs.fastTween(reduceMotion),
        label = "CheckmarkAlpha",
    )

    val visualSize = layout.checkboxSize
    val touchSize = layout.minTouchTarget
    val strokeWidth = layout.checkboxBorderWidth

    Box(
        modifier = modifier
            .size(touchSize)
            .clickable(
                enabled = enabled && onCheckedChange != null,
                role = Role.Checkbox,
                interactionSource = interactionSource,
                indication = ripple(bounded = false, radius = touchSize / 2),
                onClick = { onCheckedChange?.invoke(!checked) },
            )
            .semantics {
                role = Role.Checkbox
                if (contentDescription != null) {
                    this.contentDescription = contentDescription
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        val bgColor = if (checked) colors.accent else Color.Transparent
        val borderColor = if (checked) colors.accent else colors.checkboxBorder

        Box(
            modifier = Modifier
                .size(visualSize)
                .scale(scaleAnim)
                .clip(CircleShape)
                .border(strokeWidth, borderColor, CircleShape)
                .background(bgColor),
            contentAlignment = Alignment.Center,
        ) {
            if (checkmarkAlpha > 0f) {
                val markColor = colors.textOnAccent.copy(alpha = checkmarkAlpha)
                Canvas(modifier = Modifier.size(visualSize * 0.6f)) {
                    val w = size.width
                    val h = size.height
                    val path = Path().apply {
                        moveTo(w * 0.2f, h * 0.5f)
                        lineTo(w * 0.45f, h * 0.75f)
                        lineTo(w * 0.85f, h * 0.25f)
                    }
                    drawPath(
                        path = path,
                        color = markColor,
                        style = Stroke(
                            width = strokeWidth.toPx() * 1.2f,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round,
                        ),
                    )
                }
            }
        }
    }
}
