package com.example.newstart.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.*
import kotlinx.coroutines.launch

enum class PickerViewMode { Day, Month, Year }

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AdvancedDatePickerDialog(
    initialStartDate: LocalDate,
    initialEndDate: LocalDate?,
    isRange: Boolean = true,
    onDismiss: () -> Unit,
    onDateRangeSelected: (LocalDate, LocalDate?) -> Unit
) {
    var viewMode by remember { mutableStateOf(PickerViewMode.Day) }
    var selectedStartDate by remember { mutableStateOf(initialStartDate) }
    var selectedEndDate by remember { mutableStateOf(initialEndDate) }

    val initialPage = 500
    val pagerState = rememberPagerState(pageCount = { 1000 }, initialPage = initialPage)
    val scope = rememberCoroutineScope()
    
    val displayMonth by remember {
        derivedStateOf {
            val monthDiff = pagerState.currentPage - initialPage
            YearMonth.from(initialStartDate).plusMonths(monthDiff.toLong())
        }
    }

    val context = LocalContext.current
    val locale = context.resources.configuration.locales[0]
    val isVietnamese = locale.language == "vi"

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight()
                .clip(RoundedCornerShape(24.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { viewMode = if (viewMode == PickerViewMode.Month) PickerViewMode.Day else PickerViewMode.Month },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val monthLabel = if (isVietnamese) "Tháng ${displayMonth.monthValue}" else displayMonth.month.getDisplayName(TextStyle.FULL, locale)
                            Text(
                                text = monthLabel,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Icon(
                                imageVector = if (viewMode == PickerViewMode.Month) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    TextButton(
                        onClick = { viewMode = if (viewMode == PickerViewMode.Year) PickerViewMode.Day else PickerViewMode.Year },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = displayMonth.year.toString(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Icon(
                                imageVector = if (viewMode == PickerViewMode.Year) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    if (viewMode == PickerViewMode.Day) {
                        IconButton(onClick = { 
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                        }) {
                            Icon(Icons.Default.ChevronLeft, null)
                        }
                        IconButton(onClick = { 
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                        }) {
                            Icon(Icons.Default.ChevronRight, null)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 2.dp)
                ) {
                    val weekLabels = if (isVietnamese) listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN")
                    else listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su")
                    weekLabels.forEach { label ->
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Box(modifier = Modifier.height(240.dp)) {
                    AnimatedContent(
                        targetState = viewMode,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                        },
                        label = "pickerViewMode"
                    ) { targetMode ->
                        when (targetMode) {
                            PickerViewMode.Day -> {
                                HorizontalPager(
                                    state = pagerState,
                                    modifier = Modifier.fillMaxSize(),
                                    pageSpacing = 16.dp 
                                ) { page ->
                                    val pageMonth = YearMonth.from(initialStartDate).plusMonths((page - initialPage).toLong())
                                    DayPickerGrid(
                                        displayMonth = pageMonth,
                                        startDate = selectedStartDate,
                                        endDate = selectedEndDate,
                                        onDateClick = { date ->
                                            if (!isRange) {
                                                selectedStartDate = date
                                                selectedEndDate = null
                                            } else {
                                                if (selectedEndDate != null) {
                                                    selectedStartDate = date
                                                    selectedEndDate = null
                                                } else if (date.isBefore(selectedStartDate)) {
                                                    selectedStartDate = date
                                                } else {
                                                    selectedEndDate = date
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                            PickerViewMode.Month -> {
                                MonthPickerGrid(
                                    selectedMonth = displayMonth.monthValue,
                                    onMonthClick = { m ->
                                        scope.launch {
                                            val targetMonth = YearMonth.of(displayMonth.year, m)
                                            val monthDiff = (targetMonth.year - initialStartDate.year) * 12 + (targetMonth.monthValue - initialStartDate.monthValue)
                                            pagerState.scrollToPage(initialPage + monthDiff)
                                            viewMode = PickerViewMode.Day
                                        }
                                    }
                                )
                            }
                            PickerViewMode.Year -> {
                                YearPickerGrid(
                                    selectedYear = displayMonth.year,
                                    onYearClick = { y ->
                                        scope.launch {
                                            val targetMonth = YearMonth.of(y, displayMonth.monthValue)
                                            val monthDiff = (targetMonth.year - initialStartDate.year) * 12 + (targetMonth.monthValue - initialStartDate.monthValue)
                                            pagerState.scrollToPage(initialPage + monthDiff)
                                            viewMode = PickerViewMode.Day
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(if (isVietnamese) "HUỶ" else "CANCEL", fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    TextButton(
                        onClick = { onDateRangeSelected(selectedStartDate, selectedEndDate) }
                    ) {
                        Text(if (isVietnamese) "XÁC NHẬN" else "CONFIRM", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun DayPickerGrid(
    displayMonth: YearMonth,
    startDate: LocalDate,
    endDate: LocalDate?,
    onDateClick: (LocalDate) -> Unit
) {
    val today = remember { LocalDate.now() }
    val daysInMonth = remember(displayMonth) { displayMonth.lengthOfMonth() }
    val offset = remember(displayMonth) { 
        val firstDay = displayMonth.atDay(1).dayOfWeek.value
        // Adjust for Monday as first day of week (value 1)
        firstDay - 1 
    }
    val rangeColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
    
    Column(modifier = Modifier.fillMaxSize()) {
        repeat(6) { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(7) { col ->
                    val cellIndex = row * 7 + col
                    val dayNum = cellIndex - offset + 1
                    
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .drawBehind {
                                if (dayNum in 1..daysInMonth) {
                                    val date = displayMonth.atDay(dayNum)
                                    val isStart = date == startDate
                                    val isEnd = date == endDate
                                    val isInRange = endDate != null && date.isAfter(startDate) && date.isBefore(endDate)

                                    if (isInRange || (isStart && endDate != null) || isEnd) {
                                        val verticalPadding = 6.dp.toPx()
                                        val rectHeight = size.height - (verticalPadding * 2)
                                        val cornerRadius = rectHeight / 2
                                        
                                        when {
                                            isInRange -> {
                                                drawRect(
                                                    color = rangeColor,
                                                    topLeft = Offset(0f, verticalPadding),
                                                    size = Size(size.width, rectHeight)
                                                )
                                            }
                                            isStart -> {
                                                drawRoundRect(
                                                    color = rangeColor,
                                                    topLeft = Offset(0f, verticalPadding),
                                                    size = Size(size.width, rectHeight),
                                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius)
                                                )
                                                drawRect(
                                                    color = rangeColor,
                                                    topLeft = Offset(size.width / 2, verticalPadding),
                                                    size = Size(size.width / 2, rectHeight)
                                                )
                                            }
                                            isEnd -> {
                                                drawRoundRect(
                                                    color = rangeColor,
                                                    topLeft = Offset(0f, verticalPadding),
                                                    size = Size(size.width, rectHeight),
                                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius)
                                                )
                                                drawRect(
                                                    color = rangeColor,
                                                    topLeft = Offset(0f, verticalPadding),
                                                    size = Size(size.width / 2, rectHeight)
                                                )
                                            }
                                        }
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (dayNum in 1..daysInMonth) {
                            val date = remember(displayMonth, dayNum) { displayMonth.atDay(dayNum) }
                            val isToday = date == today
                            val isStart = date == startDate
                            val isEnd = date == endDate

                            Surface(
                                modifier = Modifier.size(36.dp),
                                shape = CircleShape,
                                color = if (isStart || isEnd) MaterialTheme.colorScheme.primary else Color.Transparent,
                                border = if (isToday && !isStart && !isEnd) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
                                onClick = { onDateClick(date) }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = dayNum.toString(),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isStart || isEnd || isToday) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isStart || isEnd) MaterialTheme.colorScheme.onPrimary 
                                                else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MonthPickerGrid(
    selectedMonth: Int,
    onMonthClick: (Int) -> Unit
) {
    val months = (1..12).toList()
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(months) { m ->
            val isSelected = m == selectedMonth
            Surface(
                onClick = { onMonthClick(m) },
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Box(modifier = Modifier.padding(vertical = 20.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Thg $m",
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun YearPickerGrid(
    selectedYear: Int,
    onYearClick: (Int) -> Unit
) {
    val currentYear = LocalDate.now().year
    val years = remember { (2020..2050).toList() }
    val gridState = rememberLazyGridState()

    LaunchedEffect(Unit) {
        val focusYear = if (years.contains(selectedYear)) selectedYear else currentYear
        val index = years.indexOf(focusYear)
        if (index != -1) {
            gridState.scrollToItem(index)
        }
    }
    
    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(years) { y ->
            val isSelected = y == selectedYear
            val isTodayYear = y == currentYear
            Surface(
                onClick = { onYearClick(y) },
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = if (isTodayYear && !isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
            ) {
                Box(modifier = Modifier.padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = y.toString(),
                        fontWeight = if (isSelected || isTodayYear) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun MonthPickerDialog(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    AdvancedDatePickerDialog(
        initialStartDate = selectedDate,
        initialEndDate = null,
        isRange = false,
        onDismiss = onDismiss,
        onDateRangeSelected = { start, _ ->
            onDateSelected(start)
        }
    )
}
