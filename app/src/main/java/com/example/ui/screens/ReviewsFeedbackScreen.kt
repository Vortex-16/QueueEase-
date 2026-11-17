package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CentreReview
import com.example.data.model.ServiceCentre
import com.example.ui.components.AdaptiveGridRow
import com.example.ui.components.QEaseResponsiveContainer
import com.example.ui.components.ResponsiveDialogShell
import com.example.ui.theme.*
import com.example.ui.viewmodel.QEaseViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReviewsFeedbackScreen(
    viewModel: QEaseViewModel
) {
    val reviews by viewModel.reviews.collectAsState()
    val centres by viewModel.centres.collectAsState()
    val selectedCentreFilter by viewModel.selectedCentre.collectAsState()

    var activeFilterTag by remember { mutableStateOf<String?>(null) }
    var showWriteReviewDialog by remember { mutableStateOf(false) }

    val filteredReviews = remember(reviews, selectedCentreFilter, activeFilterTag) {
        reviews.filter { review ->
            val matchesCentre = selectedCentreFilter == null || review.centreId == selectedCentreFilter?.id
            val matchesTag = activeFilterTag == null ||
                review.visitTag.contains(activeFilterTag!!, ignoreCase = true) ||
                review.queueSpeedRating.equals(activeFilterTag, ignoreCase = true)
            matchesCentre && matchesTag
        }
    }

    Scaffold(
        containerColor = BackgroundPolish,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showWriteReviewDialog = true },
                containerColor = DeepForestDark,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("write_review_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.RateReview,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Write Review",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    ) { padding ->
        QEaseResponsiveContainer(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            maxContentWidth = 1120.dp
        ) { metrics ->
            val columns = metrics.gridColumns
            val reviewRows = remember(filteredReviews, columns) {
                filteredReviews.chunked(columns)
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = metrics.horizontalPadding)
                    .testTag("reviews_feedback_screen"),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 88.dp)
            ) {
                // Header Rating Metric Hero Card
                item {
                    Surface(
                        color = DeepForestDark,
                        shape = RoundedCornerShape(24.dp),
                        shadowElevation = 4.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                text = "CITIZEN REVIEWS & FEEDBACK",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = SageContainer,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f, fill = false)) {
                                    Row(verticalAlignment = Alignment.Bottom) {
                                        Text(
                                            text = if (selectedCentreFilter != null) "${selectedCentreFilter?.rating}" else "4.8",
                                            fontSize = 36.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color.White
                                        )
                                        Text(
                                            text = " / 5.0",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White.copy(alpha = 0.7f),
                                            modifier = Modifier.padding(bottom = 6.dp)
                                        )
                                    }
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                            repeat(5) {
                                                Icon(
                                                    imageVector = Icons.Default.Star,
                                                    contentDescription = null,
                                                    tint = PolishAmber,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text(
                                            text = "${reviews.size} verified citizen reviews",
                                            fontSize = 11.sp,
                                            color = Color.White.copy(alpha = 0.8f)
                                        )
                                    }
                                }

                                Surface(
                                    color = SageContainer,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                    ) {
                                        Text(
                                            text = "96%",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Black,
                                            color = DeepForestDark
                                        )
                                        Text(
                                            text = "On-Time Service",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = DeepForestDark
                                        )
                                    }
                                }
                            }

                            if (selectedCentreFilter != null) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Surface(
                                    color = Color.White.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    FlowRow(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "Filtering by: ${selectedCentreFilter?.name}",
                                            fontSize = 11.sp,
                                            color = Color.White
                                        )
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Clear filter",
                                            tint = Color.White,
                                            modifier = Modifier
                                                .size(18.dp)
                                                .clickable { viewModel.selectCentre(null) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Quick Category / Speed Filters
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChipPill(
                            label = "All Reviews",
                            isSelected = activeFilterTag == null,
                            onClick = { activeFilterTag = null }
                        )
                        FilterChipPill(
                            label = "⚡ Fast Speed",
                            isSelected = activeFilterTag == "Fast",
                            onClick = { activeFilterTag = if (activeFilterTag == "Fast") null else "Fast" }
                        )
                        FilterChipPill(
                            label = "Passport",
                            isSelected = activeFilterTag == "Passport",
                            onClick = { activeFilterTag = if (activeFilterTag == "Passport") null else "Passport" }
                        )
                        FilterChipPill(
                            label = "Aadhaar",
                            isSelected = activeFilterTag == "Aadhaar",
                            onClick = { activeFilterTag = if (activeFilterTag == "Aadhaar") null else "Aadhaar" }
                        )
                        FilterChipPill(
                            label = "DL / Transport",
                            isSelected = activeFilterTag == "DL",
                            onClick = { activeFilterTag = if (activeFilterTag == "DL") null else "DL" }
                        )
                    }
                }

                // Reviews List / Adaptive Grid
                if (filteredReviews.isEmpty()) {
                    item {
                        Surface(
                            color = SurfaceGlass,
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderPolish),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.RateReview,
                                    contentDescription = null,
                                    tint = TextMuted,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No reviews matching this filter",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DeepForestDark
                                )
                                Text(
                                    text = "Be the first to share your queue experience!",
                                    fontSize = 12.sp,
                                    color = TextMuted
                                )
                            }
                        }
                    }
                } else {
                    items(reviewRows) { rowItems ->
                        AdaptiveGridRow(
                            items = rowItems,
                            columns = columns
                        ) { review, cardModifier ->
                            ReviewItemCard(
                                review = review,
                                onUpvote = { viewModel.upvoteReview(review.id) },
                                modifier = cardModifier
                            )
                        }
                    }
                }
            }
        }
    }

    if (showWriteReviewDialog) {
        WriteReviewDialog(
            centres = centres,
            preselectedCentre = selectedCentreFilter ?: centres.firstOrNull(),
            onDismiss = { showWriteReviewDialog = false },
            onSubmit = { centreId, rating, text, tag, speed ->
                viewModel.submitReview(
                    centreId = centreId,
                    rating = rating,
                    reviewText = text,
                    visitTag = tag,
                    queueSpeedRating = speed
                )
                showWriteReviewDialog = false
            }
        )
    }
}

