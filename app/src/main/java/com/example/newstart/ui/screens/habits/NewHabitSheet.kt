package com.example.newstart.ui.screens.habits

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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

data class HabitPreset(
    val name: String,
    val icon: String,
    val color: Color = Color(0xFF1D1D1F)
)

@Composable
fun NewHabitSheet(
    onDismiss: () -> Unit,
    onHabitSelected: (HabitPreset) -> Unit
) {
    var showCustomDialog by remember { mutableStateOf(false) }
    val categories = listOf("Popular", "Health", "Sports", "Mind", "Study")
    var selectedCategory by remember { mutableStateOf("Popular") }
    
    val presets = listOf(
        HabitPreset("Walk", "🚶"),
        HabitPreset("Sleep", "🛌"),
        HabitPreset("Drink water", "💧"),
        HabitPreset("Meditation", "🧘"),
        HabitPreset("Run", "🏃"),
        HabitPreset("Stand", "🧍"),
        HabitPreset("Cycling", "🚴"),
        HabitPreset("Workout", "💪"),
        HabitPreset("Active Calorie", "🔥")
    )

    if (showCustomDialog) {
        CustomHabitDialog(
            onDismiss = { showCustomDialog = false },
            onConfirm = { name, icon ->
                onHabitSelected(HabitPreset(name, icon))
                showCustomDialog = false
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(top = 8.dp)
    ) {
        // ... Handle, Header, Categories, Preset List code ...
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
            Text("New Habit", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
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
                PresetItem(preset, onClick = { onHabitSelected(preset) })
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
                onClick = { showCustomDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4D67)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.height(44.dp).fillMaxWidth(0.6f)
            ) {
                Text("Custom Habit", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomHabitDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var icon by remember { mutableStateOf("✨") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tạo thói quen mới", color = Color.White) },
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
                OutlinedTextField(
                    value = icon,
                    onValueChange = { icon = it },
                    label = { Text("Biểu tượng (Emoji)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFFF4D67)
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onConfirm(name, icon) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4D67))
            ) {
                Text("Tạo")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy", color = Color.Gray)
            }
        }
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
