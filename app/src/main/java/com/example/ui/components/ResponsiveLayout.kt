package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.BackgroundPolish
import com.example.ui.theme.BorderPolish

enum class QEaseWindowWidthClass {
    COMPACT,
    MEDIUM,
    EXPANDED
}

@Immutable
data class QEaseLayoutMetrics(
    val widthClass: QEaseWindowWidthClass,
    val availableWidth: Dp,
    val fontScale: Float
) {
    val isCompact: Boolean get() = widthClass == QEaseWindowWidthClass.COMPACT
    val isMedium: Boolean get() = widthClass == QEaseWindowWidthClass.MEDIUM
    val isExpanded: Boolean get() = widthClass == QEaseWindowWidthClass.EXPANDED
    val isWide: Boolean get() = widthClass != QEaseWindowWidthClass.COMPACT

    /**
     * Effective width takes font scaling into account so layouts at 125%+
     * treat tight viewports like narrower ones and avoid text squishing.
     */
    val effectiveWidthDp: Float get() = availableWidth.value / fontScale.coerceAtLeast(1f)
    val isScaledDisplay: Boolean get() = fontScale >= 1.15f
    val isTightHorizontal: Boolean get() = effectiveWidthDp < 360f

    val horizontalPadding: Dp
        get() = when (widthClass) {
            QEaseWindowWidthClass.COMPACT -> 16.dp
            QEaseWindowWidthClass.MEDIUM -> 24.dp
            QEaseWindowWidthClass.EXPANDED -> 28.dp
        }

    val gridColumns: Int
        get() = when (widthClass) {
            QEaseWindowWidthClass.COMPACT -> 1
            QEaseWindowWidthClass.MEDIUM -> if (effectiveWidthDp >= 480f) 2 else 1
            QEaseWindowWidthClass.EXPANDED -> if (effectiveWidthDp >= 640f) 2 else 1
        }
}

@Composable
fun rememberQEaseLayoutMetrics(availableWidth: Dp): QEaseLayoutMetrics {
    val fontScale = LocalDensity.current.fontScale
    val widthClass = when {
        availableWidth < 600.dp -> QEaseWindowWidthClass.COMPACT
        availableWidth < 840.dp -> QEaseWindowWidthClass.MEDIUM
        else -> QEaseWindowWidthClass.EXPANDED
    }
    return QEaseLayoutMetrics(
        widthClass = widthClass,
        availableWidth = availableWidth,
        fontScale = fontScale
    )
}

@Composable
fun QEaseResponsiveContainer(
    modifier: Modifier = Modifier,
    maxContentWidth: Dp = 1120.dp,
    content: @Composable (QEaseLayoutMetrics) -> Unit
) {
    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        val metrics = rememberQEaseLayoutMetrics(maxWidth)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = maxContentWidth)
        ) {
            content(metrics)
        }
    }
}

/**
 * Renders a row of [columns] items with uniform height using IntrinsicSize.Max
 * so multi-column cards stay visually aligned even when font scaling increases text wrapping.
 */
@Composable
fun <T> AdaptiveGridRow(
    items: List<T>,
    columns: Int,
    modifier: Modifier = Modifier,
    horizontalSpacing: Dp = 12.dp,
    itemContent: @Composable (T, Modifier) -> Unit
) {
    if (columns <= 1 || items.size == 1 && columns == 1) {
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items.forEach { item ->
                itemContent(item, Modifier.fillMaxWidth())
            }
        }
    } else {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Max),
            horizontalArrangement = Arrangement.spacedBy(horizontalSpacing)
        ) {
            for (index in 0 until columns) {
                val item = items.getOrNull(index)
                if (item != null) {
                    itemContent(
                        item,
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

/**
 * Scrollable, width-bounded dialog shell that prevents clipping at 125%+ scaling
 * or on compact/landscape screens.
 */
@Composable
fun ResponsiveDialogShell(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    maxWidth: Dp = 520.dp,
    contentPadding: PaddingValues = PaddingValues(22.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(26.dp),
                color = BackgroundPolish,
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderPolish),
                shadowElevation = 16.dp,
                modifier = modifier
                    .fillMaxWidth()
                    .widthIn(max = maxWidth)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(contentPadding),
                    content = content
                )
            }
        }
    }
}
