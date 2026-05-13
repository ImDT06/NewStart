package com.example.newstart.ui.screens.auth

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.KeyboardArrowDown
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.example.newstart.R
import com.example.newstart.ui.theme.NewStartTheme
import com.example.newstart.ui.util.LanguagePreviews
import com.example.newstart.ui.util.LanguagePickerDialog
import com.example.newstart.ui.util.SmallLanguageSwitcher

private val RegPrimaryBlue = Color(0xFF1D5FE2)
private val RegTextGrey = Color(0xFF6B7280)
private val RegFieldBorder = Color(0xFFE5E7EB)
private val RegIconGrey = Color(0xFF9CA3AF)
private val RegWaveBlue = Color(0xFF0036D6)

@Composable
fun RegisterScreen(
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit = {},
    onRegisterSuccess: () -> Unit = {},
    viewModel: RegisterViewModel = hiltViewModel(),
) {
    var showLanguagePicker by remember { mutableStateOf(false) }

    RegisterContent(
        fullName = viewModel.fullName,
        onFullNameChange = viewModel::onFullNameChange,
        email = viewModel.email,
        onEmailChange = viewModel::onEmailChange,
        mobile = viewModel.mobile,
        onMobileChange = viewModel::onMobileChange,
        password = viewModel.password,
        onPasswordChange = viewModel::onPasswordChange,
        confirmPassword = viewModel.confirmPassword,
        onConfirmPasswordChange = viewModel::onConfirmPasswordChange,
        acceptTerms = viewModel.acceptTerms,
        onAcceptTermsChange = viewModel::onAcceptTermsChange,
        fullNameError = viewModel.fullNameError,
        emailError = viewModel.emailError,
        mobileError = viewModel.mobileError,
        passwordError = viewModel.passwordError,
        confirmPasswordError = viewModel.confirmPasswordError,
        onSignUpClick = {
            if (viewModel.validateAndRegister()) {
                onRegisterSuccess()
            }
        },
        onLoginNowClick = {
            onRegisterSuccess()
        },
        onBackClick = onNavigateBack,
        showLanguagePicker = showLanguagePicker,
        onToggleLanguagePicker = { showLanguagePicker = it },
        modifier = modifier,
    )
}

