package com.example.newstart.ui.features.habits

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.LocalDate
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    onNavigateBack: () -> Unit,
    viewModel: HabitStatsViewModel = hiltViewModel()
) {
    val state by viewModel.heatMapState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Thống kê thói quen", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = "Mức độ hoạt động",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            HabitHeatMap(
                habitData = state.habitData,
                maxCompletions = state.maxCompletions
            )
            
            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Ít", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(4.dp))
                listOf(0.1f, 0.4f, 0.7f, 1f).forEach { alpha ->
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .padding(horizontal = 1.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = alpha))
                    )
                }
                Spacer(Modifier.width(4.dp))
                Text("Nhiều", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun HabitHeatMap(
    habitData: Map<LocalDate, Int>,
    maxCompletions: Int
) {
    val today = LocalDate.now()
    val endDate = today
    val startDate = today.minusMonths(6) // Hiển thị 6 tháng gần nhất

    val dates = remember(startDate, endDate) {
        val list = mutableListOf<LocalDate>()
        var current = startDate
        while (!current.isAfter(endDate)) {
            list.add(current)
            current = current.plusDays(1)
        }
        list
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Box(modifier = Modifier.padding(12.dp)) {
            LazyHorizontalGrid(
                rows = GridCells.Fixed(7), // 7 ngày trong tuần
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(dates) { date ->
                    val count = habitData[date] ?: 0
                    val alpha = if (count == 0) 0.05f else (count.toFloat() / maxCompletions).coerceIn(0.2f, 1f)
                    
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                if (count > 0) MaterialTheme.colorScheme.primary.copy(alpha = alpha)
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                            )
                            .border(
                                width = 0.5.dp,
                                color = if (date == today) MaterialTheme.colorScheme.primary else Color.Transparent,
                                shape = RoundedCornerShape(3.dp)
                            )
                    )
                }
            }
        }
    }
}
