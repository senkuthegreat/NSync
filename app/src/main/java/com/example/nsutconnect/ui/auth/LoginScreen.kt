package com.example.nsutconnect.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import app.rive.runtime.kotlin.RiveAnimationView
import com.example.nsutconnect.R
import com.example.nsutconnect.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    initialRollNumber: String = "",
    initialPassword: String = "",
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onLogin: (rollNumber: String, password: String, autoLogin: Boolean) -> Unit
) {
    var rollNumber by remember { mutableStateOf(initialRollNumber) }
    var password by remember { mutableStateOf(initialPassword) }
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
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Greg the Frog Rive Animation Mascot
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
                    .clip(RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        RiveAnimationView(ctx).apply {
                            setRiveResource(
                                resId = R.raw.greg_the_frog,
                                stateMachineName = "State Machine 1",
                                autoplay = true
                            )
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "N",
                    fontFamily = AppleSans,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 32.sp,
                    letterSpacing = (-0.5).sp,
                    color = AppleLabel
                )
                Text(
                    text = "Sync",
                    fontFamily = AppleSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 32.sp,
                    letterSpacing = (-0.5).sp,
                    color = AppleSystemBlue
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Error Banner with Apple Accordion Spring Motion
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

            // Input Container Card (Clean, Borderless Apple Card)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = AppleCardSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Roll Number Field
                    Text(
                        text = "ROLL NUMBER / USER ID",
                        fontFamily = AppleSans,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AppleSecondaryLabel,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                    )

                    TextField(
                        value = rollNumber,
                        onValueChange = {
                            rollNumber = it.uppercase()
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

                    // Password Field
                    Text(
                        text = "IMS PASSWORD",
                        fontFamily = AppleSans,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AppleSecondaryLabel,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                    )

                    TextField(
                        value = password,
                        onValueChange = {
                            password = it
                            validationError = null
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
                                    painter = painterResource(
                                        id = if (passwordVisible) R.drawable.ic_huge_view else R.drawable.ic_huge_view_off
                                    ),
                                    contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                    tint = AppleSecondaryLabel,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                submitLogin(
                                    roll = rollNumber,
                                    pass = password,
                                    onError = { validationError = it },
                                    onProceed = { onLogin(rollNumber, password, true) }
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
                        onError = { validationError = it },
                        onProceed = { onLogin(rollNumber, password, true) }
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
    onError: (String) -> Unit,
    onProceed: () -> Unit
) {
    if (roll.isBlank()) {
        onError("Please enter your Roll Number / User ID")
        return
    }
    if (pass.isBlank()) {
        onError("Please enter your IMS Password")
        return
    }
    onProceed()
}