@Composable
private fun FilterChipPill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = if (isSelected) DeepForestDark else SurfaceGlass,
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) DeepForestDark else BorderPolish
        ),
        modifier = Modifier
            .heightIn(min = 38.dp)
            .clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.White else TextDark
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReviewItemCard(
    review: CentreReview,
    onUpvote: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = SurfaceGlass,
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderPolish),
        shadowElevation = 1.dp,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = SageContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = review.userName.take(1).uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = DeepForestDark
                                )
                            }
                        }

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = review.userName,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DeepForestDark
                                )
                                if (review.isVerifiedVisitor) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.Verified,
                                        contentDescription = "Verified Visitor",
                                        tint = PolishGreen,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                            Text(
                                text = review.centreName,
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        repeat(review.rating) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = PolishAmber,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = SageContainer,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = review.visitTag,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = DeepForestDark,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Surface(
                        color = when (review.queueSpeedRating) {
                            "Fast" -> PolishGreenBg
                            "Slow" -> PolishRedBg
                            else -> PolishAmberBg
                        },
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "${review.queueSpeedRating} Queue",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (review.queueSpeedRating) {
                                "Fast" -> PolishGreen
                                "Slow" -> PolishRed
                                else -> PolishAmber
                            },
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = review.reviewText,
                    fontSize = 12.sp,
                    color = TextDark,
                    lineHeight = 17.sp
                )
            }

            Column {
                Spacer(modifier = Modifier.height(12.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(review.timestamp)),
                        fontSize = 10.sp,
                        color = TextMuted
                    )

                    Surface(
                        color = SurfaceContainerNav,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .heightIn(min = 36.dp)
                            .clickable(onClick = onUpvote)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ThumbUp,
                                contentDescription = "Helpful",
                                tint = DeepForestDark,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Helpful (${review.helpfulCount})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = DeepForestDark
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
private fun WriteReviewDialog(
    centres: List<ServiceCentre>,
    preselectedCentre: ServiceCentre?,
    onDismiss: () -> Unit,
    onSubmit: (centreId: String, rating: Int, text: String, tag: String, speed: String) -> Unit
) {
    var selectedCentreId by remember { mutableStateOf(preselectedCentre?.id ?: centres.firstOrNull()?.id ?: "") }
    var rating by remember { mutableStateOf(5) }
    var reviewTextInput by remember { mutableStateOf("") }
    val visitTagInput = "Passport Renewal"
    var queueSpeed by remember { mutableStateOf("Fast") }

    ResponsiveDialogShell(onDismissRequest = onDismiss) {
        Text(
            text = "Citizen Experience Feedback",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = DeepForestDark
        )
        Text(
            text = "Help fellow citizens plan their visit with accurate queue data.",
            fontSize = 11.sp,
            color = TextMuted
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text("Select Service Centre", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = DeepForestDark)
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            centres.forEach { centre ->
                val isSelected = selectedCentreId == centre.id
                Surface(
                    color = if (isSelected) DeepForestDark else SurfaceContainerNav,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .heightIn(min = 38.dp)
                        .clickable { selectedCentreId = centre.id }
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                        Text(
                            text = centre.name,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else TextMuted
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text("Rating", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = DeepForestDark)
        Spacer(modifier = Modifier.height(4.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            (1..5).forEach { star ->
                IconButton(
                    onClick = { rating = star },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = if (star <= rating) Icons.Default.Star else Icons.Outlined.StarBorder,
                        contentDescription = "Star $star",
                        tint = if (star <= rating) PolishAmber else TextMuted,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "$rating Stars",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = DeepForestDark
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text("Queue Processing Speed", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = DeepForestDark)
        Spacer(modifier = Modifier.height(6.dp))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Fast", "Normal", "Slow").forEach { speed ->
                val isSelected = queueSpeed == speed
                Surface(
                    color = if (isSelected) DeepForestDark else SurfaceContainerNav,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f, fill = true)
                        .widthIn(min = 84.dp)
                        .heightIn(min = 38.dp)
                        .clickable { queueSpeed = speed }
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp)) {
                        Text(
                            text = speed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else TextMuted,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text("Your Experience & Tips", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = DeepForestDark)
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = reviewTextInput,
            onValueChange = { reviewTextInput = it },
            placeholder = { Text("e.g. Counter 2 staff was very efficient. Bring cash for fee.", fontSize = 12.sp) },
            minLines = 3,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .weight(1f, fill = true)
                    .widthIn(min = 100.dp)
                    .heightIn(min = 48.dp)
            ) {
                Text("Cancel")
            }

            Button(
                onClick = {
                    val text = reviewTextInput.ifBlank { "Prompt service and smooth document scrutiny process." }
                    onSubmit(selectedCentreId, rating, text, visitTagInput, queueSpeed)
                },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DeepForestDark),
                modifier = Modifier
                    .weight(1.4f, fill = true)
                    .widthIn(min = 140.dp)
                    .heightIn(min = 48.dp)
            ) {
                Text("Submit Review", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}
