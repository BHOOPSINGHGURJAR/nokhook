package com.example.auth

import android.util.Patterns
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.example.ui.components.NokhookBrandMark
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.TierLightRead
import com.example.ui.theme.TierSkip
import com.example.ui.theme.TierWorthReading
import com.example.viewmodel.PendingRegistrationDraft
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthGateBottomSheet(
    actionReason: String,
    initialSignUpMode: Boolean = false,
    onDismiss: () -> Unit,
    onSaveDraft: (PendingRegistrationDraft) -> Unit,
    onAuthSuccess: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = ObsidianBg,
        dragHandle = null
    ) {
        LoginSignUpAuthContent(
            actionReason = actionReason,
            initialSignUpMode = initialSignUpMode,
            onDismiss = onDismiss,
            onSaveDraft = onSaveDraft,
            onAuthSuccess = onAuthSuccess,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        )
    }
}

/**
 * Standalone full-screen composable for Login & Sign Up powered by Firebase Auth (Credential Manager)
 * with live input validation and a 5-segment password strength meter.
 */
@Composable
fun LoginSignUpScreen(
    actionReason: String = "",
    initialSignUpMode: Boolean = false,
    onClose: () -> Unit,
    onSaveDraft: (PendingRegistrationDraft) -> Unit,
    onAuthSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = ObsidianBg
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            LoginSignUpAuthContent(
                actionReason = actionReason,
                initialSignUpMode = initialSignUpMode,
                onDismiss = onClose,
                onSaveDraft = onSaveDraft,
                onAuthSuccess = onAuthSuccess,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun LoginSignUpAuthContent(
    actionReason: String,
    initialSignUpMode: Boolean,
    onDismiss: () -> Unit,
    onSaveDraft: (PendingRegistrationDraft) -> Unit,
    onAuthSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var isSignUpMode by remember(initialSignUpMode) { mutableStateOf(initialSignUpMode) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun triggerFirebaseGoogleAuth() {
        if (Firebase.auth.currentUser != null) {
            onAuthSuccess()
            return
        }
        isLoading = true
        errorMessage = null
        onGoogleSignInClicked(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = {
                isLoading = false
                onAuthSuccess()
            },
            onAuthError = { msg ->
                isLoading = false
                errorMessage = msg
            },
            scope = scope,
            onAuthCancelled = { isLoading = false }
        )
    }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Brand Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.width(40.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                NokhookBrandMark(tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "NOKHOOK",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.8.sp
                    ),
                    color = Color.White
                )
            }
            IconButton(onClick = onDismiss, modifier = Modifier.testTag("auth_close_button")) {
                Icon(Icons.Filled.Close, contentDescription = "Close login sheet", tint = Color.White)
            }
        }

        if (actionReason.isNotBlank()) {
            Text(
                text = actionReason,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFC4B5FD),
                modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
            )
        } else {
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Crisp White Floating Card matching reference Auth Screen
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 22.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (isSignUpMode) "Sign Up" else "Log In",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111827),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                // Segmented Mode Switcher inside Card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF3F4F6),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        AuthModePill(
                            label = "Log In",
                            selected = !isSignUpMode,
                            onClick = {
                                errorMessage = null
                                isSignUpMode = false
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("auth_mode_login")
                        )
                        AuthModePill(
                            label = "Sign Up",
                            selected = isSignUpMode,
                            onClick = {
                                errorMessage = null
                                isSignUpMode = true
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("auth_mode_signup")
                        )
                    }
                }

                AnimatedVisibility(visible = errorMessage != null) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = TierSkip.copy(alpha = 0.1f),
                        border = BorderStroke(1.dp, TierSkip.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = errorMessage.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = TierSkip,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .padding(10.dp)
                                .testTag("auth_error_banner")
                        )
                    }
                }

                if (!isSignUpMode) {
                    SignInCardContent(
                        isLoading = isLoading,
                        onValidatedCredentialsLogin = { draft ->
                            onSaveDraft(draft)
                            triggerFirebaseGoogleAuth()
                        },
                        onGoogleSignIn = {
                            onSaveDraft(
                                PendingRegistrationDraft(
                                    verifiedMethod = "email",
                                    phoneNumber = "",
                                    countryCode = "",
                                    phoneVerified = false
                                )
                            )
                            triggerFirebaseGoogleAuth()
                        },
                        onValidationError = { msg -> errorMessage = msg.ifBlank { null } }
                    )
                } else {
                    SignUpOtpCardContent(
                        isLoading = isLoading,
                        onVerifiedAndGoogleConnect = { draft ->
                            onSaveDraft(draft)
                            triggerFirebaseGoogleAuth()
                        },
                        onValidationError = { msg -> errorMessage = msg.ifBlank { null } },
                        onBackToLogin = {
                            errorMessage = null
                            isSignUpMode = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable {
                    errorMessage = null
                    isSignUpMode = !isSignUpMode
                }
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .testTag("auth_toggle_mode_link")
        ) {
            Text(
                text = if (isSignUpMode) "Already have an account? " else "New to nokhook? ",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFD1D5DB)
            )
            Text(
                text = if (isSignUpMode) "Log In" else "Sign Up",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
private fun AuthModePill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(38.dp)
            .clip(RoundedCornerShape(9.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(9.dp),
        color = if (selected) Color.White else Color.Transparent,
        shadowElevation = if (selected) 1.dp else 0.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) Color(0xFF111827) else Color(0xFF6B7280)
            )
        }
    }
}

