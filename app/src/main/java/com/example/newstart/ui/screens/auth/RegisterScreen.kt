package com.example.newstart.ui.screens.auth

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
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.newstart.R
import com.example.newstart.ui.MainViewModel
import com.example.newstart.ui.theme.NewStartTheme
import com.example.newstart.ui.theme.authHeaderGradient
import com.example.newstart.ui.util.AppCombinedPreviews
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.newstart.ui.util.LanguagePickerDialog
import com.example.newstart.ui.util.TransparentLanguageSwitcher

@Composable
fun RegisterScreen(
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit = {},
    onNavigateToLogin: () -> Unit = {},
    onRegisterSuccess: () -> Unit = {},
    viewModel: RegisterViewModel = hiltViewModel(),
    mainViewModel: MainViewModel = viewModel()
) {
    val context = LocalContext.current
    var showLanguagePicker by remember { mutableStateOf(false) }

    RegisterContent(
        fullName = viewModel.fullName,
        onFullNameChange = viewModel::onFullNameChange,
        email = viewModel.email,
        onEmailChange = viewModel::onEmailChange,
        password = viewModel.password,
        onPasswordChange = viewModel::onPasswordChange,
        passwordVisible = viewModel.passwordVisible,
        onPasswordVisibleChange = viewModel::togglePasswordVisibility,
        confirmPassword = viewModel.confirmPassword,
        onConfirmPasswordChange = viewModel::onConfirmPasswordChange,
        confirmPasswordVisible = viewModel.confirmPasswordVisible,
        onConfirmPasswordVisibleChange = viewModel::toggleConfirmPasswordVisibility,
        acceptTerms = viewModel.acceptTerms,
        onAcceptTermsChange = viewModel::onAcceptTermsChange,
        fullNameError = viewModel.fullNameError,
        emailError = viewModel.emailError,
        passwordError = viewModel.passwordError,
        confirmPasswordError = viewModel.confirmPasswordError,
        isLoading = viewModel.isLoading,
        onSignUpClick = {
            viewModel.register(
                onSuccess = {
                    Toast.makeText(context, "Đăng ký thành công! Vui lòng kiểm tra Email để xác thực tài khoản.", Toast.LENGTH_LONG).show()
                    onRegisterSuccess()
                },
                onError = { error ->
                    Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                }
            )
        },
        onLoginNowClick = onNavigateToLogin,
        onBackClick = onNavigateBack,
        showLanguagePicker = showLanguagePicker,
        onToggleLanguagePicker = { showLanguagePicker = it },
        modifier = modifier,
        headerGradient = authHeaderGradient(mainViewModel)
    )
}

@Composable
fun RegisterContent(
    fullName: String,
    onFullNameChange: (String) -> Unit,
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    passwordVisible: Boolean,
    onPasswordVisibleChange: () -> Unit,
    confirmPassword: String,
    onConfirmPasswordChange: (String) -> Unit,
    confirmPasswordVisible: Boolean,
    onConfirmPasswordVisibleChange: () -> Unit,
    acceptTerms: Boolean,
    onAcceptTermsChange: (Boolean) -> Unit,
    fullNameError: String?,
    emailError: String?,
    passwordError: String?,
    confirmPasswordError: String?,
    isLoading: Boolean,
    onSignUpClick: () -> Unit,
    onLoginNowClick: () -> Unit,
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
                    .height(180.dp)
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
                            tint = Color.White,
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
                        .padding(start = 32.dp, end = 32.dp, bottom = 40.dp),
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Text(
                        text = stringResource(id = R.string.register_title),
                        color = Color.White,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }

            // Form Section
            Column(
                modifier = Modifier
                    .padding(top = 155.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    RegisterInputField(
                        value = fullName,
                        onValueChange = onFullNameChange,
                        label = stringResource(id = R.string.register_label_name),
                        placeholder = stringResource(id = R.string.register_placeholder_name),
                        errorText = fullNameError,
                        icon = Icons.Default.Person,
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Next,
                            keyboardType = KeyboardType.Text
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        )
                    )

                    RegisterInputField(
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

                    RegisterInputField(
                        value = password,
                        onValueChange = onPasswordChange,
                        label = stringResource(id = R.string.register_label_password),
                        placeholder = stringResource(id = R.string.register_placeholder_password),
                        errorText = passwordError,
                        icon = Icons.Default.Lock,
                        isPassword = true,
                        passwordVisible = passwordVisible,
                        onPasswordVisibleChange = onPasswordVisibleChange,
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Next,
                            keyboardType = KeyboardType.Password
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        )
                    )

                    RegisterInputField(
                        value = confirmPassword,
                        onValueChange = onConfirmPasswordChange,
                        label = stringResource(id = R.string.register_label_confirm),
                        placeholder = stringResource(id = R.string.register_placeholder_confirm),
                        errorText = confirmPasswordError,
                        icon = Icons.Default.Lock,
                        isPassword = true,
                        passwordVisible = confirmPasswordVisible,
                        onPasswordVisibleChange = onConfirmPasswordVisibleChange,
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Done,
                            keyboardType = KeyboardType.Password
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                onSignUpClick()
                            }
                        )
                    )

                    Text(
                        text = buildAnnotatedString {
                            withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant)) {
                                append(stringResource(id = R.string.register_terms_prefix))
                            }
                            withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)) {
                                append(stringResource(id = R.string.register_terms_service))
                            }
                            withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant)) {
                                append(stringResource(id = R.string.register_terms_and))
                            }
                            withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)) {
                                append(stringResource(id = R.string.register_terms_privacy))
                            }
                        },
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier
                            .padding(horizontal = 4.dp, vertical = 8.dp)
                            .clickable { onAcceptTermsChange(!acceptTerms) },
                        textAlign = TextAlign.Start
                    )
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(
                        onClick = onSignUpClick,
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Text(
                                text = stringResource(id = R.string.register_btn_now),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    TextButton(
                        onClick = onLoginNowClick,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = buildAnnotatedString {
                                withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant)) {
                                    append(stringResource(id = R.string.register_already_member))
                                }
                                append(" ")
                                withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)) {
                                    append(stringResource(id = R.string.register_login_now))
                                }
                            },
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RegisterInputField(
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
fun RegisterScreenPreview() {
    NewStartTheme {
        RegisterContent(
            fullName = "John Doe",
            onFullNameChange = {},
            email = "john.doe@example.com",
            onEmailChange = {},
            password = "password123",
            onPasswordChange = {},
            passwordVisible = false,
            onPasswordVisibleChange = {},
            confirmPassword = "password123",
            onConfirmPasswordChange = {},
            confirmPasswordVisible = false,
            onConfirmPasswordVisibleChange = {},
            acceptTerms = true,
            onAcceptTermsChange = {},
            fullNameError = null,
            emailError = null,
            passwordError = null,
            confirmPasswordError = null,
            isLoading = false,
            onSignUpClick = {},
            onLoginNowClick = {},
            onBackClick = {},
            showLanguagePicker = false,
            onToggleLanguagePicker = {},
            headerGradient = authHeaderGradient(com.example.newstart.ui.theme.AppThemeColor.BLUE)
        )
    }
}
