package com.example.newstart.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.newstart.R
import com.example.newstart.ui.MainViewModel
import com.example.newstart.ui.theme.NewStartTheme
import com.example.newstart.ui.theme.ThemeMode
import com.example.newstart.ui.util.LanguagePreviews
import com.example.newstart.ui.util.LanguagePickerDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
    mainViewModel: MainViewModel = hiltViewModel()
) {
    var showLanguagePicker by remember { mutableStateOf(false) }
    var showThemePicker by remember { mutableStateOf(false) }
    val themeMode by mainViewModel.themeMode.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        text = stringResource(id = R.string.settings_title),
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    ) 
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Account Section
            item { SectionTitle(titleRes = R.string.settings_account_section) }
            item {
                SettingsCard {
                    SettingsItem(
                        icon = Icons.Default.Person,
                        titleRes = R.string.settings_profile,
                        onClick = {}
                    )
                    SettingsDivider()
                    SettingsItem(
                        icon = Icons.Default.Shield,
                        titleRes = R.string.settings_security,
                        onClick = {}
                    )
                }
            }

            // Preferences Section
            item { SectionTitle(titleRes = R.string.settings_preferences_section) }
            item {
                SettingsCard {
                    SettingsItem(
                        icon = Icons.Default.Language,
                        titleRes = R.string.settings_language,
                        onClick = { showLanguagePicker = true }
                    )
                    SettingsDivider()
                    SettingsItem(
                        icon = Icons.Default.Notifications,
                        titleRes = R.string.settings_notifications,
                        onClick = {}
                    )
                    SettingsDivider()
                    SettingsItem(
                        icon = Icons.Default.DarkMode,
                        titleRes = R.string.settings_dark_mode,
                        onClick = { showThemePicker = true }
                    )
                }
            }

            // Support Section
            item { SectionTitle(titleRes = R.string.settings_support_section) }
            item {
                SettingsCard {
                    SettingsItem(
                        icon = Icons.Default.HelpCenter,
                        titleRes = R.string.settings_help_center,
                        onClick = {}
                    )
                }
            }

            // Logout
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onLogout,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Icon(Icons.Default.Logout, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(id = R.string.settings_logout),
                        fontWeight = FontWeight.Bold
                    )
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
                "Chọn giao diện",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            ThemeOption("Sáng", ThemeMode.LIGHT, currentMode == ThemeMode.LIGHT) {
                onModeSelected(ThemeMode.LIGHT)
            }
            ThemeOption("Tối", ThemeMode.DARK, currentMode == ThemeMode.DARK) {
                onModeSelected(ThemeMode.DARK)
            }
            ThemeOption("Mặc định hệ thống", ThemeMode.SYSTEM, currentMode == ThemeMode.SYSTEM) {
                onModeSelected(ThemeMode.SYSTEM)
            }
        }
    }
}

@Composable
fun ThemeOption(
    label: String,
    mode: ThemeMode,
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
        RadioButton(
            selected = isSelected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF1D5FE2))
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = label, fontSize = 16.sp)
    }
}

@Composable
fun SectionTitle(titleRes: Int) {
    Text(
        text = stringResource(id = titleRes),
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.outline,
        modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
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
    titleRes: Int,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(0xFF1D5FE2).copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF1D5FE2),
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = stringResource(id = titleRes),
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outlineVariant
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(0xFF1D5FE2).copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF1D5FE2),
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = stringResource(id = titleRes),
            fontSize = 16.sp,
            color = Color.Black,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF1D5FE2)
            )
        )
    }
}

@Composable
fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        thickness = 0.5.dp,
        color = Color.LightGray.copy(alpha = 0.3f)
    )
}

@LanguagePreviews
@Composable
fun SettingsScreenPreview() {
    NewStartTheme {
        SettingsScreen(onLogout = {})
    }
}
