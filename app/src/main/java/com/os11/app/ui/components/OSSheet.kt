package com.os11.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.os11.app.ui.theme.OS11Theme

/**
 * OS11 Bottom Sheet container.
 *
 * Restyled modal bottom sheet with OS11 tokens, top corner radius (12dp),
 * custom minimal drag handle, dark surface token, and IME insets.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OSSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = OS11Theme.colors
    val radius = OS11Theme.radius
    val spacing = OS11Theme.spacing

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = sheetState,
        shape = RoundedCornerShape(
            topStart = radius.lg,
            topEnd = radius.lg,
            bottomStart = 0.dp,
            bottomEnd = 0.dp,
        ),
        containerColor = colors.surfaceSidebar,
        contentColor = colors.textPrimary,
        scrimColor = colors.scrim,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = spacing.s3)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(RoundedCornerShape(radius.full))
                    .background(colors.borderStrong),
            )
        },
        contentWindowInsets = { WindowInsets.navigationBars },
        content = content,
    )
}