@Composable
private fun VerificationChannelSelector(
    selectedChannel: VerificationChannel,
    onSelectChannel: (VerificationChannel) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "Verification Method (choose Phone OR Email)",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF4B5563)
        )
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFFF3F4F6),
            border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                VerificationChannel.entries.forEach { channel ->
                    val isSelected = channel == selectedChannel
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .clickable {
                                if (!isSelected) onSelectChannel(channel)
                            }
                            .testTag(
                                if (channel == VerificationChannel.PHONE) "channel_phone_tab"
                                else "channel_email_tab"
                            ),
                        shape = RoundedCornerShape(9.dp),
                        color = if (isSelected) Color.White else Color.Transparent,
                        shadowElevation = if (isSelected) 1.dp else 0.dp
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (channel == VerificationChannel.PHONE) {
                                    Icons.Filled.Phone
                                } else {
                                    Icons.Filled.Email
                                },
                                contentDescription = null,
                                tint = if (isSelected) ElectricViolet else Color(0xFF6B7280),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = channel.label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color(0xFF111827) else Color(0xFF6B7280)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SignInCardContent(
    isLoading: Boolean,
    onValidatedCredentialsLogin: (PendingRegistrationDraft) -> Unit,
    onGoogleSignIn: () -> Unit,
    onValidationError: (String) -> Unit
) {
    var selectedChannel by remember { mutableStateOf(VerificationChannel.PHONE) }
    var countryCode by remember { mutableStateOf("+91") }
    var mobileNumber by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var submitted by remember { mutableStateOf(false) }

    val phoneError = if (submitted && selectedChannel == VerificationChannel.PHONE && mobileNumber.length < 8) {
        "Enter at least 8 mobile digits"
    } else null
    val emailError = if (
        submitted &&
        selectedChannel == VerificationChannel.EMAIL &&
        !Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()
    ) {
        "Enter a valid email address"
    } else null
    val passwordError = if (submitted && password.length < 6) {
        "Password must be at least 6 characters"
    } else null

    val lightFieldColors = nokhookLightAuthFieldColors()

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        VerificationChannelSelector(
            selectedChannel = selectedChannel,
            onSelectChannel = { nextChannel ->
                selectedChannel = nextChannel
                submitted = false
                onValidationError("")
                if (nextChannel == VerificationChannel.PHONE) {
                    email = ""
                } else {
                    mobileNumber = ""
                }
            },
            modifier = Modifier.testTag("login_channel_selector")
        )

        if (selectedChannel == VerificationChannel.PHONE) {
            FieldLabel("Mobile Number")
            OutlinedTextField(
                value = mobileNumber,
                onValueChange = { mobileNumber = it.filter(Char::isDigit).take(12) },
                placeholder = { Text("7023337229") },
                prefix = { Text("$countryCode ", color = Color(0xFF4B5563)) },
                isError = phoneError != null,
                supportingText = phoneError?.let { msg -> { Text(msg, color = TierSkip) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = lightFieldColors,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("login_mobile_input")
            )
        } else {
            FieldLabel("Email Address")
            OutlinedTextField(
                value = email,
                onValueChange = { email = it.trim().take(64) },
                placeholder = { Text("reader@example.com") },
                isError = emailError != null,
                supportingText = emailError?.let { msg -> { Text(msg, color = TierSkip) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = lightFieldColors,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("login_email_input")
            )
        }

        FieldLabel("Password")
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            placeholder = { Text("Enter password") },
            isError = passwordError != null,
            supportingText = passwordError?.let { msg -> { Text(msg, color = TierSkip) } },
            singleLine = true,
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { showPassword = !showPassword }) {
                    Icon(
                        imageVector = if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = "Toggle password visibility",
                        tint = Color(0xFF6B7280)
                    )
                }
            },
            shape = RoundedCornerShape(10.dp),
            colors = lightFieldColors,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("login_password_input")
        )

        Button(
            onClick = {
                submitted = true
                when {
                    selectedChannel == VerificationChannel.PHONE && mobileNumber.length < 8 ->
                        onValidationError("Enter a valid mobile number (at least 8 digits)")
                    selectedChannel == VerificationChannel.EMAIL &&
                        !Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() ->
                        onValidationError("Enter a valid email address")
                    password.length < 6 ->
                        onValidationError("Password must be at least 6 characters")
                    else -> {
                        onValidationError("")
                        if (selectedChannel == VerificationChannel.PHONE) {
                            onValidatedCredentialsLogin(
                                PendingRegistrationDraft(
                                    username = "reader_${mobileNumber.takeLast(4)}",
                                    countryCode = countryCode,
                                    phoneNumber = mobileNumber,
                                    phoneVerified = true,
                                    email = "",
                                    verifiedMethod = "phone"
                                )
                            )
                        } else {
                            onValidatedCredentialsLogin(
                                PendingRegistrationDraft(
                                    username = email.substringBefore("@").filter { it.isLetterOrDigit() || it == '_' }.take(24),
                                    countryCode = "",
                                    phoneNumber = "",
                                    phoneVerified = false,
                                    email = email.trim(),
                                    verifiedMethod = "email"
                                )
                            )
                        }
                    }
                }
            },
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("login_submit_button"),
            shape = RoundedCornerShape(999.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = ElectricViolet,
                contentColor = Color.White
            )
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = if (selectedChannel == VerificationChannel.PHONE) {
                        "Log In with Mobile Number"
                    } else {
                        "Log In with Email"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        if (selectedChannel == VerificationChannel.EMAIL) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE5E7EB))
                Text(
                    text = "  OR  ",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF6B7280)
                )
                HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE5E7EB))
            }

            OutlinedButton(
                onClick = onGoogleSignIn,
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("google_signin_button"),
                shape = RoundedCornerShape(999.dp),
                border = BorderStroke(1.dp, Color(0xFFD1D5DB)),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.White,
                    contentColor = Color(0xFF111827)
                )
            ) {
                Icon(Icons.Filled.Lock, contentDescription = null, tint = ElectricViolet, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Continue with Google Email",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun SignUpOtpCardContent(
    isLoading: Boolean,
    onVerifiedAndGoogleConnect: (PendingRegistrationDraft) -> Unit,
    onValidationError: (String) -> Unit,
    onBackToLogin: () -> Unit
) {
    var selectedChannel by remember { mutableStateOf(VerificationChannel.PHONE) }
    var username by remember { mutableStateOf("") }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var countryCode by remember { mutableStateOf("+91") }
    var countryFlag by remember { mutableStateOf("\uD83C\uDDEE\uD83C\uDDF3") }
    var showCountryMenu by remember { mutableStateOf(false) }
    var mobileNumber by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var captchaVerified by remember { mutableStateOf(false) }
    var otpState by remember { mutableStateOf(OtpSessionState()) }
    var termsAccepted by remember { mutableStateOf(true) }
    var attemptedSubmit by remember { mutableStateOf(false) }

    val countryOptions = listOf(
        Triple("\uD83C\uDDEE\uD83C\uDDF3", "+91", "India"),
        Triple("\uD83C\uDDFA\uD83C\uDDF8", "+1", "USA"),
        Triple("\uD83C\uDDEC\uD83C\uDDE7", "+44", "UK"),
        Triple("\uD83C\uDDEF\uD83C\uDDF5", "+81", "Japan"),
        Triple("\uD83C\uDDE6\uD83C\uDDFA", "+61", "Australia")
    )

    val lightFieldColors = nokhookLightAuthFieldColors()

    // 60-second OTP validity & resend cooldown countdown
    LaunchedEffect(otpState.sessionId, otpState.isSent, otpState.isVerified) {
        if (otpState.isSent && !otpState.isVerified) {
            while (otpState.remainingSeconds > 0) {
                delay(1000L)
                otpState = OtpVerificationEngine.onTick(otpState)
            }
        }
    }

    val isEmailValid = Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()
    val passwordStrength = remember(password) { computePasswordStrength(password) }
    val usernameError = if (attemptedSubmit && username.length < 3) "Min 3 letters/digits required" else null
    val phoneError = if (attemptedSubmit && selectedChannel == VerificationChannel.PHONE && mobileNumber.length < 8) {
        "Enter at least 8 digits"
    } else null
    val emailError = if (attemptedSubmit && selectedChannel == VerificationChannel.EMAIL && !isEmailValid) {
        "Enter a valid email address"
    } else null
    val firstNameError = if (attemptedSubmit && firstName.isBlank()) "First name is required" else null
    val passwordError = if (attemptedSubmit && password.length < 6) "Password must be at least 6 characters" else null

    val currentDestination = if (selectedChannel == VerificationChannel.PHONE) {
        "$countryCode $mobileNumber".trim()
    } else {
        email.trim()
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        VerificationChannelSelector(
            selectedChannel = selectedChannel,
            onSelectChannel = { nextChannel ->
                selectedChannel = nextChannel
                attemptedSubmit = false
                onValidationError("")
                otpState = OtpVerificationEngine.invalidateForDestinationOrChannelChange(otpState, nextChannel)
                if (nextChannel == VerificationChannel.PHONE) {
                    email = ""
                } else {
                    mobileNumber = ""
                }
            },
            modifier = Modifier.testTag("verification_channel_selector")
        )

        FieldLabel("Username")
        OutlinedTextField(
            value = username,
            onValueChange = { username = it.filter { ch -> ch.isLetterOrDigit() || ch == '_' }.take(24) },
            placeholder = { Text("reader_handle") },
            prefix = { Text("@", color = Color(0xFF6B7280)) },
            isError = usernameError != null,
            supportingText = usernameError?.let { msg -> { Text(msg, color = TierSkip) } },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            colors = lightFieldColors,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("signup_username_input")
        )

        if (selectedChannel == VerificationChannel.PHONE) {
            FieldLabel("Mobile Number (OTP Verification)")
            Box {
                OutlinedTextField(
                    value = mobileNumber,
                    onValueChange = { raw ->
                        val cleaned = raw.filter(Char::isDigit).take(12)
                        if (cleaned != mobileNumber) {
                            mobileNumber = cleaned
                            otpState = OtpVerificationEngine.invalidateForDestinationOrChannelChange(
                                otpState,
                                VerificationChannel.PHONE
                            )
                        }
                    },
                    placeholder = { Text("7023337229") },
                    isError = phoneError != null,
                    supportingText = phoneError?.let { msg -> { Text(msg, color = TierSkip) } },
                    leadingIcon = {
                        Row(
                            modifier = Modifier
                                .clickable { showCountryMenu = true }
                                .padding(start = 12.dp, end = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(countryFlag, fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("$countryCode -", color = Color(0xFF4B5563), style = MaterialTheme.typography.bodyMedium)
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = lightFieldColors,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("signup_mobile_input")
                )
                DropdownMenu(
                    expanded = showCountryMenu,
                    onDismissRequest = { showCountryMenu = false }
                ) {
                    countryOptions.forEach { (flag, code, name) ->
                        DropdownMenuItem(
                            text = { Text("$flag  $code ($name)") },
                            onClick = {
                                if (countryCode != code) {
                                    otpState = OtpVerificationEngine.invalidateForDestinationOrChannelChange(
                                        otpState,
                                        VerificationChannel.PHONE
                                    )
                                }
                                countryFlag = flag
                                countryCode = code
                                showCountryMenu = false
                            }
                        )
                    }
                }
            }
        } else {
            FieldLabel("Email Address (OTP Verification)")
            OutlinedTextField(
                value = email,
                onValueChange = { raw ->
                    val cleaned = raw.trim().take(64)
                    if (cleaned != email) {
                        email = cleaned
                        otpState = OtpVerificationEngine.invalidateForDestinationOrChannelChange(
                            otpState,
                            VerificationChannel.EMAIL
                        )
                    }
                },
                placeholder = { Text("reader@example.com") },
                isError = emailError != null,
                supportingText = emailError?.let { msg -> { Text(msg, color = TierSkip) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = lightFieldColors,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("signup_email_input")
            )
        }

        FieldLabel("First Name")
        OutlinedTextField(
            value = firstName,
            onValueChange = { firstName = it },
            placeholder = { Text("Aarav") },
            isError = firstNameError != null,
            supportingText = firstNameError?.let { msg -> { Text(msg, color = TierSkip) } },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            colors = lightFieldColors,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("signup_firstname_input")
        )

        FieldLabel("Last Name")
        OutlinedTextField(
            value = lastName,
            onValueChange = { lastName = it },
            placeholder = { Text("Mehta") },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            colors = lightFieldColors,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("signup_lastname_input")
        )

        FieldLabel("Password")
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            placeholder = { Text("••••••••••") },
            isError = passwordError != null,
            supportingText = passwordError?.let { msg -> { Text(msg, color = TierSkip) } },
            singleLine = true,
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { showPassword = !showPassword }) {
                    Icon(
                        imageVector = if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = "Toggle password visibility",
                        tint = Color(0xFF6B7280)
                    )
                }
            },
            shape = RoundedCornerShape(10.dp),
            colors = lightFieldColors,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("signup_password_input")
        )

        // 5-Segment Password Strength Meter matching reference screen
        if (password.isNotEmpty()) {
            PasswordStrengthMeter(
                password = password,
                strength = passwordStrength,
                modifier = Modifier.testTag("password_strength_meter")
            )
        }

        // Bot Protection / Human Verification Card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .clickable { captchaVerified = !captchaVerified }
                .testTag("captcha_verify_box"),
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFFFAFAFA),
            border = BorderStroke(1.dp, if (captchaVerified) TierWorthReading else Color(0xFFD1D5DB))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (captchaVerified) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = "Human verification passed",
                            tint = Color(0xFF16A34A),
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE5E7EB)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Security, contentDescription = null, tint = ElectricViolet, modifier = Modifier.size(14.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (captchaVerified) "Success!" else "Tap to verify human",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF111827)
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Shield,
                            contentDescription = null,
                            tint = Color(0xFFF97316),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "PROTECT",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF374151)
                        )
                    }
                    Text(
                        text = "Privacy · Help",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF6B7280)
                    )
                }
            }
        }

        AnimatedVisibility(visible = otpState.isSent) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (!otpState.isVerified) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("otp_sms_banner"),
                        shape = RoundedCornerShape(12.dp),
                        color = if (otpState.isExpired) Color(0xFFFEF2F2) else Color(0xFFF5F3FF),
                        border = BorderStroke(
                            1.dp,
                            if (otpState.isExpired) TierSkip.copy(alpha = 0.45f)
                            else ElectricViolet.copy(alpha = 0.35f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = if (otpState.channel == VerificationChannel.PHONE) {
                                            Icons.Filled.Sms
                                        } else {
                                            Icons.Filled.Email
                                        },
                                        contentDescription = "OTP verification notification",
                                        tint = if (otpState.isExpired) TierSkip else ElectricViolet,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        val channelPrefix = if (otpState.channel == VerificationChannel.PHONE) {
                                            "SMS sent to"
                                        } else {
                                            "Email OTP sent to"
                                        }
                                        Text(
                                            text = if (otpState.isExpired) "OTP Expired"
                                            else "$channelPrefix ${otpState.destinationLabel}",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF111827)
                                        )
                                        if (!otpState.isExpired) {
                                            Text(
                                                text = "Code: ${otpState.expectedCode}",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.Bold,
                                                    letterSpacing = 1.5.sp
                                                ),
                                                color = ElectricViolet,
                                                modifier = Modifier.testTag("otp_delivered_code")
                                            )
                                        } else {
                                            Text(
                                                text = "60s window ended — tap Resend OTP",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TierSkip
                                            )
                                        }
                                    }
                                }

                                if (!otpState.isExpired && !otpState.isLockedOut) {
                                    Surface(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                val (next, _) = OtpVerificationEngine.updateEnteredCode(
                                                    otpState,
                                                    otpState.expectedCode
                                                )
                                                otpState = next
                                                if (next.isVerified) {
                                                    onValidationError("")
                                                }
                                            }
                                            .testTag("otp_autofill_button"),
                                        shape = RoundedCornerShape(8.dp),
                                        color = ElectricViolet
                                    ) {
                                        Text(
                                            text = "Auto-fill",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                LinearProgressIndicator(
                                    progress = {
                                        otpState.remainingSeconds.toFloat() /
                                            OtpSessionState.OTP_DURATION_SECONDS.toFloat()
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(5.dp)
                                        .clip(RoundedCornerShape(999.dp)),
                                    color = when {
                                        otpState.isExpired -> TierSkip
                                        otpState.remainingSeconds <= 15 -> TierLightRead
                                        else -> ElectricViolet
                                    },
                                    trackColor = Color(0xFFE5E7EB)
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.Timer,
                                        contentDescription = null,
                                        tint = if (otpState.isExpired) TierSkip else Color(0xFF4B5563),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    val secsFormatted = otpState.remainingSeconds.toString().padStart(2, '0')
                                    Text(
                                        text = "00:$secsFormatted",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = if (otpState.isExpired) TierSkip else Color(0xFF1F2937),
                                        modifier = Modifier.testTag("otp_countdown_timer")
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("otp_verified_badge"),
                        shape = RoundedCornerShape(12.dp),
                        color = TierWorthReading.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, TierWorthReading.copy(alpha = 0.45f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = "OTP verified",
                                tint = TierWorthReading,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            val verifiedLabel = if (otpState.channel == VerificationChannel.PHONE) {
                                "Phone verified (${otpState.destinationLabel})"
                            } else {
                                "Email verified (${otpState.destinationLabel})"
                            }
                            Text(
                                text = verifiedLabel,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF14532D)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FieldLabel("6-Digit OTP")
                    TextButton(
                        onClick = {
                            onValidationError("")
                            otpState = OtpVerificationEngine.startSessionForChannel(
                                current = otpState,
                                channel = selectedChannel,
                                destination = currentDestination
                            )
                        },
                        enabled = otpState.canResend,
                        modifier = Modifier.testTag("resend_otp_button")
                    ) {
                        Text(
                            text = when {
                                otpState.isVerified -> "Verified"
                                otpState.remainingSeconds > 0 -> "Resend in ${otpState.remainingSeconds}s"
                                else -> "Resend OTP"
                            },
                            color = if (otpState.canResend) ElectricViolet else Color(0xFF9CA3AF),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                OtpSixDigitBoxInput(
                    code = otpState.enteredCode,
                    isError = otpState.errorMessage != null && !otpState.isVerified,
                    isVerified = otpState.isVerified,
                    enabled = !otpState.isVerified && !isLoading,
                    onCodeChange = { raw ->
                        val (next, result) = OtpVerificationEngine.updateEnteredCode(otpState, raw)
                        otpState = next
                        when (result) {
                            is OtpVerifyResult.Verified -> onValidationError("")
                            is OtpVerifyResult.Expired,
                            is OtpVerifyResult.InvalidCode,
                            is OtpVerifyResult.LockedOut -> {
                                next.errorMessage?.let { onValidationError(it) }
                            }
                            else -> Unit
                        }
                    }
                )

                otpState.errorMessage?.let { otpErr ->
                    Text(
                        text = otpErr,
                        style = MaterialTheme.typography.bodySmall,
                        color = TierSkip,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.testTag("otp_error_text")
                    )
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { termsAccepted = !termsAccepted }
        ) {
            Checkbox(
                checked = termsAccepted,
                onCheckedChange = { termsAccepted = it },
                colors = CheckboxDefaults.colors(checkedColor = ElectricViolet)
            )
            Text(
                text = "Accept Terms of Service & Privacy Policy",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF374151)
            )
        }

        Button(
            onClick = {
                attemptedSubmit = true
                if (!otpState.isSent) {
                    when {
                        username.length < 3 -> onValidationError("Username must be at least 3 characters")
                        selectedChannel == VerificationChannel.PHONE && mobileNumber.length < 8 ->
                            onValidationError("Enter a valid mobile number first")
                        selectedChannel == VerificationChannel.EMAIL && !isEmailValid ->
                            onValidationError("Enter a valid email address first")
                        firstName.isBlank() -> onValidationError("First name is required")
                        password.length < 6 -> onValidationError("Password must be at least 6 characters")
                        !captchaVerified -> onValidationError("Please complete the human verification check")
                        !termsAccepted -> onValidationError("Please accept Terms & Privacy Policy")
                        else -> {
                            onValidationError("")
                            otpState = OtpVerificationEngine.startSessionForChannel(
                                current = otpState,
                                channel = selectedChannel,
                                destination = currentDestination
                            )
                        }
                    }
                } else {
                    val (verifiedState, verifyResult) = OtpVerificationEngine.verify(otpState)
                    otpState = verifiedState
                    if (verifyResult is OtpVerifyResult.Verified) {
                        onValidationError("")
                        val draft = if (selectedChannel == VerificationChannel.PHONE) {
                            PendingRegistrationDraft(
                                username = username.trim(),
                                firstName = firstName.trim(),
                                lastName = lastName.trim(),
                                countryCode = countryCode,
                                phoneNumber = mobileNumber.trim(),
                                phoneVerified = true,
                                email = "",
                                verifiedMethod = "phone"
                            )
                        } else {
                            PendingRegistrationDraft(
                                username = username.trim(),
                                firstName = firstName.trim(),
                                lastName = lastName.trim(),
                                countryCode = "",
                                phoneNumber = "",
                                phoneVerified = false,
                                email = email.trim(),
                                verifiedMethod = "email"
                            )
                        }
                        onVerifiedAndGoogleConnect(draft)
                    } else {
                        onValidationError(
                            verifiedState.errorMessage
                                ?: "Enter the 6-digit OTP sent to $currentDestination"
                        )
                    }
                }
            },
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag(
                    when {
                        !otpState.isSent -> "send_otp_button"
                        !otpState.isVerified -> "verify_otp_button"
                        else -> "complete_signup_button"
                    }
                ),
            shape = RoundedCornerShape(999.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = ElectricViolet,
                contentColor = Color.White
            )
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = when {
                        !otpState.isSent && selectedChannel == VerificationChannel.PHONE -> "Get Phone OTP"
                        !otpState.isSent && selectedChannel == VerificationChannel.EMAIL -> "Get Email OTP"
                        !otpState.isVerified -> "Verify OTP & Create Account"
                        selectedChannel == VerificationChannel.PHONE -> "Complete Phone Sign Up"
                        else -> "Complete Email Sign Up"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        TextButton(
            onClick = {
                if (otpState.isSent) {
                    otpState = OtpVerificationEngine.invalidateForDestinationOrChannelChange(otpState, selectedChannel)
                    onValidationError("")
                } else {
                    onBackToLogin()
                }
            },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("Back", color = Color(0xFF374151), fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun OtpSixDigitBoxInput(
    code: String,
    isError: Boolean,
    isVerified: Boolean,
    enabled: Boolean,
    onCodeChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    BasicTextField(
        value = code,
        onValueChange = onCodeChange,
        enabled = enabled,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        cursorBrush = SolidColor(ElectricViolet),
        textStyle = TextStyle(color = Color.Transparent),
        modifier = modifier
            .fillMaxWidth()
            .testTag("otp_input_field"),
        decorationBox = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (index in 0 until OtpSessionState.OTP_LENGTH) {
                    val char = code.getOrNull(index)?.toString().orEmpty()
                    val isFocusedCell = enabled && index == code.length.coerceAtMost(OtpSessionState.OTP_LENGTH - 1) && char.isEmpty()
                    val borderColor = when {
                        isVerified -> TierWorthReading
                        isError -> TierSkip
                        isFocusedCell -> ElectricViolet
                        char.isNotEmpty() -> ElectricViolet.copy(alpha = 0.65f)
                        else -> Color(0xFFD1D5DB)
                    }
                    val bgColor = when {
                        isVerified -> TierWorthReading.copy(alpha = 0.08f)
                        isError -> Color(0xFFFEF2F2)
                        char.isNotEmpty() -> Color(0xFFF5F3FF)
                        else -> Color(0xFFF9FAFB)
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(bgColor)
                            .border(
                                width = if (isFocusedCell || isVerified || isError) 1.5.dp else 1.dp,
                                color = borderColor,
                                shape = RoundedCornerShape(10.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = char.ifEmpty { "•" },
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            ),
                            color = if (char.isEmpty()) Color(0xFF9CA3AF) else Color(0xFF111827)
                        )
                    }
                }
            }
        }
    )
}

@Composable
private fun PasswordStrengthMeter(
    password: String,
    strength: PasswordStrength,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            for (seg in 1..5) {
                val active = seg <= strength.segments
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (active) strength.color else Color(0xFFE5E7EB))
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Strength: ${strength.label}",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = strength.color
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PasswordRuleChip("6+ ch", password.length >= 6)
                PasswordRuleChip("Aa", password.any { it.isUpperCase() } && password.any { it.isLowerCase() })
                PasswordRuleChip("0-9", password.any { it.isDigit() })
                PasswordRuleChip("!@#", password.any { !it.isLetterOrDigit() })
            }
        }
    }
}

@Composable
private fun PasswordRuleChip(label: String, met: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (met) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = TierWorthReading,
                modifier = Modifier.size(11.dp)
            )
            Spacer(modifier = Modifier.width(2.dp))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (met) TierWorthReading else Color(0xFF9CA3AF),
            fontWeight = if (met) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun nokhookLightAuthFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color(0xFF111827),
    unfocusedTextColor = Color(0xFF111827),
    errorTextColor = Color(0xFF111827),
    focusedBorderColor = ElectricViolet,
    unfocusedBorderColor = Color(0xFFD1D5DB),
    errorBorderColor = TierSkip,
    focusedContainerColor = Color(0xFFF9FAFB),
    unfocusedContainerColor = Color(0xFFF3F4F6),
    errorContainerColor = Color(0xFFFEF2F2),
    focusedPlaceholderColor = Color(0xFF9CA3AF),
    unfocusedPlaceholderColor = Color(0xFF9CA3AF)
)

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Medium,
        color = Color(0xFF1F2937)
    )
}

internal data class PasswordStrength(val label: String, val segments: Int, val color: Color)

internal fun computePasswordStrength(pw: String): PasswordStrength {
    var score = 0
    if (pw.length >= 6) score++
    if (pw.length >= 10) score++
    if (pw.any { it.isUpperCase() } && pw.any { it.isLowerCase() }) score++
    if (pw.any { it.isDigit() }) score++
    if (pw.any { !it.isLetterOrDigit() }) score++

    return when (score) {
        0, 1 -> PasswordStrength("Weak", 1, TierSkip)
        2 -> PasswordStrength("Fair", 2, TierLightRead)
        3 -> PasswordStrength("Good", 3, TierLightRead)
        4 -> PasswordStrength("Strong", 4, TierWorthReading)
        else -> PasswordStrength("Very Strong", 5, TierWorthReading)
    }
}
