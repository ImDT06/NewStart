package com.example.newstart.ui.screens.auth

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.newstart.R
import com.example.newstart.ui.MainViewModel
import com.example.newstart.ui.theme.NewStartTheme
import com.example.newstart.ui.theme.authHeaderGradient
import com.example.newstart.ui.util.AppCombinedPreviews
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.newstart.ui.util.LanguagePickerDialog
import com.example.newstart.ui.util.TransparentLanguageSwitcher
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onNavigateBack: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onLoginSuccess: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = hiltViewModel(),
    mainViewModel: MainViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = CredentialManager.create(context)
    var showLanguagePicker by remember { mutableStateOf(false) }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var resetEmail by remember { mutableStateOf("") }

    if (showForgotPasswordDialog) {
        AlertDialog(
            onDismissRequest = { showForgotPasswordDialog = false },
            title = { Text("Quên mật khẩu") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Nhập email của bạn để nhận liên kết đặt lại mật khẩu.")
                    OutlinedTextField(
                        value = resetEmail,
                        onValueChange = { resetEmail = it },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(20.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.sendPasswordReset(
                            email = resetEmail,
                            onSuccess = {
                                Toast.makeText(context, "Liên kết đã được gửi vào Email của bạn", Toast.LENGTH_LONG).show()
                                showForgotPasswordDialog = false
                            },
                            onError = { error ->
                                Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                            }
                        )
                    },
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Gửi yêu cầu")
                }
            },
            dismissButton = {
                TextButton(onClick = { showForgotPasswordDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }

    LoginContent(
        email = viewModel.email,
        onEmailChange = viewModel::onEmailChange,
        password = viewModel.password,
        onPasswordChange = viewModel::onPasswordChange,
        passwordVisible = viewModel.passwordVisible,
        onPasswordVisibleChange = viewModel::togglePasswordVisibility,
        emailError = viewModel.emailError,
        passwordError = viewModel.passwordError,
        isLoading = viewModel.isLoading,
        onLoginClick = {
            viewModel.login(
                onSuccess = onLoginSuccess,
                onError = { error ->
                    Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                }
            )
        },
        onGoogleLoginClick = {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(context.getString(R.string.default_web_client_id))
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            scope.launch {
                try {
                    val result = credentialManager.getCredential(context, request)
                    val credential = result.credential
                    
                    if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                        viewModel.loginWithGoogle(
                            idToken = googleIdTokenCredential.idToken,
                            onSuccess = onLoginSuccess,
                            onError = { error ->
                                Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                            }
                        )
                    }
                } catch (e: Exception) {
                    Log.e("LoginScreen", "Google login error", e)
                    Toast.makeText(context, "Đăng nhập Google thất bại", Toast.LENGTH_SHORT).show()
                }
            }
        },
        onForgotPasswordClick = {
            resetEmail = viewModel.email
            showForgotPasswordDialog = true
        },
        onRegisterClick = onNavigateToRegister,
        onBackClick = onNavigateBack,
        showLanguagePicker = showLanguagePicker,
        onToggleLanguagePicker = { showLanguagePicker = it },
        modifier = modifier,
        headerGradient = authHeaderGradient(mainViewModel)
    )
}

@Composable
fun LoginContent(
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    passwordVisible: Boolean,
    onPasswordVisibleChange: () -> Unit,
    emailError: String?,
    passwordError: String?,
    isLoading: Boolean,
    onLoginClick: () -> Unit,
    onGoogleLoginClick: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    onRegisterClick: () -> Unit,
    onBackClick: () -> Unit,
    showLanguagePicker: Boolean,
    onToggleLanguagePicker: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    headerGradient: Brush
) {
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .pointerInput(Unit) {
                detectTapGestures {
                    focusManager.clearFocus()
                }
            }
            .verticalScroll(scrollState)
            .imePadding()
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Blue Header Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(brush = headerGradient)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(top = 8.dp, start = 16.dp, end = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    TransparentLanguageSwitcher(
                        onClick = { onToggleLanguagePicker(true) }
                    )
                }

                if (showLanguagePicker) {
                    LanguagePickerDialog(onDismiss = { onToggleLanguagePicker(false) })
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(start = 32.dp, end = 32.dp, bottom = 45.dp), // Adjusted bottom padding
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Text(
                        text = stringResource(id = R.string.login_title),
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }

            // Form Section
            Column(
                modifier = Modifier
                    .padding(top = 175.dp) // Adjusted to overlap new header height
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { // Reduced spacing between inputs
                    AuthInputField(
                        value = email,
                        onValueChange = onEmailChange,
                        label = stringResource(id = R.string.login_label_email),
                        placeholder = stringResource(id = R.string.login_placeholder_email),
                        errorText = emailError,
                        icon = Icons.Default.Email,
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Next,
                            keyboardType = KeyboardType.Email
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        )
                    )

                    Column { // Group Password and Forgot Password for tighter spacing
                        AuthInputField(
                            value = password,
                            onValueChange = onPasswordChange,
                            label = stringResource(id = R.string.login_label_password),
                            placeholder = stringResource(id = R.string.login_placeholder_password),
                            errorText = passwordError,
                            icon = Icons.Default.Lock,
                            isPassword = true,
                            passwordVisible = passwordVisible,
                            onPasswordVisibleChange = onPasswordVisibleChange,
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Done,
                                keyboardType = KeyboardType.Password
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus()
                                    onLoginClick()
                                }
                            )
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .offset(y = (-8).dp), // Negative offset to pull closer to field
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = onForgotPasswordClick,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp) // Reduced padding
                            ) {
                                Text(
                                    text = stringResource(id = R.string.login_forgot_password),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(
                        onClick = onLoginClick,
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 24.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Text(
                                text = stringResource(id = R.string.login_btn_now),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                        Text(
                            text = stringResource(id = R.string.login_or),
                            modifier = Modifier.padding(horizontal = 16.dp),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        OutlinedButton(
                            onClick = onGoogleLoginClick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_google),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = Color.Unspecified
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = stringResource(id = R.string.login_google),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        TextButton(
                            onClick = onRegisterClick,
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                text = buildAnnotatedString {
                                    withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant)) {
                                        append(stringResource(id = R.string.welcome_new_here))
                                    }
                                    append(" ")
                                    withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)) {
                                        append(stringResource(id = R.string.welcome_sign_up))
                                    }
                                },
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AuthInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    icon: ImageVector,
    errorText: String? = null,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onPasswordVisibleChange: (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(text = label) },
            placeholder = {
                if (!isFocused && value.isEmpty()) {
                    Text(
                        text = placeholder,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        fontSize = 14.sp
                    )
                }
            },
            leadingIcon = {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (errorText != null) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(20.dp)
                )
            },
            trailingIcon = {
                if (isPassword && onPasswordVisibleChange != null) {
                    IconButton(onClick = onPasswordVisibleChange) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            isError = errorText != null,
            visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
            shape = RoundedCornerShape(20.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                errorBorderColor = MaterialTheme.colorScheme.error,
                errorLabelColor = MaterialTheme.colorScheme.error
            ),
            interactionSource = interactionSource,
            singleLine = true,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions
        )
        if (errorText != null) {
            Text(
                text = errorText,
                color = MaterialTheme.colorScheme.error,
                fontSize = 11.sp,
                modifier = Modifier.padding(start = 12.dp, top = 2.dp)
            )
        }
    }
}

@AppCombinedPreviews
@Composable
fun LoginScreenPreview() {
    NewStartTheme {
        LoginContent(
            email = "user@example.com",
            onEmailChange = {},
            password = "password",
            onPasswordChange = {},
            passwordVisible = false,
            onPasswordVisibleChange = {},
            emailError = null,
            passwordError = null,
            isLoading = false,
            onLoginClick = {},
            onGoogleLoginClick = {},
            onForgotPasswordClick = {},
            onRegisterClick = {},
            onBackClick = {},
            showLanguagePicker = false,
            onToggleLanguagePicker = {},
            headerGradient = authHeaderGradient(com.example.newstart.ui.theme.AppThemeColor.BLUE)
        )
    }
}
