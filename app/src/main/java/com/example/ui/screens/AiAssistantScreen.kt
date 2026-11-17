package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChatMessage
import com.example.ui.components.LivePulseIndicator
import com.example.ui.components.QEaseResponsiveContainer
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainNavTab
import com.example.ui.viewmodel.QEaseViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAssistantScreen(
    viewModel: QEaseViewModel,
    modifier: Modifier = Modifier
) {
    val chatMessages by viewModel.chatMessages.collectAsState()
    val isAiThinking by viewModel.isAiThinking.collectAsState()

    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    val quickQuestions = listOf(
        "Required documents for Passport renewal?",
        "What is the fee for Driving License renewal?",
        "How long is the wait at Central Passport Seva Kendra?",
        "Aadhaar biometric update address proofs?"
    )

    LaunchedEffect(chatMessages.size, isAiThinking) {
        if (chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(chatMessages.size - 1)
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("ai_assistant_screen"),
        containerColor = BackgroundPolish,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(SageContainer, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = DeepForestDark,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Service Intelligence AI",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = DeepForestDark
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                LivePulseIndicator()
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Grounded in Official Guidelines",
                                    fontSize = 10.sp,
                                    color = TextMuted
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundPolish)
            )
        }
    ) { padding ->
        QEaseResponsiveContainer(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            maxContentWidth = 900.dp
        ) { metrics ->
            val bubbleMaxWidth = (metrics.availableWidth.coerceAtMost(900.dp) * 0.82f).coerceAtMost(540.dp)

            Column(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = metrics.horizontalPadding),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(chatMessages) { msg ->
                        ChatBubblePolish(
                            message = msg,
                            maxBubbleWidth = bubbleMaxWidth,
                            onOpenService = { serviceId ->
                                viewModel.services.value.find { it.id == serviceId }?.let {
                                    viewModel.selectService(it)
                                    viewModel.setNavTab(MainNavTab.SERVICES)
                                }
                            }
                        )
                    }

                    if (isAiThinking) {
                        item {
                            Surface(
                                color = SurfacePolish,
                                shape = RoundedCornerShape(20.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, BorderPolish),
                                modifier = Modifier.padding(end = 40.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = DeepForestDark
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Verifying document rules & official criteria...",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = TextMuted
                                    )
                                }
                            }
                        }
                    }
                }

                // Quick Questions Chips
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = metrics.horizontalPadding, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(quickQuestions) { query ->
                        Surface(
                            onClick = { viewModel.sendAiMessage(query) },
                            shape = RoundedCornerShape(16.dp),
                            color = SurfacePolish,
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderPolish),
                            modifier = Modifier.heightIn(min = 38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                Text(
                                    text = query,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = DeepForestDark
                                )
                            }
                        }
                    }
                }

                // Input Bar
                Surface(
                    color = SurfacePolish,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderPolish),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = metrics.horizontalPadding, vertical = 8.dp),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            placeholder = { Text("Ask about proofs, fees, procedures...", fontSize = 13.sp, color = TextMuted) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("ai_chat_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent
                            ),
                            singleLine = false,
                            maxLines = 3
                        )

                        IconButton(
                            onClick = {
                                if (inputText.isNotBlank()) {
                                    val query = inputText.trim()
                                    inputText = ""
                                    viewModel.sendAiMessage(query)
                                }
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .background(DeepForestDark, CircleShape)
                                .testTag("ai_send_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatBubblePolish(
    message: ChatMessage,
    onOpenService: (String) -> Unit,
    modifier: Modifier = Modifier,
    maxBubbleWidth: Dp = 340.dp
) {
    val isUser = message.isUser

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(SageContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = DeepForestDark, modifier = Modifier.size(14.dp))
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Surface(
            color = if (isUser) DeepForestDark else SurfacePolish,
            shape = RoundedCornerShape(
                topStart = 20.dp,
                topEnd = 20.dp,
                bottomStart = if (isUser) 20.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 20.dp
            ),
            border = if (!isUser) androidx.compose.foundation.BorderStroke(1.dp, BorderPolish) else null,
            modifier = Modifier.widthIn(max = maxBubbleWidth)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = message.message,
                    fontSize = 12.sp,
                    color = if (isUser) Color.White else TextDark,
                    lineHeight = 17.sp
                )

                if (message.groundedSources.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = BorderPolish)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "📚 Grounded Authority Sources:",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = SageAccent
                    )
                    message.groundedSources.forEach { src ->
                        Text(
                            text = "• $src",
                            fontSize = 9.sp,
                            color = TextMuted
                        )
                    }
                }

                if (message.suggestedServiceId != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        onClick = { onOpenService(message.suggestedServiceId) },
                        color = SageContainer,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "View Verified Document Checklist →",
                                fontSize = 10.sp,
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
