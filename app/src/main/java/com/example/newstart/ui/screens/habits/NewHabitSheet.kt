package com.example.newstart.ui.screens.habits

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    onDismiss: () -> Unit,
    onHabitSelected: (String, String, String?, Int, Color, LocalDate) -> Unit
) {
    var selectedPreset by remember { mutableStateOf<HabitPreset?>(null) }
    var showConfigDialog by remember { mutableStateOf(false) }
    
    val categories = listOf("Phổ biến", "Sức khỏe", "Thể thao", "Tâm trí", "Học tập")
    var selectedCategory by remember { mutableStateOf("Phổ biến") }
    
    val presets = remember(selectedCategory) {
        when (selectedCategory) {
            "Sức khỏe" -> listOf(
                HabitPreset("Ngủ", "🛌"),
                HabitPreset("Uống nước", "💧"),
                HabitPreset("Đứng", "🧍"),
                HabitPreset("Lượng Calo", "🔥")
            )
            "Thể thao" -> listOf(
                HabitPreset("Chạy bộ", "🏃"),
                HabitPreset("Đạp xe", "🚴"),
                HabitPreset("Tập thể dục", "💪"),
                HabitPreset("Đi bộ", "🚶")
            )
            "Tâm trí" -> listOf(
                HabitPreset("Thiền", "🧘"),
                HabitPreset("Ngủ", "🛌"),
                HabitPreset("Đọc sách", "📚")
            )
            "Học tập" -> listOf(
                HabitPreset("Đọc sách", "📚"),
                HabitPreset("Học bài", "✍️"),
                HabitPreset("Lập trình", "💻")
            )
            else -> listOf( // Phổ biến
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
            onConfirm = { name, icon, time, mins, date ->
                onHabitSelected(name, icon, time, mins, Color(0xFF1D1D1F), date)
                showConfigDialog = false
                selectedPreset = null
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(top = 8.dp)
    ) {
        // Handle
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(4.dp)
                .clip(CircleShape)
                .background(Color.Gray.copy(alpha = 0.5f))
                .align(Alignment.CenterHorizontally)
        )

        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, null, tint = Color.White)
            }
            Text("Thói quen mới", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            IconButton(onClick = { /* Store/Shop */ }) {
                Icon(Icons.Default.Storefront, null, tint = Color(0xFFFF4D67))
            }
        }

        // Categories
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
                    color = if (isSelected) Color(0xFFFF4D67) else Color(0xFF1D1D1F),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = category,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        // Preset List
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

        // Custom Habit Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Button(
                onClick = { showConfigDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4D67)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.height(44.dp).fillMaxWidth(0.6f)
            ) {
                Text("Tự tạo thói quen", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitConfigDialog(
    initialDate: LocalDate,
    preset: HabitPreset? = null,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String?, Int, LocalDate) -> Unit
) {
    var name by remember { mutableStateOf(preset?.name ?: "") }
    var icon by remember { mutableStateOf(preset?.icon ?: "✨") }
    var showTimePicker by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedTime by remember { mutableStateOf<String?>(null) }
    var selectedDate by remember { mutableStateOf(initialDate) }
    var minsBefore by remember { mutableStateOf(0) }

    val timePickerState = rememberTimePickerState()
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (preset == null) "Tạo thói quen mới" else "Cấu hình thói quen", color = Color.White) },
        containerColor = Color(0xFF1D1D1F),
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Tên thói quen") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFFF4D67)
                    )
                )
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = icon,
                        onValueChange = { icon = it },
                        label = { Text("Icon") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(horizontalAlignment = Alignment.End) {
                        TextButton(
                            onClick = { showDatePicker = true },
                            colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFFF4D67))
                        ) {
                            Icon(Icons.Default.CalendarToday, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(selectedDate.format(DateTimeFormatter.ofPattern("dd/MM")), fontSize = 12.sp)
                        }
                        Button(
                            onClick = { showTimePicker = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(Icons.Default.AccessTime, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(selectedTime ?: "Giờ", fontSize = 12.sp)
                        }
                    }
                }

                if (selectedTime != null) {
                    Text("Nhắc trước (phút):", color = Color.Gray, fontSize = 12.sp)
                    
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
                                label = { Text("$mins", fontSize = 10.sp) },
                                modifier = Modifier.height(28.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    labelColor = Color.White,
                                    selectedContainerColor = Color(0xFFFF4D67)
                                )
                            )
                        }
                        
                        FilterChip(
                            selected = isCustomMins,
                            onClick = { isCustomMins = true },
                            label = { Text("Tùy chỉnh", fontSize = 10.sp) },
                            modifier = Modifier.height(28.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                labelColor = Color.White,
                                selectedContainerColor = Color(0xFFFF4D67)
                            )
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
                            label = { Text("Số phút nhắc trước", fontSize = 12.sp) },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                            ),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFFFF4D67)
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank() && selectedTime != null) onConfirm(name, icon, selectedTime, minsBefore, selectedDate) },
                enabled = name.isNotBlank() && selectedTime != null,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF4D67),
                    disabledContainerColor = Color.Gray.copy(alpha = 0.5f)
                )
            ) {
                Text("Xác nhận")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy", color = Color.Gray)
            }
        }
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
        color = Color(0xFF1D1D1F).copy(alpha = 0.5f),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(preset.icon, fontSize = 20.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Text(preset.name, color = Color.White, fontSize = 14.sp, modifier = Modifier.weight(1f))
            Icon(Icons.Default.FavoriteBorder, null, tint = Color(0xFFFF4D67), modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(18.dp))
        }
    }
}
