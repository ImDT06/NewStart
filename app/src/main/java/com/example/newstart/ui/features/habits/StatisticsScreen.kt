package com.example.newstart.ui.features.habits

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.newstart.domain.model.Habit
import com.example.newstart.ui.MainViewModel
import com.example.newstart.ui.features.habits.components.HabitSwipeableItem
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    onNavigateBack: () -> Unit,
    mainViewModel: MainViewModel,
    viewModel: HabitStatsViewModel = hiltViewModel()
) {
    val allHabits by viewModel.allHabits.collectAsStateWithLifecycle()
    var selectedIds by remember { mutableStateOf(setOf<String>()) }
    var isSelectionMode by remember { mutableStateOf(false) }

    val sortedGroupedHabits = remember(allHabits) {
        allHabits.groupBy { it.date }
            .toList()
            .sortedByDescending { it.first }
    }

    val context = LocalContext.current
    val locale = remember(context) { context.resources.configuration.locales[0] }
    val isVietnamese = locale.language == "vi"

    Scaffold(
        topBar = {
            if (isSelectionMode) {
                TopAppBar(
                    title = { Text("${selectedIds.size} ${if (isVietnamese) "đã chọn" else "selected"}") },
                    navigationIcon = {
                        IconButton(onClick = { 
                            isSelectionMode = false
                            selectedIds = emptySet()
                        }) {
                            Icon(Icons.Default.Close, contentDescription = if (isVietnamese) "Hủy" else "Cancel")
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            if (selectedIds.size == allHabits.size) {
                                selectedIds = emptySet()
                            } else {
                                selectedIds = allHabits.map { it.id }.toSet()
                            }
                        }) {
                            Icon(Icons.Default.SelectAll, contentDescription = if (isVietnamese) "Chọn tất cả" else "Select All")
                        }
                        IconButton(
                            onClick = {
                                viewModel.deleteHabits(selectedIds)
                                isSelectionMode = false
                                selectedIds = emptySet()
                            },
                            enabled = selectedIds.isNotEmpty()
                        ) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = if (isVietnamese) "Xóa đã chọn" else "Delete selected", tint = if (selectedIds.isNotEmpty()) Color.Red else Color.Gray)
                        }
                    }
                )
            } else {
                CenterAlignedTopAppBar(
                    title = { Text(if (isVietnamese) "Danh sách thói quen" else "Habit List", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { isSelectionMode = true }) {
                            Icon(Icons.Default.Checklist, contentDescription = if (isVietnamese) "Chọn nhiều" else "Select multiple")
                        }
                    }
                )
            }
        }
    ) { padding ->
        if (allHabits.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ListAlt,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outlineVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (isVietnamese) "Chưa có thói quen nào được tạo.\nHãy quay lại trang chủ và thêm thói quen mới!"
                               else "No habits created yet.\nGo back to home and add a new habit!",
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.outline,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                sortedGroupedHabits.forEach { (dateStr, habitsInGroup) ->
                    item {
                        val formattedDate = remember(dateStr) {
                            try {
                                val date = LocalDate.parse(dateStr)
                                val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
                                date.format(formatter)
                            } catch (e: Exception) {
                                dateStr
                            }
                        }
                        Text(
                            text = formattedDate,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 24.dp, top = 16.dp, bottom = 8.dp)
                        )
                    }
                    items(items = habitsInGroup, key = { it.id }) { habit ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            if (isSelectionMode) {
                                Checkbox(
                                    checked = selectedIds.contains(habit.id),
                                    onCheckedChange = { checked ->
                                        selectedIds = if (checked) {
                                            selectedIds + habit.id
                                        } else {
                                            selectedIds - habit.id
                                        }
                                    }
                                )
                            }
                            HabitSwipeableItem(
                                modifier = Modifier.weight(1f),
                                habit = habit,
                                onDelete = { viewModel.deleteHabit(habit.id) },
                                onToggle = { h, c -> viewModel.toggleHabit(h, c) },
                                onEdit = { mainViewModel.startEditingHabit(it) }
                            )
                        }
                    }
                }
            }
        }
    }
}
