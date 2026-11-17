package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserRole
import com.example.ui.components.AuthRbacDialog
import com.example.ui.components.ResponsiveDialogShell
import com.example.ui.components.rememberQEaseLayoutMetrics
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainNavTab
import com.example.ui.viewmodel.QEaseViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: QEaseViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        )
        setContent {
            MyApplicationTheme {
                QEaseMainApp(viewModel)
            }
        }
    }
}

private data class NavDestinationItem(
    val tab: MainNavTab,
    val icon: ImageVector,
    val label: String,
    val hasBadge: Boolean = false
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QEaseMainApp(viewModel: QEaseViewModel) {
    val currentTab by viewModel.currentTab.collectAsState()
    val currentRole by viewModel.currentRole.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val isAuthenticated by viewModel.isAuthenticated.collectAsState()
    val feedbackMessage by viewModel.userFeedbackMessage.collectAsState()
    val activeToken by viewModel.activeUserToken.collectAsState()
    val showWelcome by viewModel.showWelcomeScreen.collectAsState()
    val showAuthDialog by viewModel.showAuthDialog.collectAsState()
    val showLogoutConfirm by viewModel.showLogoutConfirm.collectAsState()
    val authTargetRole by viewModel.authTargetRole.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(feedbackMessage) {
        feedbackMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearFeedbackMessage()
        }
    }

    if (!isAuthenticated || showWelcome) {
        WelcomeScreen(
            onStartCitizenLogin = { viewModel.openAuthDialog(UserRole.CUSTOMER) },
            onSelectRoleLogin = { targetRole ->
                viewModel.openAuthDialog(targetRole)
            }
        )
    } else {
        val navDestinations = remember(currentRole, activeToken) {
            when (currentRole) {
                UserRole.CUSTOMER -> listOf(
                    NavDestinationItem(MainNavTab.EXPLORE, Icons.Default.Home, "Home"),
                    NavDestinationItem(MainNavTab.CENTRES, Icons.Default.LocationOn, "Centres"),
                    NavDestinationItem(MainNavTab.LIVE_TRACKER, Icons.Default.GpsFixed, "Live Radar", activeToken != null),
                    NavDestinationItem(MainNavTab.MY_QUEUE, Icons.Default.ConfirmationNumber, "Queue", activeToken != null),
                    NavDestinationItem(MainNavTab.REVIEWS_FEEDBACK, Icons.Default.RateReview, "Reviews"),
                    NavDestinationItem(MainNavTab.AI_ASSISTANT, Icons.Default.AutoAwesome, "AI Help")
                )
                UserRole.STAFF -> listOf(
                    NavDestinationItem(MainNavTab.STAFF_DESK, Icons.Default.DesktopWindows, "Desk"),
                    NavDestinationItem(MainNavTab.LIVE_TRACKER, Icons.Default.GpsFixed, "Live Radar"),
                    NavDestinationItem(MainNavTab.CENTRES, Icons.Default.LocationOn, "Centres"),
                    NavDestinationItem(MainNavTab.REVIEWS_FEEDBACK, Icons.Default.RateReview, "Reviews"),
                    NavDestinationItem(MainNavTab.AI_ASSISTANT, Icons.Default.AutoAwesome, "AI Help")
                )
                UserRole.ADMIN -> listOf(
                    NavDestinationItem(MainNavTab.ADMIN_PANEL, Icons.Default.AdminPanelSettings, "Admin"),
                    NavDestinationItem(MainNavTab.SERVICES, Icons.Default.Description, "Services"),
                    NavDestinationItem(MainNavTab.CENTRES, Icons.Default.LocationOn, "Centres"),
                    NavDestinationItem(MainNavTab.REVIEWS_FEEDBACK, Icons.Default.RateReview, "Feedback"),
                    NavDestinationItem(MainNavTab.LIVE_TRACKER, Icons.Default.GpsFixed, "Live Radar")
                )
            }
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundPolish)
        ) {
            val metrics = rememberQEaseLayoutMetrics(maxWidth)
            val useNavigationRail = metrics.isWide

            Row(modifier = Modifier.fillMaxSize()) {
                if (useNavigationRail) {
                    Surface(
                        color = SurfaceContainerNav,
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderPolish),
                        shadowElevation = 4.dp,
                        modifier = Modifier
                            .fillMaxHeight()
                            .widthIn(min = 84.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .fillMaxHeight()
                                .statusBarsPadding()
                                .navigationBarsPadding()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 8.dp, vertical = 12.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = SageContainer,
                                modifier = Modifier
                                    .size(40.dp)
                                    .border(1.dp, PolishGreen.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_qease_minimal_logo),
                                    contentDescription = "QEase Logo",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(12.dp))
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            navDestinations.forEach { dest ->
                                val isSelected = currentTab == dest.tab
                                NavigationRailItem(
                                    selected = isSelected,
                                    onClick = { viewModel.setNavTab(dest.tab) },
                                    icon = {
                                        Box {
                                            Icon(
                                                imageVector = dest.icon,
                                                contentDescription = dest.label,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            if (dest.hasBadge) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(7.dp)
                                                        .background(PolishGreen, CircleShape)
                                                        .align(Alignment.TopEnd)
                                                )
                                            }
                                        }
                                    },
                                    label = {
                                        Text(
                                            text = dest.label,
                                            fontSize = 10.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            textAlign = TextAlign.Center,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    colors = NavigationRailItemDefaults.colors(
                                        selectedIconColor = DeepForestDark,
                                        selectedTextColor = DeepForestDark,
                                        indicatorColor = SageContainer,
                                        unselectedIconColor = TextDark.copy(alpha = 0.55f),
                                        unselectedTextColor = TextDark.copy(alpha = 0.55f)
                                    ),
                                    modifier = Modifier
                                        .testTag("nav_tab_${dest.label.lowercase().replace(" ", "_")}")
                                )
                            }

                            Spacer(modifier = Modifier.weight(1f, fill = true))

                            NavigationRailItem(
                                selected = false,
                                onClick = { viewModel.requestLogoutConfirmation() },
                                icon = {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Logout,
                                        contentDescription = "Logout",
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        text = "Logout",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                },
                                colors = NavigationRailItemDefaults.colors(
                                    unselectedIconColor = PolishRed,
                                    unselectedTextColor = PolishRed
                                ),
                                modifier = Modifier.testTag("rail_logout_button")
                            )
                        }
                    }
                }

                Scaffold(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    containerColor = BackgroundPolish,
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    bottomBar = {
                        if (!useNavigationRail) {
                            Surface(
                                color = SurfaceContainerNav,
                                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, BorderPolish),
                                shadowElevation = 8.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                val useScrollableNav = metrics.isScaledDisplay || metrics.isTightHorizontal
                                val rowModifier = Modifier
                                    .fillMaxWidth()
                                    .navigationBarsPadding()
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                                    .let { if (useScrollableNav) it.horizontalScroll(rememberScrollState()) else it }

                                Row(
                                    modifier = rowModifier,
                                    horizontalArrangement = if (useScrollableNav) Arrangement.spacedBy(6.dp) else Arrangement.SpaceAround,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    navDestinations.forEach { dest ->
                                        NavIconItem(
                                            icon = dest.icon,
                                            label = dest.label,
                                            isSelected = currentTab == dest.tab,
                                            hasBadge = dest.hasBadge,
                                            onClick = { viewModel.setNavTab(dest.tab) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    val bottomSafePadding = if (useNavigationRail) {
                        Modifier.navigationBarsPadding()
                    } else {
                        Modifier
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .padding(innerPadding)
                            .then(bottomSafePadding)
                    ) {
                        when (currentTab) {
                            MainNavTab.EXPLORE -> CustomerHomeScreen(viewModel)
                            MainNavTab.CENTRES -> CentresScreen(viewModel)
                            MainNavTab.SERVICES -> ServicesScreen(viewModel)
                            MainNavTab.MY_QUEUE -> MyQueueScreen(viewModel)
                            MainNavTab.LIVE_TRACKER -> WhereIsMyTurnScreen(
                                viewModel = viewModel,
                                onNavigateCentres = { viewModel.setNavTab(MainNavTab.CENTRES) }
                            )
                            MainNavTab.REVIEWS_FEEDBACK -> ReviewsFeedbackScreen(viewModel)
                            MainNavTab.AI_ASSISTANT -> AiAssistantScreen(viewModel)
                            MainNavTab.STAFF_DESK -> StaffDeskScreen(viewModel)
                            MainNavTab.ADMIN_PANEL -> AdminPanelScreen(viewModel)
                            MainNavTab.SAVED_CENTRES -> CentresScreen(viewModel)
                        }
                    }
                }
            }
        }
    }

    // Role-locked Login Modal (only reachable from Welcome/Login screen)
    if (showAuthDialog && !isAuthenticated) {
        AuthRbacDialog(
            targetRole = authTargetRole,
            onDismiss = { viewModel.dismissAuthDialog() },
            onRequestCitizenOtp = { mobile, fullName -> viewModel.requestCitizenOtp(mobile, fullName) },
            onVerifyCitizenOtp = { mobile, otp, name -> viewModel.verifyCitizenOtp(mobile, otp, name) },
            onAuthenticateOfficer = { email, pin, role ->
                viewModel.authenticateUser(email, pin, role)
            }
        )
    }

    // Logout Confirmation Dialog
    if (showLogoutConfirm) {
        LogoutConfirmationDialog(
            userName = currentUser.name,
            role = currentRole,
            onDismiss = { viewModel.dismissLogoutConfirmation() },
            onConfirmLogout = { viewModel.confirmLogout() }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LogoutConfirmationDialog(
    userName: String,
    role: UserRole,
    onDismiss: () -> Unit,
    onConfirmLogout: () -> Unit
) {
    val roleDisplay = when (role) {
        UserRole.CUSTOMER -> "Citizen Portal"
        UserRole.STAFF -> "Staff Counter Desk"
        UserRole.ADMIN -> "System Admin Console"
    }

    ResponsiveDialogShell(
        onDismissRequest = onDismiss,
        maxWidth = 440.dp,
        modifier = Modifier.testTag("logout_confirm_dialog")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                color = PolishRedBg,
                shape = CircleShape,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = null,
                        tint = PolishRed,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Column {
                Text(
                    text = "Sign Out of QEase?",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = DeepForestDark
                )
                Text(
                    text = "$userName • $roleDisplay",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "To switch between Citizen, Staff Desk, or Admin accounts, you must sign out first and authenticate from the login portal.",
            fontSize = 12.sp,
            color = TextMuted,
            lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(20.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .widthIn(min = 120.dp)
                    .heightIn(min = 48.dp)
            ) {
                Text("Stay Signed In", color = DeepForestDark, fontWeight = FontWeight.SemiBold)
            }

            Button(
                onClick = onConfirmLogout,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PolishRed),
                modifier = Modifier
                    .weight(1f)
                    .widthIn(min = 120.dp)
                    .heightIn(min = 48.dp)
                    .testTag("confirm_logout_button")
            ) {
                Text("Confirm Logout", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun NavIconItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    hasBadge: Boolean = false,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .defaultMinSize(minWidth = 52.dp, minHeight = 48.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 4.dp)
            .testTag("nav_tab_${label.lowercase().replace(" ", "_")}")
    ) {
        Box(
            modifier = Modifier
                .background(
                    if (isSelected) SageContainer else Color.Transparent,
                    RoundedCornerShape(14.dp)
                )
                .padding(horizontal = 10.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) DeepForestDark else TextDark.copy(alpha = 0.45f),
                modifier = Modifier.size(19.dp)
            )
            if (hasBadge) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(PolishGreen, CircleShape)
                        .align(Alignment.TopEnd)
                )
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) DeepForestDark else TextDark.copy(alpha = 0.45f),
            maxLines = 1,
            softWrap = false
        )
    }
}

