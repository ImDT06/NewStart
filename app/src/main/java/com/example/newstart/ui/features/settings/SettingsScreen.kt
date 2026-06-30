package com.example.newstart.ui.features.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import com.example.newstart.widget.HabitWidget
import androidx.glance.appwidget.updateAll
import com.example.newstart.widget.HabitWidgetReceiver
import com.example.newstart.widget.WidgetPinnedReceiver
import android.widget.Toast
import kotlinx.coroutines.launch
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.newstart.R
import com.example.newstart.ui.MainViewModel
import com.example.newstart.ui.theme.AppThemeColor
import com.example.newstart.ui.theme.NewStartTheme
import com.example.newstart.ui.theme.ThemeMode
import com.example.newstart.ui.util.AppCombinedPreviews
import com.example.newstart.ui.util.LanguagePickerDialog
import com.example.newstart.ui.features.settings.components.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalFocusManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToSocial: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToAdmin: () -> Unit = {},
    modifier: Modifier = Modifier,
    mainViewModel: MainViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isVietnamese = remember(configuration) { configuration.locales[0].language == "vi" }
    var showLanguagePicker by remember { mutableStateOf(false) }
    var showThemePicker by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showNotificationSettingsDialog by remember { mutableStateOf(false) }

    var deleteAccountStep by remember { mutableStateOf(1) }
    var selectedReason by remember { mutableStateOf("") }
    var emailConfirmationText by remember { mutableStateOf("") }
    var countdownSeconds by remember { mutableStateOf(3) }
    var deleteAccountPasswordText by remember { mutableStateOf("") }

    val sharedPrefs = remember(context) { context.getSharedPreferences("settings", android.content.Context.MODE_PRIVATE) }
    var isStreakWidgetEnabled by remember { mutableStateOf(sharedPrefs.getBoolean("is_streak_widget_enabled", true)) }
    val scope = rememberCoroutineScope()
    var showChangeEmailDialog by remember { mutableStateOf(false) }
    var showBirthdayDialog by remember { mutableStateOf(false) }
    var showChangePasswordBottomSheet by remember { mutableStateOf(false) }


    var emailChangeStep by remember { mutableStateOf(1) }
    var emailPasswordText by remember { mutableStateOf("") }

    val themeMode by mainViewModel.themeMode.collectAsStateWithLifecycle()
    val themeColor by mainViewModel.themeColor.collectAsStateWithLifecycle()
    val avatarUri by mainViewModel.avatarUri.collectAsStateWithLifecycle()
    val currentUser by mainViewModel.currentUser.collectAsStateWithLifecycle()
    val journalCount by mainViewModel.journalCount.collectAsStateWithLifecycle()
    val habitStats by mainViewModel.habitStats.collectAsStateWithLifecycle()
    val isJournalPromptEnabled by mainViewModel.isJournalPromptEnabled.collectAsStateWithLifecycle()
    val isSearchable by mainViewModel.isSearchable.collectAsStateWithLifecycle()
    val isHabitNotifEnabled by mainViewModel.isHabitNotificationsEnabled.collectAsStateWithLifecycle()
    val isCommunityNotifEnabled by mainViewModel.isCommunityNotificationsEnabled.collectAsStateWithLifecycle()
    val isAdmin by mainViewModel.isAdmin.collectAsStateWithLifecycle()

    var userEmailText by remember(currentUser) { mutableStateOf(currentUser?.email ?: "") }
    
    val locale = remember(context) { context.resources.configuration.locales[0] }
    val isVi = remember(locale) { locale.language == "vi" }
    
    var birthdayDay by remember { 
        mutableIntStateOf(currentUser?.birthday?.split("/")?.getOrNull(0)?.toIntOrNull() ?: 27) 
    }
    var birthdayMonth by remember { 
        mutableIntStateOf(currentUser?.birthday?.split("/")?.getOrNull(1)?.toIntOrNull() ?: 6) 
    } // 1-12
    
    // Đồng bộ lại khi currentUser load xong
    LaunchedEffect(currentUser?.birthday) {
        currentUser?.birthday?.split("/")?.let { parts ->
            if (parts.size == 2) {
                parts[0].toIntOrNull()?.let { birthdayDay = it }
                parts[1].toIntOrNull()?.let { birthdayMonth = it }
            }
        }
    }
    
    val monthsList = listOf(
        "Tháng 1", "Tháng 2", "Tháng 3", "Tháng 4", 
        "Tháng 5", "Tháng 6", "Tháng 7", "Tháng 8", 
        "Tháng 9", "Tháng 10", "Tháng 11", "Tháng 12"
    )
    val monthsListEn = listOf(
        "January", "February", "March", "April", 
        "May", "June", "July", "August", 
        "September", "October", "November", "December"
    )
    
    val birthdayText = remember(birthdayDay, birthdayMonth, isVi) {
        if (isVi) {
            "$birthdayDay ${monthsList[birthdayMonth - 1]}"
        } else {
            "${monthsListEn[birthdayMonth - 1]} $birthdayDay"
        }
    }

    val requestPinWidget = {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val myProvider = ComponentName(context, HabitWidgetReceiver::class.java)
        if (appWidgetManager.isRequestPinAppWidgetSupported) {
            try {
                val successIntent = Intent(context, WidgetPinnedReceiver::class.java)
                val successCallback = android.app.PendingIntent.getBroadcast(
                    context,
                    0,
                    successIntent,
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_MUTABLE
                )
                appWidgetManager.requestPinAppWidget(myProvider, null, successCallback)
            } catch (e: Exception) {
                Toast.makeText(context, "Lỗi ghim tiện ích: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(context, "Thiết bị hoặc Launcher không hỗ trợ tự động ghim tiện ích", Toast.LENGTH_SHORT).show()
        }
    }

    val isDark = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    LaunchedEffect(showDeleteAccountDialog) {
        if (!showDeleteAccountDialog) {
            deleteAccountStep = 1
            selectedReason = ""
            emailConfirmationText = ""
            deleteAccountPasswordText = ""
            countdownSeconds = 3
        }
    }

    LaunchedEffect(deleteAccountStep) {
        if (deleteAccountStep == 4) {
            countdownSeconds = 3
            while (countdownSeconds > 0) {
                kotlinx.coroutines.delay(1000L)
                countdownSeconds--
            }
        }
    }

    LaunchedEffect(currentUser) {
        currentUser?.email?.let { userEmailText = it }
    }

    LaunchedEffect(showChangeEmailDialog) {
        if (!showChangeEmailDialog) {
            emailChangeStep = 1
            emailPasswordText = ""
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            mainViewModel.setAvatarUri(uri)
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text(stringResource(id = R.string.settings_confirm_logout_title)) },
            text = { Text(stringResource(id = R.string.settings_confirm_logout_msg)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        mainViewModel.logout()
                        showLogoutDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(id = R.string.settings_logout), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text(stringResource(id = R.string.settings_cancel))
                }
            }
        )
    }

    if (showDeleteAccountDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAccountDialog = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            confirmButton = {},
            title = {
                Text(
                    text = when (deleteAccountStep) {
                        1 -> "Tại sao bạn rời đi?"
                        2 -> "Xác nhận địa chỉ email"
                        3 -> "Xác thực mật khẩu"
                        else -> "Cảnh báo bảo mật cuối cùng"
                    },
                    fontWeight = FontWeight.ExtraBold,
                    color = if (deleteAccountStep == 4) Color.Red else MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    when (deleteAccountStep) {
                        1 -> {
                            Text(
                                "Vui lòng chọn lý do bạn muốn xóa tài khoản:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val reasons = listOf(
                                "Ứng dụng không còn hữu ích",
                                "Tôi muốn bắt đầu lại tài khoản mới",
                                "Lo ngại về vấn đề quyền riêng tư",
                                "Lý do khác"
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                reasons.forEach { reason ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { selectedReason = reason }
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = selectedReason == reason,
                                            onClick = { selectedReason = reason },
                                            colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = reason,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                TextButton(
                                    onClick = { showDeleteAccountDialog = false },
                                    modifier = Modifier.weight(1f).height(48.dp)
                                ) {
                                    Text("Hủy", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Button(
                                    onClick = { deleteAccountStep = 2 },
                                    enabled = selectedReason.isNotEmpty(),
                                    modifier = Modifier.weight(1f).height(48.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Tiếp tục", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        2 -> {
                            Text(
                                text = "Nhập chính xác email bên dưới để xác nhận:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val userEmail = currentUser?.email ?: ""
                            Text(
                                text = userEmail,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            OutlinedTextField(
                                value = emailConfirmationText,
                                onValueChange = { emailConfirmationText = it },
                                placeholder = { Text("Nhập lại địa chỉ email") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                TextButton(
                                    onClick = { deleteAccountStep = 1 },
                                    modifier = Modifier.weight(1f).height(48.dp)
                                ) {
                                    Text("Quay lại", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Button(
                                    onClick = { deleteAccountStep = 3 },
                                    enabled = emailConfirmationText.trim() == userEmail.trim() && userEmail.isNotEmpty(),
                                    modifier = Modifier.weight(1f).height(48.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Tiếp tục", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        3 -> {
                            Text(
                                "Vui lòng nhập mật khẩu tài khoản của bạn để tiếp tục.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                            TextField(
                                value = deleteAccountPasswordText,
                                onValueChange = { deleteAccountPasswordText = it },
                                placeholder = { Text("Mật khẩu", color = Color.Gray) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color(0xFF252528),
                                    unfocusedContainerColor = Color(0xFF252528),
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                singleLine = true,
                                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                TextButton(
                                    onClick = { deleteAccountStep = 2 },
                                    modifier = Modifier.weight(1f).height(48.dp)
                                ) {
                                    Text("Quay lại", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Button(
                                    onClick = { deleteAccountStep = 4 },
                                    enabled = deleteAccountPasswordText.isNotBlank(),
                                    modifier = Modifier.weight(1f).height(48.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Tiếp tục", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        else -> {
                            Text(
                                text = "LƯU Ý: Hành động này sẽ xóa vĩnh viễn toàn bộ nhật ký của bạn ($journalCount mục), cùng với tất cả dữ liệu thói quen và công việc. Bạn không thể hoàn tác.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Red,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Button(
                                    onClick = {
                                        mainViewModel.logout()
                                        showDeleteAccountDialog = false
                                    },
                                    enabled = countdownSeconds <= 0,
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.Red,
                                        contentColor = Color.White,
                                        disabledContainerColor = Color.Red.copy(alpha = 0.3f),
                                        disabledContentColor = Color.White.copy(alpha = 0.6f)
                                    )
                                ) {
                                    Text(
                                        text = if (countdownSeconds > 0) "Chờ (${countdownSeconds}s)" else "Xóa tài khoản vĩnh viễn",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                TextButton(
                                    onClick = { deleteAccountStep = 3 },
                                    modifier = Modifier.fillMaxWidth().height(48.dp)
                                ) {
                                    Text("Quay lại", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        text = "Thông tin cá nhân",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 22.sp
                    ) 
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        val background = MaterialTheme.colorScheme.background
        val primaryContainer = MaterialTheme.colorScheme.primaryContainer
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = remember(isDark, background, primaryContainer) {
                        Brush.verticalGradient(
                            colors = if (isDark) {
                                listOf(Color(0xFF001A33), background)
                            } else {
                                listOf(primaryContainer.copy(alpha = 0.5f), background)
                            }
                        )
                    }
                )
        ) {
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(bottom = 32.dp, start = 20.dp, end = 20.dp)
            ) {
                // Profile Header Card
                item {
                    ProfileHeaderCard(
                        name = currentUser?.name ?: stringResource(id = R.string.social_default_user_name),
                        email = currentUser?.email ?: "",
                        avatarUri = avatarUri,
                        onEditAvatar = { galleryLauncher.launch("image/*") }
                    )
                }

                // Stats Section
                item {
                    QuickStatsRow(
                        journalCount = journalCount,
                        completionPercent = habitStats.first,
                        streakDays = habitStats.second
                    )
                }

                // 1. Thiết lập Tiện ích (Widget Settings)
                item { SectionTitle(title = stringResource(id = R.string.settings_widget_section)) }
                item {
                    SettingsCard {
                        SettingsItem(
                            icon = Icons.Default.AddBox,
                            title = stringResource(id = R.string.settings_add_widget),
                            subtitle = stringResource(id = R.string.settings_add_widget_desc),
                            onClick = { requestPinWidget() }
                        )

                        SettingsDivider()
                        SettingsToggleItem(
                            icon = Icons.Default.Whatshot,
                            title = stringResource(id = R.string.settings_widget_streak),
                            checked = isStreakWidgetEnabled,
                            onCheckedChange = {
                                isStreakWidgetEnabled = it
                                sharedPrefs.edit().putBoolean("is_streak_widget_enabled", it).apply()
                                scope.launch {
                                    try {
                                        HabitWidget().updateAll(context)
                                    } catch (e: Exception) {
                                        android.util.Log.e("SettingsScreen", "Error updating widget: ${e.message}")
                                    }
                                }
                            }
                        )
                    }
                }

                // 2. Tổng quát (General)
                item { SectionTitle(title = stringResource(id = R.string.settings_general_section)) }
                item {
                    SettingsCard {
                        SettingsItem(
                            icon = Icons.Default.Notifications,
                            title = stringResource(id = R.string.settings_notifications),
                            subtitle = if (isVi) "Quản lý các loại thông báo" else "Manage notification types",
                            onClick = { showNotificationSettingsDialog = true }
                        )
                        SettingsDivider()
                        SettingsItem(
                            icon = Icons.Default.Person,
                            title = stringResource(id = R.string.settings_edit_name),
                            subtitle = currentUser?.name ?: stringResource(id = R.string.social_default_user_name),
                            onClick = { showEditProfileDialog = true }
                        )
                        SettingsDivider()
                        SettingsItem(
                            icon = Icons.Default.Email,
                            title = stringResource(id = R.string.settings_change_email),
                            subtitle = userEmailText,
                            onClick = { showChangeEmailDialog = true }
                        )
                        SettingsDivider()
                        SettingsItem(
                            icon = Icons.Default.Cake,
                            title = stringResource(id = R.string.settings_edit_birthday),
                            subtitle = birthdayText,
                            onClick = { showBirthdayDialog = true }
                        )
                        SettingsDivider()
                        SettingsItem(
                            icon = Icons.Default.VpnKey,
                            title = if (isVi) "Đổi mật khẩu" else "Change Password",
                            subtitle = if (isVi) "Cập nhật mật khẩu tài khoản" else "Update account password",
                            onClick = { showChangePasswordBottomSheet = true }
                        )
                    }
                }

                // 3. Cộng đồng & Tùy chọn (Community & Preferences)
                item { SectionTitle(title = stringResource(id = R.string.settings_community_preferences)) }
                item {
                    SettingsCard {
                        SettingsItem(
                            icon = Icons.Default.Language,
                            titleRes = R.string.settings_language,
                            subtitle = if (isVietnamese) "Tiếng Việt" else "English",
                            onClick = { showLanguagePicker = true }
                        )
                        SettingsDivider()
                        SettingsItem(
                            icon = when (themeMode) {
                                ThemeMode.LIGHT -> Icons.Default.LightMode
                                ThemeMode.DARK -> Icons.Default.DarkMode
                                ThemeMode.SYSTEM -> Icons.Default.SettingsBrightness
                            },
                            title = stringResource(id = R.string.settings_theme_display),
                            subtitle = stringResource(
                                id = when (themeMode) {
                                    ThemeMode.LIGHT -> R.string.settings_theme_light
                                    ThemeMode.DARK -> R.string.settings_theme_dark
                                    ThemeMode.SYSTEM -> R.string.settings_theme_system
                                }
                            ),
                            onClick = { showThemePicker = true }
                        )
                        SettingsDivider()
                        SettingsItem(
                            icon = Icons.Default.Palette,
                            title = stringResource(id = R.string.settings_color_select),
                            subtitle = when (themeColor) {
                                AppThemeColor.BLUE -> "Ocean Blue"
                                AppThemeColor.ROYAL_GREEN -> "Deep Emerald"
                                AppThemeColor.RED -> "Sunset Crimson"
                                AppThemeColor.BLACK -> "Carbon Black"
                            },
                            onClick = { showColorPicker = true }
                        )
                        SettingsDivider()
                        SettingsToggleItem(
                            icon = Icons.Default.EditNote,
                            title = stringResource(id = R.string.settings_journal_prompt),
                            checked = isJournalPromptEnabled,
                            onCheckedChange = { mainViewModel.setJournalPromptEnabled(it) }
                        )
                    }
                }

                // 4. Riêng tư & bảo mật
                item { SectionTitle(title = stringResource(id = R.string.settings_privacy_security)) }
                item {
                    SettingsCard {
                        SettingsToggleItem(
                            icon = Icons.Default.Lock,
                            title = stringResource(id = R.string.settings_allow_search),
                            subtitle = stringResource(id = R.string.settings_allow_search_desc),
                            checked = isSearchable,
                            onCheckedChange = { mainViewModel.setSearchable(it) }
                        )
                    }
                }

                // 4.5. Admin Section
                if (isAdmin) {
                    item { SectionTitle(title = if (isVi) "Quản trị hệ thống" else "Administration") }
                    item {
                        SettingsCard {
                            SettingsItem(
                                icon = Icons.Default.AdminPanelSettings,
                                title = if (isVi) "Bảng quản trị" else "Admin Dashboard",
                                subtitle = if (isVi) "Quản lý bài đăng và tài khoản người dùng" else "Manage community posts and user accounts",
                                onClick = onNavigateToAdmin
                            )
                        }
                    }
                }

                // 5. Vùng nguy hiểm (Danger Zone)
                item { SectionTitle(title = stringResource(id = R.string.settings_danger_zone)) }
                item {
                    SettingsCard {
                        SettingsItem(
                            icon = Icons.Default.DeleteForever,
                            title = stringResource(id = R.string.settings_delete_account),
                            subtitle = stringResource(id = R.string.settings_delete_account_desc),
                            isDestructive = true,
                            onClick = { showDeleteAccountDialog = true }
                        )
                        SettingsDivider()
                        SettingsItem(
                            icon = Icons.AutoMirrored.Filled.Logout,
                            title = stringResource(id = R.string.settings_logout),
                            subtitle = currentUser?.email ?: "",
                            onClick = { showLogoutDialog = true }
                        )
                    }
                }
            }
        }
    }

    if (showLanguagePicker) {
        LanguagePickerDialog(onDismiss = { showLanguagePicker = false })
    }

    if (showThemePicker) {
        ThemeSelectionDialog(
            currentMode = themeMode,
            onModeSelected = {
                mainViewModel.setThemeMode(it)
                showThemePicker = false
            },
            onDismiss = { showThemePicker = false }
        )
    }

    if (showColorPicker) {
        ColorSelectionDialog(
            currentColor = themeColor,
            onColorSelected = {
                mainViewModel.setThemeColor(it)
                showColorPicker = false
            },
            onDismiss = { showColorPicker = false }
        )
    }

    if (showNotificationSettingsDialog) {
        NotificationSettingsDialog(
            isHabitEnabled = isHabitNotifEnabled,
            isCommunityEnabled = isCommunityNotifEnabled,
            isVi = isVi,
            onHabitToggle = { mainViewModel.setHabitNotificationsEnabled(it) },
            onCommunityToggle = { mainViewModel.setCommunityNotificationsEnabled(it) },
            onDismiss = { showNotificationSettingsDialog = false }
        )
    }

    if (showEditProfileDialog) {
        EditProfileDialog(
            currentName = currentUser?.name ?: "",
            onConfirm = { 
                mainViewModel.updateProfileName(it)
                showEditProfileDialog = false 
            },
            onDismiss = { showEditProfileDialog = false }
        )
    }

    if (showChangeEmailDialog) {
        var tempEmail by remember { mutableStateOf(userEmailText) }
        val isDark = isSystemInDarkTheme()
        
        val emailSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showChangeEmailDialog = false },
            sheetState = emailSheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            val focusManager = LocalFocusManager.current
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { focusManager.clearFocus() }
                    )
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 36.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = if (emailChangeStep == 1) "Xác thực bảo mật" else "Địa chỉ email mới",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                if (emailChangeStep == 1) {
                    Text(
                        "Vui lòng nhập mật khẩu tài khoản của bạn để tiếp tục.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    
                    TextField(
                        value = emailPasswordText,
                        onValueChange = { emailPasswordText = it },
                        placeholder = { Text("Mật khẩu", color = Color.Gray) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        ),
                        singleLine = true,
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TextButton(
                            onClick = { showChangeEmailDialog = false },
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Text("Hủy", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Button(
                            onClick = { emailChangeStep = 2 },
                            enabled = emailPasswordText.isNotBlank(),
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(25.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f)
                            )
                        ) {
                            Text("Xác nhận", fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Text(
                        "Nhập địa chỉ email mới của bạn dưới đây:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    
                    TextField(
                        value = tempEmail,
                        onValueChange = { tempEmail = it },
                        placeholder = { Text("Địa chỉ email", color = Color.Gray) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        ),
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TextButton(
                            onClick = { emailChangeStep = 1 },
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Text("Quay lại", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Button(
                            onClick = {
                                mainViewModel.updateEmail(tempEmail)
                                showChangeEmailDialog = false
                            },
                            enabled = tempEmail.isNotBlank() && tempEmail != userEmailText,
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(25.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f)
                            )
                        ) {
                            Text("Lưu", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (showBirthdayDialog) {
        var tempDay by remember { mutableStateOf(birthdayDay) }
        var tempMonth by remember { mutableStateOf(birthdayMonth) }
        val isDark = isSystemInDarkTheme()
        
        // 0: Main form, 1: Month Selector, 2: Day Selector
        var activePickerState by remember { mutableStateOf(0) }
        
        val maxDays = when (tempMonth) {
            2 -> 29
            4, 6, 9, 11 -> 30
            else -> 31
        }
        val daysList = (1..maxDays).toList()
        
        ModalBottomSheet(
            onDismissRequest = { 
                showBirthdayDialog = false 
                activePickerState = 0
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            AnimatedContent(
                targetState = activePickerState,
                label = "birthday_picker_transition"
            ) { state ->
                when (state) {
                    0 -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .padding(horizontal = 24.dp)
                                .padding(bottom = 36.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = if (isVi) "Khi nào là sinh nhật của bạn?" else "When is your birthday?",
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isVi) "Hãy cho chúng tôi biết để cùng chúc mừng! 🥳" else "Let us know so we can celebrate! 🥳",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Chọn Tháng
                                Surface(
                                    onClick = { activePickerState = 1 },
                                    modifier = Modifier.weight(1f).height(56.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                                ) {
                                    Box(contentAlignment = Alignment.CenterStart, modifier = Modifier.padding(horizontal = 16.dp)) {
                                        Column {
                                            Text(if (isVi) "Tháng" else "Month", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = if (isVi) monthsList[tempMonth - 1] else monthsListEn[tempMonth - 1], 
                                                style = MaterialTheme.typography.bodyMedium, 
                                                color = MaterialTheme.colorScheme.onSurface, 
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                                
                                // Chọn Ngày
                                Surface(
                                    onClick = { activePickerState = 2 },
                                    modifier = Modifier.weight(1f).height(56.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                                ) {
                                    Box(contentAlignment = Alignment.CenterStart, modifier = Modifier.padding(horizontal = 16.dp)) {
                                        Column {
                                            Text(if (isVi) "Ngày" else "Day", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(tempDay.toString(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Button(
                                onClick = {
                                    birthdayDay = tempDay
                                    birthdayMonth = tempMonth
                                    mainViewModel.updateBirthday("$tempDay/$tempMonth")
                                    showBirthdayDialog = false
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                shape = RoundedCornerShape(25.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary,
                                    disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                    disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f)
                                )
                            ) {
                                Text(if (isVi) "Lưu" else "Save", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                    }
                    1 -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .padding(horizontal = 24.dp)
                                .padding(bottom = 36.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (isVi) "Tháng" else "Month",
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                            
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 280.dp)
                            ) {
                                items(12) { index ->
                                    val monthVal = index + 1
                                    val isSelected = monthVal == tempMonth
                                    val displayName = if (isVi) monthsList[index] else monthsListEn[index]
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                tempMonth = monthVal
                                                val newMaxDays = when (monthVal) {
                                                    2 -> 29
                                                    4, 6, 9, 11 -> 30
                                                    else -> 31
                                                }
                                                if (tempDay > newMaxDays) {
                                                    tempDay = newMaxDays
                                                }
                                                activePickerState = 0
                                            }
                                            .padding(vertical = 12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = displayName,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            TextButton(onClick = { activePickerState = 0 }) {
                                Text(if (isVi) "Quay lại" else "Back", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    2 -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .padding(horizontal = 24.dp)
                                .padding(bottom = 36.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (isVi) "Ngày" else "Day",
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                            
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 280.dp)
                            ) {
                                items(items = daysList) { day ->
                                    val isSelected = day == tempDay
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                tempDay = day
                                                activePickerState = 0
                                            }
                                            .padding(vertical = 12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = day.toString(),
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            TextButton(onClick = { activePickerState = 0 }) {
                                Text(if (isVi) "Quay lại" else "Back", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showChangePasswordBottomSheet) {
        val isDark = isSystemInDarkTheme()
        var currentPasswordText by remember { mutableStateOf("") }
        var newPasswordText by remember { mutableStateOf("") }
        var confirmPasswordText by remember { mutableStateOf("") }
        
        var currentPasswordError by remember { mutableStateOf<String?>(null) }
        var newPasswordError by remember { mutableStateOf<String?>(null) }
        var confirmPasswordError by remember { mutableStateOf<String?>(null) }
        
        var isLoading by remember { mutableStateOf(false) }
        val scrollState = rememberScrollState()
        
        val changePasswordSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { 
                showChangePasswordBottomSheet = false 
                currentPasswordText = ""
                newPasswordText = ""
                confirmPasswordText = ""
                currentPasswordError = null
                newPasswordError = null
                confirmPasswordError = null
            },
            sheetState = changePasswordSheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            val focusManager = LocalFocusManager.current
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .verticalScroll(scrollState)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { focusManager.clearFocus() }
                    )
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 36.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = if (isVi) "Đổi mật khẩu" else "Change Password",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                // Mật khẩu hiện tại
                OutlinedTextField(
                    value = currentPasswordText,
                    onValueChange = { 
                        currentPasswordText = it
                        currentPasswordError = null
                    },
                    placeholder = { Text(if (isVi) "Mật khẩu hiện tại" else "Current Password", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    isError = currentPasswordError != null,
                    supportingText = currentPasswordError?.let { { Text(it, color = Color(0xFFE53935)) } },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        errorContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        errorBorderColor = Color(0xFFE53935),
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        errorTextColor = MaterialTheme.colorScheme.onSurface
                    ),
                    singleLine = true,
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
                )
                
                // Mật khẩu mới
                OutlinedTextField(
                    value = newPasswordText,
                    onValueChange = { 
                        newPasswordText = it
                        newPasswordError = null
                    },
                    placeholder = { Text(if (isVi) "Mật khẩu mới (tối thiểu 6 ký tự)" else "New Password (min 6 chars)", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    isError = newPasswordError != null,
                    supportingText = newPasswordError?.let { { Text(it, color = Color(0xFFE53935)) } },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        errorContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        errorBorderColor = Color(0xFFE53935),
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        errorTextColor = MaterialTheme.colorScheme.onSurface
                    ),
                    singleLine = true,
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
                )
                
                // Xác nhận mật khẩu mới
                OutlinedTextField(
                    value = confirmPasswordText,
                    onValueChange = { 
                        confirmPasswordText = it
                        confirmPasswordError = null
                    },
                    placeholder = { Text(if (isVi) "Xác nhận mật khẩu mới" else "Confirm New Password", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    isError = confirmPasswordError != null,
                    supportingText = confirmPasswordError?.let { { Text(it, color = Color(0xFFE53935)) } },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        errorContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        errorBorderColor = Color(0xFFE53935),
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        errorTextColor = MaterialTheme.colorScheme.onSurface
                    ),
                    singleLine = true,
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        onClick = { 
                            showChangePasswordBottomSheet = false 
                            currentPasswordText = ""
                            newPasswordText = ""
                            confirmPasswordText = ""
                            currentPasswordError = null
                            newPasswordError = null
                            confirmPasswordError = null
                        },
                        enabled = !isLoading,
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text(if (isVi) "Hủy" else "Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Button(
                        onClick = {
                            if (newPasswordText.length < 6) {
                                newPasswordError = if (isVi) "Mật khẩu mới phải từ 6 ký tự trở lên" else "New password must be at least 6 characters"
                                return@Button
                            }
                            if (newPasswordText != confirmPasswordText) {
                                confirmPasswordError = if (isVi) "Mật khẩu xác nhận không khớp" else "Confirm password does not match"
                                return@Button
                            }
                            isLoading = true
                            val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                            if (user != null && user.email != null) {
                                val credential = com.google.firebase.auth.EmailAuthProvider.getCredential(user.email!!, currentPasswordText)
                                user.reauthenticate(credential).addOnCompleteListener { reauthTask ->
                                    if (reauthTask.isSuccessful) {
                                        user.updatePassword(newPasswordText).addOnCompleteListener { updateTask ->
                                            isLoading = false
                                            if (updateTask.isSuccessful) {
                                                Toast.makeText(context, if (isVi) "Đổi mật khẩu thành công!" else "Password updated successfully!", Toast.LENGTH_SHORT).show()
                                                showChangePasswordBottomSheet = false
                                                currentPasswordText = ""
                                                newPasswordText = ""
                                                confirmPasswordText = ""
                                            } else {
                                                newPasswordError = updateTask.exception?.localizedMessage ?: (if (isVi) "Đổi mật khẩu thất bại" else "Failed to update password")
                                            }
                                        }
                                    } else {
                                        isLoading = false
                                        currentPasswordError = if (isVi) "Mật khẩu hiện tại không chính xác" else "Incorrect current password"
                                    }
                                }
                            } else {
                                isLoading = false
                                Toast.makeText(context, if (isVi) "Không tìm thấy thông tin người dùng" else "User info not found", Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = !isLoading && currentPasswordText.isNotBlank() && newPasswordText.isNotBlank() && confirmPasswordText.isNotBlank(),
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(25.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                            disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f)
                        )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                        } else {
                            Text(if (isVi) "Lưu" else "Save", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@AppCombinedPreviews
@Composable
fun SettingsScreenPreview() {
    NewStartTheme {
        SettingsScreen(onNavigateToSocial = {}, onNavigateToHome = {})
    }
}
