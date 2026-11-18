package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.UserRole
import com.example.data.remote.GoogleSignInButton
import com.example.ui.components.AdaptiveGridRow
import com.example.ui.components.rememberQEaseLayoutMetrics
import com.example.ui.theme.*
import com.google.firebase.auth.FirebaseUser

private data class WelcomeCapabilityItem(
    val icon: ImageVector,
    val title: String,
    val subtitle: String
)

private data class WelcomePortalItem(
    val id: String,
    val title: String,
    val description: String,
    val badge: String,
    val badgeColor: Color,
    val badgeTextColor: Color,
    val icon: ImageVector,
    val onClick: () -> Unit
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WelcomeScreen(
    onStartCitizenLogin: () -> Unit,
    onSelectRoleLogin: (UserRole) -> Unit,
    firebaseUser: FirebaseUser? = null,
    googleAuthError: String? = null,
    onGoogleAuthSuccess: () -> Unit = {},
    onGoogleAuthError: (String) -> Unit = {},
    onSignOutGoogle: () -> Unit = {}
) {
    val scrollState = rememberScrollState()

    val capabilities = remember {
        listOf(
            WelcomeCapabilityItem(
                icon = Icons.Default.GpsFixed,
                title = "Where is My Turn",
                subtitle = "Station-by-station live tracking radar"
            ),
            WelcomeCapabilityItem(
                icon = Icons.Default.CheckCircle,
                title = "Grounded Docs",
                subtitle = "Official checklists & zero rejection"
            ),
            WelcomeCapabilityItem(
                icon = Icons.Default.Group,
                title = "Crowd Forecasts",
                subtitle = "Deterministic wait-time estimators"
            ),
            WelcomeCapabilityItem(
                icon = Icons.Default.Shield,
                title = "Staff Desk RBAC",
                subtitle = "Role-gated counter management"
            )
        )
    }

    val portals = remember(onSelectRoleLogin) {
        listOf(
            WelcomePortalItem(
                id = "customer",
                title = "Citizen Portal",
                description = "Sign in with your 10-digit Mobile + OTP to explore centres, join virtual queues & track turns.",
                badge = "Mobile + OTP",
                badgeColor = SageContainer,
                badgeTextColor = DeepForestDark,
                icon = Icons.Default.Person,
                onClick = { onSelectRoleLogin(UserRole.CUSTOMER) }
            ),
            WelcomePortalItem(
                id = "staff",
                title = "Staff Counter Desk",
                description = "Call waiting tokens, verify citizen documents, mark serving/complete & manage counters.",
                badge = "Staff PIN: 1234",
                badgeColor = PolishAmberBg,
                badgeTextColor = PolishAmber,
                icon = Icons.Default.Badge,
                onClick = { onSelectRoleLogin(UserRole.STAFF) }
            ),
            WelcomePortalItem(
                id = "admin",
                title = "Admin & Analytics Console",
                description = "Service toggles, system notices, live audit trail, security configurations & telemetry.",
                badge = "Admin PIN: 8899",
                badgeColor = PolishGreenBg,
                badgeTextColor = PolishGreen,
                icon = Icons.Default.AdminPanelSettings,
                onClick = { onSelectRoleLogin(UserRole.ADMIN) }
            )
        )
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("welcome_screen"),
        color = BackgroundPolish
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            contentAlignment = Alignment.TopCenter
        ) {
            val metrics = rememberQEaseLayoutMetrics(maxWidth)
            val capabilityColumns = when {
                metrics.effectiveWidthDp < 280f -> 1
                metrics.isExpanded && metrics.effectiveWidthDp >= 720f -> 4
                else -> 2
            }
            val portalColumns = if (metrics.isWide && metrics.effectiveWidthDp >= 560f) 2 else 1

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = metrics.horizontalPadding, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 960.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(8.dp))

                    // Brand Header Hero
                    Surface(
                        shape = RoundedCornerShape(28.dp),
                        color = DeepForestDark,
                        border = androidx.compose.foundation.BorderStroke(1.dp, DeepForestMedium),
                        shadowElevation = 10.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DeepForestDark)
                                .padding(if (metrics.isCompact) 22.dp else 30.dp)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = Color.White.copy(alpha = 0.1f),
                                    shadowElevation = 8.dp,
                                    modifier = Modifier
                                        .size(72.dp)
                                        .border(1.5.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Image(
                                            painter = painterResource(id = R.drawable.ic_qease_minimal_logo),
                                            contentDescription = "QEase Minimalist Logo",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(RoundedCornerShape(20.dp))
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = "QEase",
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    letterSpacing = (-0.5).sp
                                )

                                Text(
                                    text = "Service & Queue Intelligence",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = SageContainer.copy(alpha = 0.9f),
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = "Say goodbye to chaotic waiting lines. Real-time token tracking, verified document checklists, crowd forecasts, and secure staff desk operations.",
                                    fontSize = 13.sp,
                                    color = Color.White.copy(alpha = 0.85f),
                                    textAlign = TextAlign.Center,
                                    lineHeight = 19.sp,
                                    modifier = Modifier.widthIn(max = 640.dp)
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Surface(
                                        color = Color.White.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(20.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Bolt,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Text(
                                                text = "Real-Time DB Sync",
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    Surface(
                                        color = Color.White.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(20.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Shield,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Text(
                                                text = "RBAC Guarded",
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Feature Highlights Grid
                    Text(
                        text = "Key Capabilities",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepForestDark,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        capabilities.chunked(capabilityColumns).forEach { rowItems ->
                            AdaptiveGridRow(
                                items = rowItems,
                                columns = capabilityColumns,
                                horizontalSpacing = 12.dp
                            ) { item, itemModifier ->
                                WelcomeFeatureCard(
                                    icon = item.icon,
                                    title = item.title,
                                    subtitle = item.subtitle,
                                    modifier = itemModifier
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(22.dp))

                    // Google Cloud Sync Account Card
                    Surface(
                        color = SurfaceGlass,
                        shape = RoundedCornerShape(18.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (firebaseUser != null) PolishGreen.copy(alpha = 0.35f) else BorderPolish
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("google_cloud_sync_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Surface(
                                    color = if (firebaseUser != null) PolishGreenBg else SageContainer,
                                    shape = CircleShape,
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = if (firebaseUser != null) Icons.Default.CloudDone else Icons.Default.CloudSync,
                                            contentDescription = "Cloud Sync",
                                            tint = if (firebaseUser != null) PolishGreen else DeepForestDark,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (firebaseUser != null) "Google Cloud Sync Connected" else "Connect Google Account",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = DeepForestDark
                                    )
                                    Text(
                                        text = if (firebaseUser != null) {
                                            val name = firebaseUser.displayName?.takeIf { it.isNotBlank() } ?: "Verified Google User"
                                            val mail = firebaseUser.email ?: ""
                                            if (mail.isNotBlank()) "$name • $mail" else name
                                        } else {
                                            "Sign in with Google to synchronize live Firestore queues across devices."
                                        },
                                        fontSize = 11.sp,
                                        color = TextMuted,
                                        lineHeight = 15.sp
                                    )
                                }
                            }

                            if (firebaseUser == null) {
                                GoogleSignInButton(
                                    onAuthSuccess = onGoogleAuthSuccess,
                                    onAuthError = onGoogleAuthError,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else {
                                OutlinedButton(
                                    onClick = onSignOutGoogle,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                        .testTag("google_signout_button")
                                ) {
                                    Text(
                                        text = "Sign Out Google Account",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = DeepForestDark
                                    )
                                }
                            }

                            if (!googleAuthError.isNullOrBlank()) {
                                Text(
                                    text = googleAuthError,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = PolishRed
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(22.dp))

                    // Role Access Selector
                    Text(
                        text = "Select Portal to Continue",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepForestDark,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        portals.chunked(portalColumns).forEach { rowPortals ->
                            AdaptiveGridRow(
                                items = rowPortals,
                                columns = portalColumns,
                                horizontalSpacing = 12.dp
                            ) { portal, portalModifier ->
                                RolePortalCard(
                                    title = portal.title,
                                    description = portal.description,
                                    badge = portal.badge,
                                    badgeColor = portal.badgeColor,
                                    badgeTextColor = portal.badgeTextColor,
                                    icon = portal.icon,
                                    onClick = portal.onClick,
                                    modifier = portalModifier.testTag("portal_card_${portal.id}")
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Surface(
                        color = SurfaceContainerNav,
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderPolish),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = DeepForestDark,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Strict RBAC session isolation: Staff and Admin portals are only accessible here. Log out first to switch roles.",
                                fontSize = 11.sp,
                                color = TextMuted,
                                lineHeight = 15.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Primary Citizen Login CTA
                    Button(
                        onClick = onStartCitizenLogin,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DeepForestDark),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 520.dp)
                            .heightIn(min = 52.dp)
                            .testTag("get_started_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Citizen Sign In (Mobile + OTP)",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun WelcomeFeatureCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = SurfaceGlass,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderPolish),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Surface(
                color = SageContainer,
                shape = CircleShape,
                modifier = Modifier.size(34.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = DeepForestDark,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = DeepForestDark
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = TextMuted,
                lineHeight = 15.sp
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RolePortalCard(
    title: String,
    description: String,
    badge: String,
    badgeColor: Color,
    badgeTextColor: Color,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = SurfaceGlass,
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderPolish),
        shadowElevation = 2.dp,
        modifier = modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = SageContainer,
                shape = CircleShape,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = DeepForestDark,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepForestDark,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                    Surface(
                        color = badgeColor,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.align(Alignment.CenterVertically)
                    ) {
                        Text(
                            text = badge,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = badgeTextColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = TextMuted,
                    lineHeight = 16.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

