package com.example.newstart.ui.features.settings.components

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.newstart.R
import com.example.newstart.ui.theme.LocalDarkTheme
import com.example.newstart.ui.theme.AppThemeColor
import com.example.newstart.ui.theme.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSelectionDialog(
    currentMode: ThemeMode,
    onModeSelected: (ThemeMode) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
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

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsDialog(
    isHabitEnabled: Boolean,
    isCommunityEnabled: Boolean,
    isVi: Boolean,
    onHabitToggle: (Boolean) -> Unit,
    onCommunityToggle: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val isDark = LocalDarkTheme.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 48.dp, top = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(id = R.string.settings_notifications),
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            SettingsCard {
                SettingsToggleItem(
                    icon = Icons.Default.DirectionsRun,
                    title = if (isVi) "Nhắc nhở thói quen" else "Habit Reminders",
                    subtitle = if (isVi) "Thông báo khi đến giờ thực hiện thói quen" else "Notify when it's time for habits",
                    checked = isHabitEnabled,
                    onCheckedChange = onHabitToggle
                )
                SettingsDivider()
                SettingsToggleItem(
                    icon = Icons.Default.Groups,
                    title = if (isVi) "Cộng đồng & Nhóm" else "Community & Squads",
                    subtitle = if (isVi) "Thông báo về tin nhắn nhóm và tương tác" else "Notify about squad messages and interactions",
                    checked = isCommunityEnabled,
                    onCheckedChange = onCommunityToggle
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(25.dp)
            ) {
                Text("Xong", fontWeight = FontWeight.Bold)
            }
        }
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
    val isDark = LocalDarkTheme.current
    
    val editProfileSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = editProfileSheetState,
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
                text = "Sửa tên của bạn",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            TextField(
                value = firstName,
                onValueChange = { firstName = it },
                placeholder = { Text("Tên", color = Color.Gray) },
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
            
            TextField(
                value = lastName,
                onValueChange = { lastName = it },
                placeholder = { Text("Họ (không bắt buộc)", color = Color.Gray) },
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
