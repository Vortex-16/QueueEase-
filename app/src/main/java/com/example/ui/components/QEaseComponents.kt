package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.theme.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CrowdBadge(
    level: CrowdLevel,
    freshnessLabel: String = "",
    confidencePercent: Int = 90,
    modifier: Modifier = Modifier
) {
    val (bg, fg, icon) = when (level) {
        CrowdLevel.LOW -> Triple(SageContainer, DeepForestDark, Icons.Default.CheckCircle)
        CrowdLevel.MODERATE -> Triple(PolishAmberBg, PolishAmber, Icons.Default.AccessTime)
        CrowdLevel.HIGH -> Triple(PolishOrangeBg, PolishOrange, Icons.Default.Group)
        CrowdLevel.SEVERE -> Triple(PolishRedBg, PolishRed, Icons.Default.Warning)
    }

    Surface(
        color = bg,
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = fg,
                modifier = Modifier
                    .size(13.dp)
                    .align(Alignment.CenterVertically)
            )
            Text(
                text = level.label,
                color = fg,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                modifier = Modifier.align(Alignment.CenterVertically)
            )
            if (confidencePercent > 0) {
                Text(
                    text = "• $confidencePercent%",
                    color = fg.copy(alpha = 0.8f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
            }
        }
    }
}

@Composable
fun TokenStatusBadge(
    status: TokenStatus,
    modifier: Modifier = Modifier
) {
    val (bg, fg, label) = when (status) {
        TokenStatus.WAITING -> Triple(SageContainer, DeepForestDark, "IN QUEUE")
        TokenStatus.CALLED -> Triple(PolishAmberBg, PolishAmber, "CALLED TO DESK")
        TokenStatus.SERVING -> Triple(PolishGreenBg, PolishGreen, "NOW SERVING")
        TokenStatus.COMPLETED -> Triple(BorderPolish, TextMuted, "COMPLETED")
        TokenStatus.CANCELLED -> Triple(PolishRedBg, PolishRed, "CANCELLED")
        TokenStatus.NO_SHOW -> Triple(BorderPolish, TextMuted, "NO SHOW")
    }

    Surface(
        color = bg,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Text(
            text = label,
            color = fg,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun LivePulseIndicator(
    modifier: Modifier = Modifier,
    color: Color = PolishGreen
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Box(
        modifier = modifier
            .size(8.dp)
            .background(color.copy(alpha = alpha), CircleShape)
    )
}

@Composable
fun CategoryFilterRow(
    selectedCategory: ServiceCategory?,
    onSelect: (ServiceCategory?) -> Unit,
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = 16.dp
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = horizontalPadding, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val isAllSelected = selectedCategory == null
        Surface(
            onClick = { onSelect(null) },
            shape = RoundedCornerShape(20.dp),
            color = if (isAllSelected) DeepForestDark else SurfacePolish,
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isAllSelected) DeepForestDark else BorderPolish
            ),
            modifier = Modifier
                .heightIn(min = 38.dp)
                .testTag("filter_all_services")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Apps,
                    contentDescription = null,
                    tint = if (isAllSelected) Color.White else TextMuted,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "All Services",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isAllSelected) Color.White else TextDark
                )
            }
        }

        ServiceCategory.entries.forEach { cat ->
            val isSelected = selectedCategory == cat
            val icon = when (cat) {
                ServiceCategory.CIVIL_ID -> Icons.Default.Badge
                ServiceCategory.HEALTHCARE -> Icons.Default.LocalHospital
                ServiceCategory.TRANSPORT_RTO -> Icons.Default.DirectionsCar
                ServiceCategory.BANKING_FINANCE -> Icons.Default.AccountBalance
                ServiceCategory.MUNICIPAL_CIVIC -> Icons.Default.LocationCity
                ServiceCategory.EDUCATION -> Icons.Default.School
            }
            Surface(
                onClick = { onSelect(if (isSelected) null else cat) },
                shape = RoundedCornerShape(20.dp),
                color = if (isSelected) DeepForestDark else SurfacePolish,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isSelected) DeepForestDark else BorderPolish
                ),
                modifier = Modifier
                    .heightIn(min = 38.dp)
                    .testTag("filter_category_${cat.name.lowercase()}")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isSelected) Color.White else TextMuted,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = cat.displayName,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.White else TextDark
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = 16.dp
) {
    FlowRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .align(Alignment.CenterVertically)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }
        }
        if (!actionText.isNullOrBlank() && onAction != null) {
            TextButton(
                onClick = onAction,
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .heightIn(min = 40.dp)
                    .testTag("header_action_${title.lowercase().replace(" ", "_")}")
            ) {
                Text(actionText, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PolishBlue)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ActiveTokenHeroCard(
    token: QueueToken,
    onLeaveQueue: () -> Unit,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val fontScale = LocalDensity.current.fontScale

    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = SageContainer),
        shape = RoundedCornerShape(32.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderMedium),
        modifier = modifier
            .fillMaxWidth()
            .testTag("active_token_hero_card")
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val stackStats = (maxWidth / fontScale) < 280.dp

            Column(modifier = Modifier.padding(20.dp)) {
                // Header Row: Active Token label & Live pill
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Text(
                            text = "CURRENT ACTIVE TOKEN",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = DeepForestDark.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = token.tokenNumber,
                            fontSize = 40.sp,
                            lineHeight = 44.sp,
                            fontWeight = FontWeight.Black,
                            color = DeepForestDark,
                            letterSpacing = (-1).sp
                        )
                    }

                    Surface(
                        color = if (token.status == TokenStatus.CALLED) PolishAmber else DeepForestDark,
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(
                            text = if (token.status == TokenStatus.CALLED) "DESK CALL" else "LIVE",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Subcards grid: Position & Est Wait
                if (stackStats) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TokenMetricTile(
                            label = "POSITION",
                            value = "${token.peopleAhead}",
                            unit = "ahead",
                            modifier = Modifier.fillMaxWidth()
                        )
                        TokenMetricTile(
                            label = "EST. WAIT",
                            value = "${token.estimatedWaitMin}-${token.estimatedWaitMax}",
                            unit = "min",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Max),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        TokenMetricTile(
                            label = "POSITION",
                            value = "${token.peopleAhead}",
                            unit = "ahead",
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                        TokenMetricTile(
                            label = "EST. WAIT",
                            value = "${token.estimatedWaitMin}-${token.estimatedWaitMax}",
                            unit = "min",
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Footer: Live sync pulse & Leave Queue button
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    ) {
                        LivePulseIndicator()
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Updated 2m ago",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextMuted
                        )
                    }

                    Surface(
                        onClick = onLeaveQueue,
                        color = Color.White,
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderMedium),
                        modifier = Modifier
                            .align(Alignment.CenterVertically)
                            .heightIn(min = 36.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = "Leave Queue",
                                color = DeepForestDark,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TokenMetricTile(
    label: String,
    value: String,
    unit: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color.White.copy(alpha = 0.55f),
        shape = RoundedCornerShape(18.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = TextMuted,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = value,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeepForestDark,
                    modifier = Modifier.align(Alignment.Bottom)
                )
                Text(
                    text = unit,
                    fontSize = 11.sp,
                    color = DeepForestDark.copy(alpha = 0.7f),
                    modifier = Modifier
                        .align(Alignment.Bottom)
                        .padding(bottom = 2.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DocumentItemRow(
    doc: RequiredDocument,
    isChecked: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onToggle,
        shape = RoundedCornerShape(16.dp),
        color = if (isChecked) SageContainer.copy(alpha = 0.45f) else SurfacePolish,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isChecked) SageAccent else BorderPolish
        ),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .testTag("doc_item_${doc.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Checkbox(
                checked = isChecked,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = DeepForestDark,
                    uncheckedColor = BorderMedium
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = doc.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextDark,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                    if (doc.isMandatory) {
                        Surface(
                            color = PolishRedBg,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.align(Alignment.CenterVertically)
                        ) {
                            Text(
                                text = "MANDATORY",
                                color = PolishRed,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    } else {
                        Surface(
                            color = SurfaceVariantPolish,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.align(Alignment.CenterVertically)
                        ) {
                            Text(
                                text = "OPTIONAL",
                                color = TextMuted,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = doc.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    fontSize = 11.sp
                )
                if (doc.acceptableAlternatives.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Alternatives: ${doc.acceptableAlternatives.joinToString(", ")}",
                        fontSize = 11.sp,
                        color = SageAccent,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