@Composable
fun RegisterContent(
    fullName: String,
    onFullNameChange: (String) -> Unit,
    email: String,
    onEmailChange: (String) -> Unit,
    mobile: String,
    onMobileChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    confirmPassword: String,
    onConfirmPasswordChange: (String) -> Unit,
    acceptTerms: Boolean,
    onAcceptTermsChange: (Boolean) -> Unit,
    fullNameError: String?,
    emailError: String?,
    mobileError: String?,
    passwordError: String?,
    confirmPasswordError: String?,
    onSignUpClick: () -> Unit,
    onLoginNowClick: () -> Unit,
    onBackClick: () -> Unit,
    showLanguagePicker: Boolean,
    onToggleLanguagePicker: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    focusManager.clearFocus()
                })
            }
            .verticalScroll(scrollState)
            .imePadding()
    ) {
        // Cấu trúc Box lồng ghép để tạo hiệu ứng lớp chồng (Layering)
        Box(modifier = Modifier.fillMaxWidth()) {
            // Blue Header Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(RegWaveBlue, RegWaveBlue.copy(alpha = 0.8f))
                        )
                    )
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

                    // Language Switcher
                    SmallLanguageSwitcher(
                        onClick = { onToggleLanguagePicker(true) },
                        tintColor = Color.White
                    )
                }

                if (showLanguagePicker) {
                    LanguagePickerDialog(onDismiss = { onToggleLanguagePicker(false) })
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(start = 32.dp, end = 32.dp, bottom = 40.dp), // Tăng padding để đẩy chữ lên cao hẳn
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Text(
                        text = stringResource(id = R.string.register_title),
                        color = Color.White,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(id = R.string.register_subtitle),
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 14.sp,
                        lineHeight = 18.sp
                    )
                }
            }

            // Form Section
            Column(
                modifier = Modifier
                    .padding(top = 155.dp) // Đẩy điểm bắt đầu xuống 155dp để không đè chữ
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .background(Color.White)
                    .padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                        value = mobile,
                        onValueChange = onMobileChange,
                        label = stringResource(id = R.string.register_label_mobile),
                        placeholder = stringResource(id = R.string.register_placeholder_mobile),
                        errorText = mobileError,
                        icon = Icons.Default.Phone,
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Next,
                            keyboardType = KeyboardType.Phone
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

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = acceptTerms,
                            onCheckedChange = onAcceptTermsChange,
                            colors = CheckboxDefaults.colors(checkedColor = RegPrimaryBlue)
                        )
                        Text(
                            text = buildAnnotatedString {
                                withStyle(style = SpanStyle(color = RegTextGrey)) {
                                    append(stringResource(id = R.string.register_terms_prefix))
                                }
                                withStyle(style = SpanStyle(color = RegPrimaryBlue, fontWeight = FontWeight.Bold)) {
                                    append(stringResource(id = R.string.register_terms_service))
                                }
                                withStyle(style = SpanStyle(color = RegTextGrey)) {
                                    append(stringResource(id = R.string.register_terms_and))
                                }
                                withStyle(style = SpanStyle(color = RegPrimaryBlue, fontWeight = FontWeight.Bold)) {
                                    append(stringResource(id = R.string.register_terms_privacy))
                                }
                            },
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            modifier = Modifier.clickable { onAcceptTermsChange(!acceptTerms) }
                        )
                    }
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(
                        onClick = onSignUpClick,
                        enabled = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RegPrimaryBlue),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.register_btn_now),
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }

                    TextButton(
                        onClick = onLoginNowClick,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = buildAnnotatedString {
                                withStyle(style = SpanStyle(color = RegTextGrey)) {
                                    append(stringResource(id = R.string.register_already_member))
                                }
                                append(" ")
                                withStyle(style = SpanStyle(color = RegPrimaryBlue, fontWeight = FontWeight.Bold)) {
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
                    Text(text = placeholder, color = RegIconGrey, fontSize = 14.sp)
                }
            },
            leadingIcon = {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (errorText != null) MaterialTheme.colorScheme.error else RegIconGrey,
                    modifier = Modifier.size(20.dp)
                )
            },
            isError = errorText != null,
            visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = RegPrimaryBlue,
                unfocusedBorderColor = RegFieldBorder,
                focusedLabelColor = RegPrimaryBlue,
                unfocusedLabelColor = RegIconGrey,
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black,
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

@LanguagePreviews
@Composable
fun RegisterScreenPreview() {
    NewStartTheme {
        RegisterContent(
            fullName = "John Doe",
            onFullNameChange = {},
            email = "john.doe@example.com",
            onEmailChange = {},
            mobile = "0123456789",
            onMobileChange = {},
            password = "password123",
            onPasswordChange = {},
            confirmPassword = "password123",
            onConfirmPasswordChange = {},
            acceptTerms = true,
            onAcceptTermsChange = {},
            fullNameError = null,
            emailError = null,
            mobileError = null,
            passwordError = null,
            confirmPasswordError = null,
            onSignUpClick = {},
            onLoginNowClick = {},
            onBackClick = {},
            showLanguagePicker = false,
            onToggleLanguagePicker = {}
        )
    }
}

@Preview(showBackground = true, name = "Dark Mode", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun RegisterScreenDarkPreview() {
    NewStartTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            RegisterContent(
                fullName = "",
                onFullNameChange = {},
                email = "",
                onEmailChange = {},
                mobile = "",
                onMobileChange = {},
                password = "",
                onPasswordChange = {},
                confirmPassword = "",
                onConfirmPasswordChange = {},
                acceptTerms = false,
                onAcceptTermsChange = {},
                fullNameError = null,
                emailError = null,
                mobileError = null,
                passwordError = null,
                confirmPasswordError = null,
                onSignUpClick = {},
                onLoginNowClick = {},
                onBackClick = {},
                showLanguagePicker = false,
                onToggleLanguagePicker = {}
            )
        }
    }
}
