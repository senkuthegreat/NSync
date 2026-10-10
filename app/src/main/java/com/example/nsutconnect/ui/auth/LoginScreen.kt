package com.example.nsutconnect.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nsutconnect.R
import com.example.nsutconnect.data.network.CaptchaSession
import com.example.nsutconnect.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    initialRollNumber: String = "",
    initialPassword: String = "",
    isLoading: Boolean = false,
    errorMessage: String? = null,
    captchaSession: CaptchaSession? = null,
    isCaptchaLoading: Boolean = false,
    onRefreshCaptcha: () -> Unit = {},
    onLogin: (rollNumber: String, password: String, captcha: String, autoLogin: Boolean) -> Unit
) {
    var rollNumber by remember { mutableStateOf(initialRollNumber.take(12).uppercase()) }
    var password by remember { mutableStateOf(initialPassword.take(32)) }
    var captcha by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    Scaffold(
        containerColor = AppleBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // NSync Vector Monogram Logo
            Image(
                painter = painterResource(id = R.drawable.ic_nsync_logo),
                contentDescription = "NSync Logo",
                modifier = Modifier.size(92.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // App Name in Unkempt Font per user design
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "N",
                    fontFamily = UnkemptFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 40.sp,
                    letterSpacing = 0.5.sp,
                    color = AppleLabel
                )
                Text(
                    text = "Sync",
                    fontFamily = UnkemptFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 40.sp,
                    letterSpacing = 0.5.sp,
                    color = AppleSystemBlue
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Error Banner with Accordion Animation
            val displayError = errorMessage ?: validationError
            AnimatedVisibility(
                visible = displayError != null,
                enter = appleAccordionEnter(),
                exit = appleAccordionExit()
            ) {
                if (displayError != null) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = AppleSystemRedBg
                    ) {
                        Text(
                            text = displayError,
                            fontFamily = AppleSans,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = AppleSystemRed,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            }

            // Input Container Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = AppleCardSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Roll Number Field (Max 12 Chars, Uppercase AlphaNumeric Only)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ROLL NUMBER / USER ID",
                            fontFamily = AppleSans,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AppleSecondaryLabel,
                            letterSpacing = 0.5.sp,
                            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                        )
                        Text(
                            text = "${rollNumber.length}/12",
                            fontFamily = AppleSans,
                            fontSize = 10.sp,
                            color = AppleSecondaryLabel.copy(alpha = 0.6f),
                            modifier = Modifier.padding(end = 4.dp, bottom = 6.dp)
                        )
                    }

                    TextField(
                        value = rollNumber,
                        onValueChange = { input ->
                            val clean = input.filter { it.isLetterOrDigit() }.take(12).uppercase()
                            rollNumber = clean
                            validationError = null
                        },
                        placeholder = {
                            Text(
                                text = "e.g. 2024UCO1500",
                                fontFamily = AppleSans,
                                color = AppleSecondaryLabel.copy(alpha = 0.6f)
                            )
                        },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_huge_user),
                                contentDescription = null,
                                tint = AppleSecondaryLabel,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Ascii,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = AppleElevatedSurface,
                            unfocusedContainerColor = AppleElevatedSurface,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent,
                            focusedTextColor = AppleLabel,
                            unfocusedTextColor = AppleLabel
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Password Field (Max 32 Chars)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "IMS PASSWORD",
                            fontFamily = AppleSans,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AppleSecondaryLabel,
                            letterSpacing = 0.5.sp,
                            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                        )
                        Text(
                            text = "${password.length}/32",
                            fontFamily = AppleSans,
                            fontSize = 10.sp,
                            color = AppleSecondaryLabel.copy(alpha = 0.6f),
                            modifier = Modifier.padding(end = 4.dp, bottom = 6.dp)
                        )
                    }

                    TextField(
                        value = password,
                        onValueChange = { input ->
                            if (input.length <= 32) {
                                password = input
                                validationError = null
                            }
                        },
                        placeholder = {
                            Text(
                                text = "Enter password",
                                fontFamily = AppleSans,
                                color = AppleSecondaryLabel.copy(alpha = 0.6f)
                            )
                        },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_huge_lock),
                                contentDescription = null,
                                tint = AppleSecondaryLabel,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                    tint = AppleSecondaryLabel,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = AppleElevatedSurface,
                            unfocusedContainerColor = AppleElevatedSurface,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent,
                            focusedTextColor = AppleLabel,
                            unfocusedTextColor = AppleLabel
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Security Captcha Verification Section
                    Text(
                        text = "SECURITY CAPTCHA",
                        fontFamily = AppleSans,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AppleSecondaryLabel,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Live Captcha Image Surface
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF1F3F9)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (isCaptchaLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = AppleSystemBlue
                                    )
                                } else if (captchaSession?.captchaBitmap != null) {
                                    Image(
                                        bitmap = captchaSession.captchaBitmap.asImageBitmap(),
                                        contentDescription = "Security Captcha Code",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Fit
                                    )
                                } else {
                                    Text(
                                        text = "Tap 🔄 to load",
                                        color = Color.DarkGray,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        // Refresh Captcha Button
                        IconButton(
                            onClick = onRefreshCaptcha,
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(AppleElevatedSurface)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_huge_refresh),
                                contentDescription = "Refresh Captcha",
                                tint = AppleSystemBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Captcha Code Input Field (Max 6 Chars)
                    TextField(
                        value = captcha,
                        onValueChange = { input ->
                            val clean = input.filter { it.isLetterOrDigit() }.take(6).uppercase()
                            captcha = clean
                            validationError = null
                        },
                        placeholder = {
                            Text(
                                text = "Enter code shown above",
                                fontFamily = AppleSans,
                                color = AppleSecondaryLabel.copy(alpha = 0.6f)
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Ascii,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                submitLogin(
                                    roll = rollNumber,
                                    pass = password,
                                    cap = captcha,
                                    onError = { validationError = it },
                                    onProceed = { onLogin(rollNumber, password, captcha, true) }
                                )
                            }
                        ),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = AppleElevatedSurface,
                            unfocusedContainerColor = AppleElevatedSurface,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent,
                            focusedTextColor = AppleLabel,
                            unfocusedTextColor = AppleLabel
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Primary Sign In Button
            val signInInteraction = remember { MutableInteractionSource() }
            Button(
                onClick = {
                    focusManager.clearFocus()
                    submitLogin(
                        roll = rollNumber,
                        pass = password,
                        cap = captcha,
                        onError = { validationError = it },
                        onProceed = { onLogin(rollNumber, password, captcha, true) }
                    )
                },
                interactionSource = signInInteraction,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .bouncePress(signInInteraction),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppleSystemBlue),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Sign In",
                        fontFamily = AppleSans,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}

private fun submitLogin(
    roll: String,
    pass: String,
    cap: String,
    onError: (String) -> Unit,
    onProceed: () -> Unit
) {
    val cleanRoll = roll.trim().uppercase()
    if (cleanRoll.isBlank()) {
        onError("Please enter your Roll Number / User ID")
        return
    }
    if (cleanRoll.length < 8) {
        onError("Roll Number is too short (e.g. 2024UCO1500)")
        return
    }
    val nsutPattern = Regex("""^20\d{2}[A-Z]{2,5}\d{2,5}$""")
    if (!cleanRoll.matches(nsutPattern)) {
        onError("Invalid NSUT Roll Number format (e.g. 2024UCO1500)")
        return
    }
    if (pass.isBlank()) {
        onError("Please enter your IMS Password")
        return
    }
    if (pass.length < 4) {
        onError("Password must be at least 4 characters")
        return
    }
    if (cap.isBlank()) {
        onError("Please enter the security captcha code shown above")
        return
    }
    onProceed()
}
