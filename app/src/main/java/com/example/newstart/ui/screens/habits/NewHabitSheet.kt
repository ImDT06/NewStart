package com.example.newstart.ui.screens.habits

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.newstart.R
import com.example.newstart.domain.model.Habit
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class HabitPreset(
    val name: String,
    val icon: String,
    val color: Color = Color(0xFF1D1D1F),
    val time: String? = null,
    val minsBefore: Int = 0
)

@Composable
fun NewHabitSheet(
    initialDate: LocalDate,
    editingHabit: Habit? = null,
    onDismiss: () -> Unit,
    onHabitSelected: (String, String, String?, Int, Color, LocalDate) -> Unit
) {
    var selectedPreset by remember { mutableStateOf<HabitPreset?>(null) }
    var showConfigDialog by remember { mutableStateOf(false) }
    
    // Nếu đang sửa, hiện thẳng UI chỉnh sửa
    if (editingHabit != null) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(bottom = 32.dp)
        ) {
            // Handle
            Box(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                    .align(Alignment.CenterHorizontally)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, null)
                }
                Text(
                    text = stringResource(R.string.habits_custom_dialog_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.size(48.dp))
            }

            HabitConfigContent(
                initialDate = initialDate,
                habit = editingHabit,
                onConfirm = { n, i, t, m, c, d -> onHabitSelected(n, i, t, m, c, d) },
                onCancel = onDismiss
            )
        }
        return
    }

    // Chế độ tạo mới - Hiện danh sách mẫu
    val categories = listOf(
        stringResource(R.string.habits_cat_popular),
        stringResource(R.string.habits_cat_health),
        stringResource(R.string.habits_cat_sports),
        stringResource(R.string.habits_cat_mind),
        stringResource(R.string.habits_cat_study)
    )
    var selectedCategory by remember { mutableStateOf(categories[0]) }
    
    val presets = remember(selectedCategory) {
        when (selectedCategory) {
            "Health", "Sức khỏe" -> listOf(
                HabitPreset("Ngủ", "🛌"),
                HabitPreset("Uống nước", "💧"),
                HabitPreset("Đứng", "🧍"),
                HabitPreset("Lượng Calo", "🔥")
            )
            "Sports", "Thể thao" -> listOf(
                HabitPreset("Chạy bộ", "🏃"),
                HabitPreset("Đạp xe", "🚴"),
                HabitPreset("Tập thể dục", "💪"),
                HabitPreset("Đi bộ", "🚶")
            )
            "Mind", "Tâm trí" -> listOf(
                HabitPreset("Thiền", "🧘"),
                HabitPreset("Ngủ", "🛌"),
                HabitPreset("Đọc sách", "📚")
            )
            "Study", "Học tập" -> listOf(
                HabitPreset("Đọc sách", "📚"),
                HabitPreset("Học bài", "✍️"),
                HabitPreset("Lập trình", "💻")
            )
            else -> listOf(
                HabitPreset("Đi bộ", "🚶"),
                HabitPreset("Ngủ", "🛌"),
                HabitPreset("Uống nước", "💧"),
                HabitPreset("Chạy bộ", "🏃")
            )
        }
    }

    if (showConfigDialog) {
        HabitConfigDialog(
            initialDate = initialDate,
            preset = selectedPreset,
            onDismiss = { 
                showConfigDialog = false
                selectedPreset = null
            },
            onConfirm = { name, icon, time, mins, color, date ->
                onHabitSelected(name, icon, time, mins, color, date)
                showConfigDialog = false
                selectedPreset = null
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .imePadding()
            .padding(top = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(4.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                .align(Alignment.CenterHorizontally)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.onSurface)
            }
            Text(
                text = stringResource(R.string.habits_new_title), 
                color = MaterialTheme.colorScheme.onSurface, 
                fontSize = 18.sp, 
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = { /* Store/Shop */ }) {
                Icon(Icons.Default.Storefront, null, tint = MaterialTheme.colorScheme.primary)
            }
        }

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(categories) { category ->
                val isSelected = selectedCategory == category
                Surface(
                    onClick = { selectedCategory = category },
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = category,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(presets) { preset ->
                PresetItem(preset, onClick = { 
                    selectedPreset = preset
                    showConfigDialog = true
                })
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Button(
                onClick = { showConfigDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.height(44.dp).fillMaxWidth(0.6f)
            ) {
                Text(
                    text = stringResource(R.string.habits_btn_custom), 
                    fontWeight = FontWeight.Bold, 
                    fontSize = 14.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitConfigContent(
    initialDate: LocalDate,
    preset: HabitPreset? = null,
    habit: Habit? = null,
    onConfirm: (String, String, String?, Int, Color, LocalDate) -> Unit,
    onCancel: () -> Unit
) {
    var name by remember { mutableStateOf(habit?.name ?: preset?.name ?: "") }
    var icon by remember { mutableStateOf(habit?.icon ?: preset?.icon ?: "✨") }
    var showTimePicker by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedTime by remember { mutableStateOf(habit?.reminderTime ?: preset?.time) }
    var selectedDate by remember { 
        mutableStateOf(
            if (habit != null) LocalDate.parse(habit.date) else initialDate
        ) 
    }
    var minsBefore by remember { mutableStateOf(habit?.reminderMinutesBefore ?: preset?.minsBefore ?: 0) }

    val timePickerState = rememberTimePickerState(
        initialHour = selectedTime?.split(":")?.get(0)?.toInt() ?: 0,
        initialMinute = selectedTime?.split(":")?.get(1)?.toInt() ?: 0
    )
    
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    )

    if (showTimePicker) {
        TimePickerDialog(
            onDismiss = { showTimePicker = false },
            onConfirm = {
                val hour = if (timePickerState.hour < 10) "0${timePickerState.hour}" else "${timePickerState.hour}"
                val minute = if (timePickerState.minute < 10) "0${timePickerState.minute}" else "${timePickerState.minute}"
                selectedTime = "$hour:$minute"
                showTimePicker = false
            }
        ) {
            TimePicker(state = timePickerState)
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        selectedDate = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text(stringResource(R.string.habits_custom_name_label)) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = icon,
                onValueChange = { icon = it },
                label = { Text(stringResource(R.string.habits_custom_icon_label)) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(horizontalAlignment = Alignment.End) {
                TextButton(
                    onClick = { showDatePicker = true },
                ) {
                    Icon(Icons.Default.CalendarToday, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(selectedDate.format(DateTimeFormatter.ofPattern("dd/MM")), fontSize = 14.sp)
                }
                Button(
                    onClick = { showTimePicker = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(36.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.AccessTime, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(selectedTime ?: "Giờ", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }

        if (selectedTime != null) {
            Text(stringResource(R.string.habits_reminder_label), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
            
            var isCustomMins by remember { mutableStateOf(false) }
            var customMinsText by remember { mutableStateOf("") }

            @OptIn(ExperimentalLayoutApi::class)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(0, 5, 10, 15, 30).forEach { mins ->
                    FilterChip(
                        selected = !isCustomMins && minsBefore == mins,
                        onClick = { 
                            minsBefore = mins
                            isCustomMins = false
                        },
                        label = { Text("$mins", fontSize = 12.sp) }
                    )
                }
                
                FilterChip(
                    selected = isCustomMins,
                    onClick = { isCustomMins = true },
                    label = { Text(stringResource(R.string.habits_custom_mins), fontSize = 12.sp) }
                )
            }

            if (isCustomMins) {
                OutlinedTextField(
                    value = customMinsText,
                    onValueChange = { 
                        if (it.all { char -> char.isDigit() } && it.length <= 3) {
                            customMinsText = it
                            minsBefore = it.toIntOrNull() ?: 0
                        }
                    },
                    label = { Text(stringResource(R.string.habits_custom_mins_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { if (name.isNotBlank() && selectedTime != null) onConfirm(name, icon, selectedTime, minsBefore, Color(0xFF1D1D1F), selectedDate) },
            enabled = name.isNotBlank() && selectedTime != null,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            Text(stringResource(R.string.habits_btn_create), fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitConfigDialog(
    initialDate: LocalDate,
    preset: HabitPreset? = null,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String?, Int, Color, LocalDate) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                text = stringResource(R.string.habits_custom_dialog_title), 
                color = MaterialTheme.colorScheme.onSurface
            ) 
        },
        containerColor = MaterialTheme.colorScheme.surface,
        text = {
            HabitConfigContent(
                initialDate = initialDate,
                preset = preset,
                onConfirm = { n, i, t, m, c, d -> onConfirm(n, i, t, m, c, d) },
                onCancel = onDismiss
            )
        },
        confirmButton = {},
        dismissButton = {}
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    content: @Composable () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        text = { content() }
    )
}

@Composable
fun PresetItem(preset: HabitPreset, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(preset.icon, fontSize = 20.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Text(preset.name, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, modifier = Modifier.weight(1f))
            Icon(Icons.Default.FavoriteBorder, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(18.dp))
        }
    }
}
