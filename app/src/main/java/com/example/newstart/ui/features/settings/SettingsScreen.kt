package com.example.newstart.ui.features.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
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
import coil.compose.AsyncImage
import com.example.newstart.R
import com.example.newstart.ui.MainViewModel
import com.example.newstart.ui.theme.AppThemeColor
import com.example.newstart.ui.theme.NewStartTheme
import com.example.newstart.ui.theme.ThemeMode
import com.example.newstart.ui.util.AppCombinedPreviews
import com.example.newstart.ui.util.LanguagePickerDialog

@Composable
fun ProfileHeaderCard(
    name: String,
    email: String,
    avatarUri: Uri?,
    onEditAvatar: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(contentAlignment = Alignment.BottomEnd) {
                Surface(
                    modifier = Modifier.size(64.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (avatarUri != null) {
                            AsyncImage(
                                model = avatarUri,
                                contentDescription = "Avatar",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text(
                                text = name.take(1).uppercase(),
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
                
                Surface(
                    modifier = Modifier
                        .size(24.dp)
                        .clickable(onClick = onEditAvatar),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.surface)
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Edit Avatar",
                        tint = Color.White,
                        modifier = Modifier.padding(4.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column {
                Text(
                    text = name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = email,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "Thành viên Premium",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun QuickStatsRow(
    journalCount: Int,
    completionPercent: Int,
    streakDays: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StatItem(label = "Nhật ký", value = journalCount.toString(), modifier = Modifier.weight(1f))
        StatItem(label = "Thói quen", value = "$completionPercent%", modifier = Modifier.weight(1f))
        StatItem(label = "Chuỗi", value = "$streakDays ngày", modifier = Modifier.weight(1f))
    }
}

@Composable
fun StatItem(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = value, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
            Text(text = label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToSocial: () -> Unit,
    onNavigateToHome: () -> Unit,
    modifier: Modifier = Modifier,
    mainViewModel: MainViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var showLanguagePicker by remember { mutableStateOf(false) }
    var showThemePicker by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var showEditProfileDialog by remember { mutableStateOf(false) }

    var deleteAccountStep by remember { mutableStateOf(1) }
    var selectedReason by remember { mutableStateOf("") }
    var emailConfirmationText by remember { mutableStateOf("") }
    var countdownSeconds by remember { mutableStateOf(3) }

    val sharedPrefs = remember(context) { context.getSharedPreferences("settings", android.content.Context.MODE_PRIVATE) }
    var isStreakWidgetEnabled by remember { mutableStateOf(sharedPrefs.getBoolean("is_streak_widget_enabled", true)) }
    val scope = rememberCoroutineScope()
    var isNotificationEnabled by remember { mutableStateOf(true) }
    var showChangeEmailDialog by remember { mutableStateOf(false) }
    var showBirthdayDialog by remember { mutableStateOf(false) }


    var emailChangeStep by remember { mutableStateOf(1) }
    var emailPasswordText by remember { mutableStateOf("") }

    val themeMode by mainViewModel.themeMode.collectAsStateWithLifecycle()
    val themeColor by mainViewModel.themeColor.collectAsStateWithLifecycle()
    val avatarUri by mainViewModel.avatarUri.collectAsStateWithLifecycle()
    val currentUser by mainViewModel.currentUser.collectAsStateWithLifecycle()
    val journalCount by mainViewModel.journalCount.collectAsStateWithLifecycle()
    val habitStats by mainViewModel.habitStats.collectAsStateWithLifecycle()
    val isJournalPromptEnabled by mainViewModel.isJournalPromptEnabled.collectAsStateWithLifecycle()

    var userEmailText by remember { mutableStateOf(currentUser?.email ?: "tranvanchinh555@gmail.com") }
    var birthdayText by remember { mutableStateOf("27 Tháng 6") }

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
            countdownSeconds = 3
        }
    }

    LaunchedEffect(deleteAccountStep) {
        if (deleteAccountStep == 3) {
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
                        else -> "Cảnh báo bảo mật cuối cùng"
                    },
                    fontWeight = FontWeight.ExtraBold,
                    color = if (deleteAccountStep == 3) Color.Red else MaterialTheme.colorScheme.onSurface
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
                        else -> {
                            Text(
                                text = "LƯU Ý: Hành động này sẽ xóa vĩnh viễn toàn bộ nhật ký của bạn ($journalCount mục), cùng với tất cả dữ liệu thói quen và công việc. Bạn không thể hoàn tác.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Red,
                                fontWeight = FontWeight.Bold
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
                                    onClick = {
                                        mainViewModel.logout()
                                        showDeleteAccountDialog = false
                                    },
                                    enabled = countdownSeconds <= 0,
                                    modifier = Modifier.weight(1f).height(48.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.Red,
                                        contentColor = Color.White,
                                        disabledContainerColor = Color.Red.copy(alpha = 0.3f),
                                        disabledContentColor = Color.White.copy(alpha = 0.6f)
                                    )
                                ) {
                                    Text(
                                        text = if (countdownSeconds > 0) "Chờ (${countdownSeconds}s)" else "Xóa vĩnh viễn",
                                        fontWeight = FontWeight.Bold
                                    )
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
                        name = currentUser?.name ?: "Người dùng",
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
                item { SectionTitle(title = "Thiết lập Tiện ích") }
                item {
                    SettingsCard {
                        SettingsItem(
                            icon = Icons.Default.AddBox,
                            title = "Thêm Tiện ích",
                            subtitle = "Đưa ứng dụng ra màn hình chính",
                            onClick = { requestPinWidget() }
                        )

                        SettingsDivider()
                        SettingsToggleItem(
                            icon = Icons.Default.Whatshot,
                            title = "Chuỗi trên tiện ích",
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
                item { SectionTitle(title = "Tổng quát") }
                item {
                    SettingsCard {
                        SettingsToggleItem(
                            icon = Icons.Default.Notifications,
                            title = "Thông báo",
                            checked = isNotificationEnabled,
                            onCheckedChange = { isNotificationEnabled = it }
                        )
                        SettingsDivider()
                        SettingsItem(
                            icon = Icons.Default.Person,
                            title = "Sửa tên",
                            subtitle = currentUser?.name ?: "Người dùng",
                            onClick = { showEditProfileDialog = true }
                        )
                        SettingsDivider()
                        SettingsItem(
                            icon = Icons.Default.Email,
                            title = "Thay đổi địa chỉ email",
                            subtitle = userEmailText,
                            onClick = { showChangeEmailDialog = true }
                        )
                        SettingsDivider()
                        SettingsItem(
                            icon = Icons.Default.Cake,
                            title = "Ngày sinh (Edit birthday)",
                            subtitle = birthdayText,
                            onClick = { showBirthdayDialog = true }
                        )
                    }
                }

                // 3. Cộng đồng & Tùy chọn (Community & Preferences)
                item { SectionTitle(title = "Cộng đồng & Tùy chọn") }
                item {
                    SettingsCard {
                        SettingsItem(
                            icon = Icons.Default.Group,
                            title = "Bạn bè (Inner Circle)",
                            subtitle = "Kết nối & Chia sẻ tiến độ",
                            onClick = onNavigateToSocial
                        )
                        SettingsDivider()
                        SettingsItem(
                            icon = Icons.Default.Language,
                            titleRes = R.string.settings_language,
                            subtitle = "Tiếng Việt",
                            onClick = { showLanguagePicker = true }
                        )
                        SettingsDivider()
                        SettingsItem(
                            icon = when (themeMode) {
                                ThemeMode.LIGHT -> Icons.Default.LightMode
                                ThemeMode.DARK -> Icons.Default.DarkMode
                                ThemeMode.SYSTEM -> Icons.Default.SettingsBrightness
                            },
                            title = "Chế độ hiển thị",
                            subtitle = when (themeMode) {
                                ThemeMode.LIGHT -> "Sáng"
                                ThemeMode.DARK -> "Tối"
                                ThemeMode.SYSTEM -> "Hệ thống"
                            },
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
                            title = "Gợi ý viết nhật ký",
                            checked = isJournalPromptEnabled,
                            onCheckedChange = { mainViewModel.setJournalPromptEnabled(it) }
                        )
                    }
                }

                // 4. Riêng tư & bảo mật
                item { SectionTitle(title = "Riêng tư & bảo mật") }
                item {
                    SettingsCard {
                        SettingsItem(
                            icon = Icons.Default.Block,
                            title = "Tài khoản bị chặn",
                            onClick = { /* Block list */ }
                        )
                        SettingsDivider()
                        SettingsItem(
                            icon = Icons.Default.Lock,
                            title = "Privacy and Data",
                            onClick = { /* Privacy details */ }
                        )
                    }
                }

                // 5. Vùng nguy hiểm (Danger Zone)
                item { SectionTitle(title = "Vùng nguy hiểm") }
                item {
                    SettingsCard {
                        SettingsItem(
                            icon = Icons.Default.DeleteForever,
                            title = "Xóa tài khoản",
                            subtitle = "Xóa vĩnh viễn toàn bộ dữ liệu",
                            isDestructive = true,
                            onClick = { showDeleteAccountDialog = true }
                        )
                        SettingsDivider()
                        SettingsItem(
                            icon = Icons.AutoMirrored.Filled.Logout,
                            title = "Đăng xuất",
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
        
        AlertDialog(
            onDismissRequest = { showChangeEmailDialog = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            confirmButton = {},
            title = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (emailChangeStep == 1) "Xác thực bảo mật" else "Địa chỉ email mới",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
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
                                    containerColor = Color(0xFFFFB000),
                                    contentColor = Color.Black,
                                    disabledContainerColor = Color(0xFFFFB000).copy(alpha = 0.4f),
                                    disabledContentColor = Color.Black.copy(alpha = 0.5f)
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
                                focusedContainerColor = Color(0xFF252528),
                                unfocusedContainerColor = Color(0xFF252528),
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
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
                                    userEmailText = tempEmail
                                    showChangeEmailDialog = false
                                },
                                enabled = tempEmail.isNotBlank() && tempEmail != userEmailText,
                                modifier = Modifier.weight(1f).height(48.dp),
                                shape = RoundedCornerShape(25.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFFFB000),
                                    contentColor = Color.Black,
                                    disabledContainerColor = Color(0xFFFFB000).copy(alpha = 0.4f),
                                    disabledContentColor = Color.Black.copy(alpha = 0.5f)
                                )
                            ) {
                                Text("Lưu", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        )
    }

    if (showBirthdayDialog) {
        val parts = remember(birthdayText) { birthdayText.split(" ", limit = 2) }
        var tempDay by remember { mutableStateOf(parts.getOrNull(0) ?: "27") }
        var tempMonth by remember { mutableStateOf(parts.getOrNull(1) ?: "Tháng 6") }
        
        AlertDialog(
            onDismissRequest = { showBirthdayDialog = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            confirmButton = {},
            title = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        "When is your birthday?",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Text(
                        "Let us know so we can celebrate! 🥳",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TextField(
                            value = tempMonth,
                            onValueChange = { tempMonth = it },
                            placeholder = { Text("Tháng", color = Color.Gray) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF252528),
                                unfocusedContainerColor = Color(0xFF252528),
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            singleLine = true
                        )
                        
                        TextField(
                            value = tempDay,
                            onValueChange = { tempDay = it },
                            placeholder = { Text("Ngày", color = Color.Gray) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF252528),
                                unfocusedContainerColor = Color(0xFF252528),
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            singleLine = true
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Button(
                        onClick = {
                            birthdayText = "$tempDay $tempMonth"
                            showBirthdayDialog = false
                        },
                        enabled = tempDay.isNotBlank() && tempMonth.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(25.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFFB000),
                            contentColor = Color.Black,
                            disabledContainerColor = Color(0xFFFFB000).copy(alpha = 0.4f),
                            disabledContentColor = Color.Black.copy(alpha = 0.5f)
                        )
                    ) {
                        Text("Lưu", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileDialog(
    currentName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val nameParts = remember(currentName) { currentName.split(" ", limit = 2) }
    var firstName by remember { mutableStateOf(nameParts.getOrNull(0) ?: "") }
    var lastName by remember { mutableStateOf(nameParts.getOrNull(1) ?: "") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        confirmButton = {},
        title = {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("Sửa tên của bạn", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurface)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TextField(
                    value = firstName,
                    onValueChange = { firstName = it },
                    placeholder = { Text("Tên", color = Color.Gray) },
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
                    singleLine = true
                )
                
                TextField(
                    value = lastName,
                    onValueChange = { lastName = it },
                    placeholder = { Text("Họ (không bắt buộc)", color = Color.Gray) },
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
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text("Hủy", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Button(
                        onClick = {
                            val fullName = listOf(firstName, lastName).filter { it.isNotBlank() }.joinToString(" ")
                            onConfirm(fullName)
                        },
                        enabled = firstName.isNotBlank(),
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(25.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFFB000),
                            contentColor = Color.Black,
                            disabledContainerColor = Color(0xFFFFB000).copy(alpha = 0.4f),
                            disabledContentColor = Color.Black.copy(alpha = 0.5f)
                        )
                    ) {
                        Text("Lưu", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    )
}

@Composable
fun ProfileHeader(
    name: String,
    email: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = name.take(1).uppercase(),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        
        Spacer(modifier = Modifier.width(20.dp))
        
        Column {
            Text(
                text = name,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = email,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSelectionDialog(
    currentMode: ThemeMode,
    onModeSelected: (ThemeMode) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 48.dp, start = 24.dp, end = 24.dp, top = 8.dp)
        ) {
            Text(
                text = stringResource(id = R.string.settings_theme_select),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            ThemeOption(
                label = stringResource(id = R.string.settings_theme_light),
                icon = Icons.Default.LightMode,
                isSelected = currentMode == ThemeMode.LIGHT
            ) {
                onModeSelected(ThemeMode.LIGHT)
            }
            ThemeOption(
                label = stringResource(id = R.string.settings_theme_dark),
                icon = Icons.Default.DarkMode,
                isSelected = currentMode == ThemeMode.DARK
            ) {
                onModeSelected(ThemeMode.DARK)
            }
            ThemeOption(
                label = stringResource(id = R.string.settings_theme_system),
                icon = Icons.Default.SettingsBrightness,
                isSelected = currentMode == ThemeMode.SYSTEM
            ) {
                onModeSelected(ThemeMode.SYSTEM)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorSelectionDialog(
    currentColor: AppThemeColor,
    onColorSelected: (AppThemeColor) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedColorState by remember { mutableStateOf(currentColor) }

    val previewColor = when (selectedColorState) {
        AppThemeColor.BLUE -> Color(0xFF1D5FE2)
        AppThemeColor.ROYAL_GREEN -> Color(0xFF006B3F)
        AppThemeColor.RED -> Color(0xFFB91D1D)
        AppThemeColor.BLACK -> Color(0xFF1B1B1F)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 36.dp, start = 24.dp, end = 24.dp, top = 8.dp)
        ) {
            Text(
                text = stringResource(id = R.string.settings_color_select),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Live Preview Card Mockup
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = previewColor.copy(alpha = 0.08f)),
                border = BorderStroke(1.dp, previewColor.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(previewColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Bản xem trước giao diện",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = previewColor
                        )
                        Text(
                            text = "Chủ đề này sẽ thay đổi màu sắc chủ đạo của toàn bộ ứng dụng.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // 2x2 Color Grid
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ColorOptionCard(
                        title = "Ocean Blue",
                        desc = "Thanh lịch & Tập trung",
                        color = Color(0xFF1D5FE2),
                        isSelected = selectedColorState == AppThemeColor.BLUE,
                        modifier = Modifier.weight(1f),
                        onClick = { selectedColorState = AppThemeColor.BLUE }
                    )
                    ColorOptionCard(
                        title = "Deep Emerald",
                        desc = "Cân bằng & Tự nhiên",
                        color = Color(0xFF006B3F),
                        isSelected = selectedColorState == AppThemeColor.ROYAL_GREEN,
                        modifier = Modifier.weight(1f),
                        onClick = { selectedColorState = AppThemeColor.ROYAL_GREEN }
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ColorOptionCard(
                        title = "Sunset Crimson",
                        desc = "Nhiệt huyết & Động lực",
                        color = Color(0xFFB91D1D),
                        isSelected = selectedColorState == AppThemeColor.RED,
                        modifier = Modifier.weight(1f),
                        onClick = { selectedColorState = AppThemeColor.RED }
                    )
                    ColorOptionCard(
                        title = "Carbon Black",
                        desc = "Huyền bí & Tối giản",
                        color = Color(0xFF1B1B1F),
                        isSelected = selectedColorState == AppThemeColor.BLACK,
                        modifier = Modifier.weight(1f),
                        onClick = { selectedColorState = AppThemeColor.BLACK }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Save / Apply Button
            Button(
                onClick = { onColorSelected(selectedColorState) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                Text("Áp dụng", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

@Composable
fun ColorOptionCard(
    title: String,
    desc: String,
    color: Color,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) color.copy(alpha = 0.04f) else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) color else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(color),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) color else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = desc,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun ThemeOption(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label, 
            fontSize = 16.sp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )
        RadioButton(
            selected = isSelected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
        )
    }
}

@Composable
fun SectionTitle(titleRes: Int) {
    SectionTitle(title = stringResource(id = titleRes))
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.outline,
        modifier = Modifier.padding(start = 8.dp, bottom = 4.dp, top = 16.dp)
    )
}

@Composable
fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp
    ) {
        Column(content = content)
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    val tintColor = if (isDestructive) Color.Red else MaterialTheme.colorScheme.primary
    val textColor = if (isDestructive) Color.Red else MaterialTheme.colorScheme.onSurface
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(tintColor.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tintColor,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = if (isDestructive) FontWeight.Bold else FontWeight.Medium,
                color = textColor
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = if (isDestructive) Color.Red.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    titleRes: Int,
    subtitle: String? = null,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    SettingsItem(
        icon = icon,
        title = stringResource(id = titleRes),
        subtitle = subtitle,
        isDestructive = isDestructive,
        onClick = onClick
    )
}

@Composable
fun SettingsToggleItem(
    icon: ImageVector,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp), // Đồng bộ padding với SettingsItem
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp) // Đồng bộ size với SettingsItem
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = if (isSystemInDarkTheme()) 0.15f else 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = title,
            fontSize = 15.sp, // Đồng bộ size chữ
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                // Khi bật: Thumb đen (onPrimary) trên nền Track trắng (Primary) để có độ tương phản
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                // Khi tắt: Thumb màu xám (outline), Track xám tối hơn
                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                uncheckedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            ),
            modifier = Modifier.scale(0.8f) // Thu nhỏ nhẹ để thanh thoát hơn
        )
    }
}

@Composable
fun SettingsToggleItem(
    icon: ImageVector,
    titleRes: Int,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    SettingsToggleItem(
        icon = icon,
        title = stringResource(id = titleRes),
        checked = checked,
        onCheckedChange = onCheckedChange
    )
}

@Composable
fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
    )
}

@AppCombinedPreviews
@Composable
fun SettingsScreenPreview() {
    NewStartTheme {
        SettingsScreen(onNavigateToSocial = {}, onNavigateToHome = {})
    }
}
