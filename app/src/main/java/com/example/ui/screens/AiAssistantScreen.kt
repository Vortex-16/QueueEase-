package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.data.model.AiActionType
import com.example.data.model.AiWorkflowAction
import com.example.data.model.ChatMessage
import com.example.data.model.UserRole
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
    val currentRole by viewModel.currentRole.collectAsState()

    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    val (headerTitle, headerSubtitle) = when (currentRole) {
        UserRole.CUSTOMER -> "Service Intelligence AI" to "Grounded in Official Guidelines"
        UserRole.STAFF -> "Counter Desk AI Copilot" to "Assigned Desk SOP & Proposal Assistant"
        UserRole.ADMIN -> "Governance & Audit AI" to "Live Proposals & Bottleneck Auditor"
    }

    val thinkingText = when (currentRole) {
        UserRole.CUSTOMER -> "Verifying document rules & official criteria..."
        UserRole.STAFF -> "Checking desk queue load & SOP rules..."
        UserRole.ADMIN -> "Auditing pending proposals & centre telemetry..."
    }

    val inputPlaceholder = when (currentRole) {
        UserRole.CUSTOMER -> "Ask about proofs, fees, procedures..."
        UserRole.STAFF -> "Ask Copilot to draft proposals or check SOPs..."
        UserRole.ADMIN -> "Ask AI to audit proposals, bottlenecks, or policies..."
    }

    val quickQuestions = remember(currentRole) {
        when (currentRole) {
            UserRole.CUSTOMER -> listOf(
                "Required documents for Passport renewal?",
                "What is the fee for Driving License renewal?",
                "How long is the wait at Central Passport Seva Kendra?",
                "Aadhaar biometric update address proofs?"
            )
            UserRole.STAFF -> listOf(
                "Draft a service duration & caution update proposal",
                "Suggest a required document rule clarification",
                "Summarize current crowd reports & desk alerts",
                "How should I handle an unverified crowd report?"
            )
            UserRole.ADMIN -> listOf(
                "Audit pending Staff proposals awaiting approval",
                "Which centre currently has the highest bottleneck?",
                "Review maintenance mode & no-show timeout policy",
                "Summarize recent system audit trail activity"
            )
        }
    }

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
                                .size(38.dp)
                                .background(SageContainer, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when (currentRole) {
                                    UserRole.CUSTOMER -> Icons.Default.AutoAwesome
                                    UserRole.STAFF -> Icons.Default.SupportAgent
                                    UserRole.ADMIN -> Icons.Default.Policy
                                },
                                contentDescription = null,
                                tint = DeepForestDark,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = headerTitle,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = DeepForestDark
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                LivePulseIndicator()
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = headerSubtitle,
                                    fontSize = 10.sp,
                                    color = TextMuted
                                )
                            }
                        }
                    }
                },
                actions = {
                    if (currentRole != UserRole.CUSTOMER) {
                        IconButton(
                            onClick = { viewModel.requestLogoutConfirmation() },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("ai_logout_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Logout,
                                contentDescription = "Sign Out",
                                tint = PolishRed
                            )
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
            val bubbleMaxWidth = (metrics.availableWidth.coerceAtMost(900.dp) * 0.86f).coerceAtMost(580.dp)

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
                    items(chatMessages, key = { it.id }) { msg ->
                        ChatBubblePolish(
                            message = msg,
                            maxBubbleWidth = bubbleMaxWidth,
                            onExecuteAction = { action ->
                                viewModel.executeAiWorkflowAction(action)
                            },
                            onOpenService = { serviceId ->
                                if (currentRole == UserRole.CUSTOMER) {
                                    viewModel.services.value.find { it.id == serviceId }?.let {
                                        viewModel.selectService(it)
                                        viewModel.setNavTab(MainNavTab.SERVICES)
                                    }
                                }
                            }
                        )
                    }

                    if (isAiThinking) {
                        item {
                            Surface(
                                color = SurfacePolish,
                                shape = RoundedCornerShape(20.dp),
                                border = BorderStroke(1.dp, BorderPolish),
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
                                        text = thinkingText,
                                        fontSize = 12.sp,
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
                            border = BorderStroke(1.dp, BorderPolish),
                            modifier = Modifier.heightIn(min = 38.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
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
                    border = BorderStroke(1.dp, BorderMedium),
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
                            placeholder = {
                                Text(
                                    text = inputPlaceholder,
                                    fontSize = 13.sp,
                                    color = TextSubtle
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("ai_chat_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextDark,
                                unfocusedTextColor = TextDark,
                                cursorColor = DeepForestDark,
                                focusedPlaceholderColor = TextSubtle,
                                unfocusedPlaceholderColor = TextSubtle,
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChatBubblePolish(
    message: ChatMessage,
    onOpenService: (String) -> Unit,
    onExecuteAction: ((AiWorkflowAction) -> Unit)? = null,
    modifier: Modifier = Modifier,
    maxBubbleWidth: Dp = 360.dp
) {
    val isUser = message.isUser

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        if (!isUser) {
            Surface(
                color = SageContainer,
                shape = CircleShape,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(30.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = DeepForestDark,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Surface(
            color = if (isUser) DeepForestDark else SurfacePolish,
            shape = RoundedCornerShape(
                topStart = 20.dp,
                topEnd = 20.dp,
                bottomStart = if (isUser) 20.dp else 6.dp,
                bottomEnd = if (isUser) 6.dp else 20.dp
            ),
            border = if (!isUser) BorderStroke(1.dp, BorderPolish) else null,
            shadowElevation = if (!isUser) 2.dp else 1.dp,
            modifier = Modifier.widthIn(max = maxBubbleWidth)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!isUser) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            color = SageContainerLight,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VerifiedUser,
                                    contentDescription = null,
                                    tint = PolishGreen,
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    text = "VERIFIED RESPONSE",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.6.sp,
                                    color = DeepForestDark
                                )
                            }
                        }
                    }
                }

                FormattedChatMessageContent(
                    rawText = message.message,
                    isUser = isUser
                )

                if (message.groundedSources.isNotEmpty()) {
                    HorizontalDivider(color = BorderPolish)
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(
                            text = "Grounded Authority Sources",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = SageAccent
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            message.groundedSources.forEach { src ->
                                Surface(
                                    color = SurfaceVariantPolish,
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, BorderPolish)
                                ) {
                                    Text(
                                        text = src,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = TextMuted,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                val action = message.workflowAction
                if (action != null && onExecuteAction != null) {
                    Surface(
                        onClick = { onExecuteAction(action) },
                        color = SageContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .heightIn(min = 42.dp)
                            .testTag("ai_workflow_action_${action.type.name.lowercase()}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = workflowActionIcon(action.type),
                                contentDescription = null,
                                tint = DeepForestDark,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = action.label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = DeepForestDark
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = DeepForestDark,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                } else if (message.suggestedServiceId != null) {
                    Surface(
                        onClick = { onOpenService(message.suggestedServiceId) },
                        color = SageContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.heightIn(min = 38.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Checklist,
                                contentDescription = null,
                                tint = DeepForestDark,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "View Verified Document Checklist",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = DeepForestDark
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = DeepForestDark,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun workflowActionIcon(type: AiActionType): ImageVector = when (type) {
    AiActionType.OPEN_SERVICE_CHECKLIST -> Icons.Default.Checklist
    AiActionType.PREFILL_STAFF_SERVICE_PROPOSAL -> Icons.Default.EditNote
    AiActionType.PREFILL_STAFF_DOC_PROPOSAL -> Icons.Default.PostAdd
    AiActionType.OPEN_STAFF_ALERTS -> Icons.Default.NotificationsActive
    AiActionType.VERIFY_LATEST_CROWD_REPORT -> Icons.Default.Verified
    AiActionType.OPEN_STAFF_DESK -> Icons.Default.DesktopWindows
    AiActionType.OPEN_ADMIN_APPROVALS -> Icons.AutoMirrored.Filled.FactCheck
    AiActionType.OPEN_ADMIN_OPS_STAFF -> Icons.Default.Groups
    AiActionType.OPEN_ADMIN_OPS_CENTRES -> Icons.Default.Domain
    AiActionType.OPEN_ADMIN_OPS_SETTINGS -> Icons.Default.Settings
    AiActionType.OPEN_ADMIN_ANALYTICS -> Icons.Default.Analytics
}

private sealed class ChatBlock {
    data class Heading(val text: String) : ChatBlock()
    data class SectionLabel(val title: String, val body: String) : ChatBlock()
    data class BulletItem(val text: String) : ChatBlock()
    data class NumberedItem(val number: String, val text: String) : ChatBlock()
    data class Paragraph(val text: String) : ChatBlock()
}

@Composable
private fun FormattedChatMessageContent(
    rawText: String,
    isUser: Boolean
) {
    val blocks = remember(rawText) { parseFormattedBlocks(rawText) }
    val primaryTextColor = if (isUser) Color.White else TextDark
    val strongColor = if (isUser) Color.White else DeepForestDark
    val codeBgColor = if (isUser) Color.White.copy(alpha = 0.16f) else SageContainerLight

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        blocks.forEach { block ->
            when (block) {
                is ChatBlock.Heading -> {
                    Text(
                        text = buildInlineRichText(
                            raw = block.text,
                            baseColor = strongColor,
                            strongColor = strongColor,
                            codeBg = codeBgColor
                        ),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = strongColor,
                        lineHeight = 18.sp
                    )
                }

                is ChatBlock.SectionLabel -> {
                    Surface(
                        color = if (isUser) Color.White.copy(alpha = 0.12f) else SurfaceVariantPolish,
                        shape = RoundedCornerShape(10.dp),
                        border = if (!isUser) BorderStroke(1.dp, BorderPolish) else null,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = stripRawMarkdownSymbols(block.title),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isUser) Color.White else SageAccent
                            )
                            if (block.body.isNotBlank()) {
                                Text(
                                    text = buildInlineRichText(
                                        raw = block.body,
                                        baseColor = primaryTextColor,
                                        strongColor = strongColor,
                                        codeBg = codeBgColor
                                    ),
                                    fontSize = 12.sp,
                                    color = primaryTextColor,
                                    lineHeight = 17.sp
                                )
                            }
                        }
                    }
                }

                is ChatBlock.BulletItem -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 6.dp)
                                .size(6.dp)
                                .background(
                                    color = if (isUser) Color.White else SageAccent,
                                    shape = CircleShape
                                )
                        )
                        Text(
                            text = buildInlineRichText(
                                raw = block.text,
                                baseColor = primaryTextColor,
                                strongColor = strongColor,
                                codeBg = codeBgColor
                            ),
                            fontSize = 12.sp,
                            color = primaryTextColor,
                            lineHeight = 17.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                is ChatBlock.NumberedItem -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Surface(
                            color = if (isUser) Color.White.copy(alpha = 0.2f) else SageContainer,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .padding(top = 1.dp)
                                .defaultMinSize(minWidth = 20.dp, minHeight = 20.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = block.number,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isUser) Color.White else DeepForestDark
                                )
                            }
                        }
                        Text(
                            text = buildInlineRichText(
                                raw = block.text,
                                baseColor = primaryTextColor,
                                strongColor = strongColor,
                                codeBg = codeBgColor
                            ),
                            fontSize = 12.sp,
                            color = primaryTextColor,
                            lineHeight = 17.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                is ChatBlock.Paragraph -> {
                    Text(
                        text = buildInlineRichText(
                            raw = block.text,
                            baseColor = primaryTextColor,
                            strongColor = strongColor,
                            codeBg = codeBgColor
                        ),
                        fontSize = 12.sp,
                        color = primaryTextColor,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

private val HEADING_REGEX = Regex("^#{1,6}\\s+(.+)$")
private val BULLET_REGEX = Regex("^[\\*\\-•]\\s+(.+)$")
private val NUMBERED_REGEX = Regex("^(\\d+)[.)]\\s+(.+)$")
private val SECTION_LINE_REGEX = Regex("^(?:\\p{So}\\s*)?(?:\\*\\*|__)?([A-Za-z][A-Za-z0-9 &/'-]{2,36})(?:\\*\\*|__)?:\\s*(.*)$")
private val INLINE_TOKEN_REGEX = Regex("(\\*\\*[^*\\n]+\\*\\*|__[^_\\n]+__|\\*[^*\\n]+\\*|_[^_\\n]+_|`[^`\\n]+`)")

private fun parseFormattedBlocks(rawText: String): List<ChatBlock> {
    val result = mutableListOf<ChatBlock>()
    val lines = rawText.replace("\r\n", "\n").lines()

    for (rawLine in lines) {
        val line = rawLine.trim()
        if (line.isBlank()) continue

        val headingMatch = HEADING_REGEX.matchEntire(line)
        if (headingMatch != null) {
            result += ChatBlock.Heading(headingMatch.groupValues[1].trim())
            continue
        }

        val bulletMatch = BULLET_REGEX.matchEntire(line)
        if (bulletMatch != null) {
            result += ChatBlock.BulletItem(bulletMatch.groupValues[1].trim())
            continue
        }

        val numberedMatch = NUMBERED_REGEX.matchEntire(line)
        if (numberedMatch != null) {
            result += ChatBlock.NumberedItem(
                number = numberedMatch.groupValues[1],
                text = numberedMatch.groupValues[2].trim()
            )
            continue
        }

        val sectionMatch = SECTION_LINE_REGEX.matchEntire(line)
        if (sectionMatch != null && !line.startsWith("http", ignoreCase = true)) {
            val title = sectionMatch.groupValues[1].trim()
            val body = sectionMatch.groupValues[2].trim()
            // Treat short titled lines (e.g. "Key Requirements:", "Official Fee: ...") as styled section blocks
            if (title.split(" ").size <= 4) {
                result += ChatBlock.SectionLabel(title = title, body = body)
                continue
            }
        }

        // Plain bold line acting as standalone subheading (e.g. "**Key Requirements**")
        if ((line.startsWith("**") && line.endsWith("**") && line.length > 4) ||
            (line.startsWith("__") && line.endsWith("__") && line.length > 4)
        ) {
            result += ChatBlock.Heading(line.substring(2, line.length - 2).trim())
            continue
        }

        result += ChatBlock.Paragraph(line)
    }

    return result
}

private fun buildInlineRichText(
    raw: String,
    baseColor: Color,
    strongColor: Color,
    codeBg: Color
): AnnotatedString = buildAnnotatedString {
    var cursor = 0
    for (match in INLINE_TOKEN_REGEX.findAll(raw)) {
        if (match.range.first > cursor) {
            val plain = stripRawMarkdownSymbols(raw.substring(cursor, match.range.first))
            withStyle(SpanStyle(color = baseColor)) { append(plain) }
        }

        val token = match.value
        when {
            (token.startsWith("**") && token.endsWith("**") && token.length > 4) ||
                (token.startsWith("__") && token.endsWith("__") && token.length > 4) -> {
                val inner = stripRawMarkdownSymbols(token.substring(2, token.length - 2))
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = strongColor)) {
                    append(inner)
                }
            }
            (token.startsWith("*") && token.endsWith("*") && token.length > 2) ||
                (token.startsWith("_") && token.endsWith("_") && token.length > 2) -> {
                val inner = stripRawMarkdownSymbols(token.substring(1, token.length - 1))
                withStyle(SpanStyle(fontStyle = FontStyle.Italic, color = baseColor)) {
                    append(inner)
                }
            }
            token.startsWith("`") && token.endsWith("`") && token.length > 2 -> {
                val inner = token.substring(1, token.length - 1)
                withStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                        color = strongColor,
                        background = codeBg
                    )
                ) {
                    append(" $inner ")
                }
            }
            else -> {
                withStyle(SpanStyle(color = baseColor)) {
                    append(stripRawMarkdownSymbols(token))
                }
            }
        }
        cursor = match.range.last + 1
    }

    if (cursor < raw.length) {
        val tail = stripRawMarkdownSymbols(raw.substring(cursor))
        withStyle(SpanStyle(color = baseColor)) { append(tail) }
    }
}

private fun stripRawMarkdownSymbols(text: String): String {
    return text
        .replace("**", "")
        .replace("__", "")
        .replace("*", "")
        .replace("`", "")
}
