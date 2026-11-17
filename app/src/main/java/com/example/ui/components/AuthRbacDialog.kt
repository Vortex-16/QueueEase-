package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserAccount
import com.example.data.model.UserRole
import com.example.ui.theme.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AuthRbacDialog(
    targetRole: UserRole,
    onDismiss: () -> Unit,
    onRequestCitizenOtp: (mobile: String, fullName: String) -> Result<String>,
    onVerifyCitizenOtp: (mobile: String, otp: String, fullName: String) -> Result<UserAccount>,
    onAuthenticateOfficer: (email: String, pin: String, role: UserRole) -> Result<UserAccount>
) {
    ResponsiveDialogShell(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("auth_rbac_dialog")
    ) {
        // Locked Role Header (no cross-role switcher inside modal)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = CircleShape,
                    color = SageContainer,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = when (targetRole) {
                                UserRole.CUSTOMER -> Icons.Default.VerifiedUser
                                UserRole.STAFF -> Icons.Default.Badge
                                UserRole.ADMIN -> Icons.Default.Security
                            },
                            contentDescription = null,
                            tint = DeepForestDark,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Column {
                    Text(
                        text = when (targetRole) {
                            UserRole.CUSTOMER -> "Citizen e-Seva Sign In"
                            UserRole.STAFF -> "Staff Counter Desk Login"
                            UserRole.ADMIN -> "System Admin Console"
                        },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepForestDark
                    )
                    Text(
                        text = when (targetRole) {
                            UserRole.CUSTOMER -> "Mobile + 6-Digit OTP Verification"
                            UserRole.STAFF -> "Officer Government ID & Desk PIN"
                            UserRole.ADMIN -> "Master Admin Key Verification"
                        },
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = TextMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Role-locked indicator pill
        Surface(
            color = when (targetRole) {
                UserRole.CUSTOMER -> SageContainer
                UserRole.STAFF -> PolishAmberBg
                UserRole.ADMIN -> PolishGreenBg
            },
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = DeepForestDark,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = when (targetRole) {
                        UserRole.CUSTOMER -> "Portal Locked: Citizen Public Services"
                        UserRole.STAFF -> "Portal Locked: Staff Desk RBAC"
                        UserRole.ADMIN -> "Portal Locked: System Administration"
                    },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeepForestDark
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (targetRole == UserRole.CUSTOMER) {
            CitizenGovtOtpAuthContent(
                onDismiss = onDismiss,
                onRequestCitizenOtp = onRequestCitizenOtp,
                onVerifyCitizenOtp = onVerifyCitizenOtp
            )
        } else {
            OfficerPinAuthContent(
                targetRole = targetRole,
                onDismiss = onDismiss,
                onAuthenticateOfficer = onAuthenticateOfficer
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CitizenGovtOtpAuthContent(
    onDismiss: () -> Unit,
    onRequestCitizenOtp: (mobile: String, fullName: String) -> Result<String>,
    onVerifyCitizenOtp: (mobile: String, otp: String, fullName: String) -> Result<UserAccount>
) {
    var fullNameInput by remember { mutableStateOf("") }
    var mobileInput by remember { mutableStateOf("") }
    var otpInput by remember { mutableStateOf("") }
    var otpRequested by remember { mutableStateOf(false) }
    var generatedDemoOtp by remember { mutableStateOf<String?>(null) }
    var resendSeconds by remember { mutableIntStateOf(0) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(otpRequested, resendSeconds) {
        if (otpRequested && resendSeconds > 0) {
            delay(1000L)
            resendSeconds -= 1
        }
    }

    AnimatedContent(targetState = otpRequested, label = "citizen_otp_step") { isOtpStep ->
        Column(modifier = Modifier.fillMaxWidth()) {
            if (!isOtpStep) {
                Text(
                    text = "Citizen Full Name *",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DeepForestDark
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = fullNameInput,
                    onValueChange = {
                        fullNameInput = it
                        errorMessage = null
                    },
                    placeholder = {
                        Text(
                            text = "e.g. Dr. Ananya Sharma",
                            fontSize = 13.sp,
                            color = TextSubtle
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextDark,
                        unfocusedTextColor = TextDark,
                        cursorColor = DeepForestDark,
                        focusedPlaceholderColor = TextSubtle,
                        unfocusedPlaceholderColor = TextSubtle,
                        focusedBorderColor = DeepForestMedium,
                        unfocusedBorderColor = BorderMedium,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("citizen_name_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Mobile Number (Aadhaar / e-Seva Linked) *",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DeepForestDark
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = SurfaceContainerNav,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, BorderMedium),
                        modifier = Modifier.heightIn(min = 54.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        ) {
                            Text(
                                text = "🇮🇳 +91",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = DeepForestDark
                            )
                        }
                    }

                    OutlinedTextField(
                        value = mobileInput,
                        onValueChange = { raw ->
                            val digits = raw.filter { it.isDigit() }.take(10)
                            mobileInput = digits
                            errorMessage = null
                        },
                        placeholder = {
                            Text(
                                text = "10-digit mobile number",
                                fontSize = 13.sp,
                                color = TextSubtle
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextDark,
                            unfocusedTextColor = TextDark,
                            cursorColor = DeepForestDark,
                            focusedPlaceholderColor = TextSubtle,
                            unfocusedPlaceholderColor = TextSubtle,
                            focusedBorderColor = DeepForestMedium,
                            unfocusedBorderColor = BorderMedium,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("citizen_mobile_input")
                    )
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage ?: "",
                        color = PolishRed,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Surface(
                    color = SageContainer,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 42.dp)
                        .testTag("autofill_demo_citizen_btn")
                        .clickable {
                            fullNameInput = "Dr. Ananya Sharma"
                            mobileInput = "9876543210"
                            errorMessage = null
                        }
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "⚡ Tap to autofill demo citizen (+91 9876543210)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = DeepForestDark,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

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
                            .widthIn(min = 100.dp)
                            .heightIn(min = 48.dp)
                    ) {
                        Text("Cancel", color = TextMuted)
                    }

                    Button(
                        onClick = {
                            val res = onRequestCitizenOtp(mobileInput, fullNameInput)
                            if (res.isSuccess) {
                                generatedDemoOtp = res.getOrNull()
                                otpInput = ""
                                otpRequested = true
                                resendSeconds = 30
                                errorMessage = null
                            } else {
                                errorMessage = res.exceptionOrNull()?.message
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DeepForestDark),
                        modifier = Modifier
                            .weight(1.5f)
                            .widthIn(min = 160.dp)
                            .heightIn(min = 48.dp)
                            .testTag("send_otp_button")
                    ) {
                        Text(
                            text = "Get 6-Digit OTP",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                // Step 2: 6-digit OTP verification
                Surface(
                    color = SurfaceContainerNav,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, BorderPolish),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "OTP sent to +91 ${mobileInput.take(5)} •••••",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = DeepForestDark,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                        Text(
                            text = "Change Number",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = PolishBlue,
                            modifier = Modifier
                                .align(Alignment.CenterVertically)
                                .clickable {
                                    otpRequested = false
                                    otpInput = ""
                                    errorMessage = null
                                }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                generatedDemoOtp?.let { demoCode ->
                    Surface(
                        color = PolishGreenBg,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, PolishGreen.copy(alpha = 0.25f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 42.dp)
                            .testTag("autofill_otp_pill")
                            .clickable {
                                otpInput = demoCode
                                errorMessage = null
                            }
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "📩 SMS Verification OTP: $demoCode (Tap to fill)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = PolishGreen,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Text(
                    text = "Enter 6-Digit Verification OTP",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DeepForestDark
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = otpInput,
                    onValueChange = { raw ->
                        otpInput = raw.filter { it.isDigit() }.take(6)
                        errorMessage = null
                    },
                    placeholder = {
                        Text(
                            text = "6-digit OTP (e.g. 482910)",
                            fontSize = 13.sp,
                            color = TextSubtle
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextDark,
                        unfocusedTextColor = TextDark,
                        cursorColor = DeepForestDark,
                        focusedPlaceholderColor = TextSubtle,
                        unfocusedPlaceholderColor = TextSubtle,
                        focusedBorderColor = DeepForestMedium,
                        unfocusedBorderColor = BorderMedium,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("citizen_otp_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    if (resendSeconds > 0) {
                        Text(
                            text = "Resend OTP in ${resendSeconds}s",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    } else {
                        Text(
                            text = "Resend OTP",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = PolishBlue,
                            modifier = Modifier.clickable {
                                val res = onRequestCitizenOtp(mobileInput, fullNameInput)
                                if (res.isSuccess) {
                                    generatedDemoOtp = res.getOrNull()
                                    resendSeconds = 30
                                    errorMessage = null
                                }
                            }
                        )
                    }
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage ?: "",
                        color = PolishRed,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

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
                            .widthIn(min = 100.dp)
                            .heightIn(min = 48.dp)
                    ) {
                        Text("Cancel", color = TextMuted)
                    }

                    Button(
                        onClick = {
                            val res = onVerifyCitizenOtp(mobileInput, otpInput, fullNameInput)
                            if (res.isFailure) {
                                errorMessage = res.exceptionOrNull()?.message
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DeepForestDark),
                        modifier = Modifier
                            .weight(1.5f)
                            .widthIn(min = 160.dp)
                            .heightIn(min = 48.dp)
                            .testTag("verify_otp_button")
                    ) {
                        Text(
                            text = "Verify OTP & Sign In",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OfficerPinAuthContent(
    targetRole: UserRole,
    onDismiss: () -> Unit,
    onAuthenticateOfficer: (email: String, pin: String, role: UserRole) -> Result<UserAccount>
) {
    val defaultEmail = if (targetRole == UserRole.STAFF) "rajesh.k@passport.gov.in" else "admin@qease.gov.in"
    val defaultPin = if (targetRole == UserRole.STAFF) "1234" else "8899"

    var emailInput by remember(targetRole) { mutableStateOf(defaultEmail) }
    var pinInput by remember(targetRole) { mutableStateOf("") }
    var pinVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Text(
        text = "Officer Government ID / Email",
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = DeepForestDark
    )
    Spacer(modifier = Modifier.height(6.dp))
    OutlinedTextField(
        value = emailInput,
        onValueChange = {
            emailInput = it
            errorMessage = null
        },
        placeholder = {
            Text(
                text = "Enter Officer ID / Email",
                fontSize = 13.sp,
                color = TextSubtle
            )
        },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = TextDark,
            unfocusedTextColor = TextDark,
            cursorColor = DeepForestDark,
            focusedPlaceholderColor = TextSubtle,
            unfocusedPlaceholderColor = TextSubtle,
            focusedBorderColor = DeepForestMedium,
            unfocusedBorderColor = BorderMedium,
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("auth_email_input")
    )

    Spacer(modifier = Modifier.height(12.dp))

    Text(
        text = when (targetRole) {
            UserRole.STAFF -> "Desk Operator PIN (Demo: 1234)"
            UserRole.ADMIN -> "Master Admin Key (Demo: 8899)"
            else -> "Security PIN"
        },
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = DeepForestDark
    )
    Spacer(modifier = Modifier.height(6.dp))
    OutlinedTextField(
        value = pinInput,
        onValueChange = {
            pinInput = it
            errorMessage = null
        },
        placeholder = {
            Text(
                text = "Enter PIN",
                fontSize = 13.sp,
                color = TextSubtle
            )
        },
        singleLine = true,
        visualTransformation = if (pinVisible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        trailingIcon = {
            IconButton(onClick = { pinVisible = !pinVisible }) {
                Icon(
                    imageVector = if (pinVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    contentDescription = "Toggle PIN visibility",
                    tint = TextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        },
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = TextDark,
            unfocusedTextColor = TextDark,
            cursorColor = DeepForestDark,
            focusedPlaceholderColor = TextSubtle,
            unfocusedPlaceholderColor = TextSubtle,
            focusedBorderColor = DeepForestMedium,
            unfocusedBorderColor = BorderMedium,
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("auth_pin_input")
    )

    if (errorMessage != null) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = errorMessage ?: "",
            color = PolishRed,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }

    Spacer(modifier = Modifier.height(16.dp))

    Surface(
        color = SageContainer,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 42.dp)
            .testTag("autofill_demo_officer_btn")
            .clickable {
                emailInput = defaultEmail
                pinInput = defaultPin
                errorMessage = null
            }
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = "⚡ Tap to autofill verified demo credentials",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = DeepForestDark,
                textAlign = TextAlign.Center
            )
        }
    }

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
                .widthIn(min = 100.dp)
                .heightIn(min = 48.dp)
        ) {
            Text("Cancel", color = TextMuted)
        }

        Button(
            onClick = {
                val res = onAuthenticateOfficer(emailInput, pinInput, targetRole)
                if (res.isFailure) {
                    errorMessage = res.exceptionOrNull()?.message
                }
            },
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = DeepForestDark),
            modifier = Modifier
                .weight(1.5f)
                .widthIn(min = 160.dp)
                .heightIn(min = 48.dp)
                .testTag("auth_submit_button")
        ) {
            Text(
                text = "Authenticate & Enter",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
        }
    }
}
