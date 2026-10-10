package com.os11.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.os11.app.ui.theme.OS11Theme

/**
 * Segmented control component for view switchers and option toggles.
 */
@Composable
fun <T> OSSegmented(
    options: List<T>,
    selectedOption: T,
    onOptionSelected: (T) -> Unit,
    labelProvider: (T) -> String,
    modifier: Modifier = Modifier,
) {
    val colors = OS11Theme.colors
    val spacing = OS11Theme.spacing
    val radius = OS11Theme.radius
    val layout = OS11Theme.layout
    val type = OS11Theme.type

    val outerShape = RoundedCornerShape(radius.md)
    val innerShape = RoundedCornerShape(radius.sm)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = layout.minTouchTarget)
            .clip(outerShape)
            .border(spacing.px, colors.borderDefault, outerShape)
            .background(colors.surfaceRaised)
            .padding(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        options.forEach { option ->
            val isSelected = option == selectedOption
            val interactionSource = remember { MutableInteractionSource() }
            val itemBg = if (isSelected) colors.surfaceSelected else colors.surfaceRaised
            val itemTextColor = if (isSelected) colors.accent else colors.textSecondary

            Box(
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = layout.minTouchTarget - 4.dp)
                    .clip(innerShape)
                    .background(itemBg)
                    .clickable(
                        role = Role.Button,
                        interactionSource = interactionSource,
                        indication = ripple(color = colors.surfaceHover),
                        onClick = { onOptionSelected(option) },
                    )
                    .padding(horizontal = spacing.s2, vertical = spacing.s1),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = labelProvider(option),
                    style = type.chip.copy(color = itemTextColor),
                )
            }
        }
    }
}
