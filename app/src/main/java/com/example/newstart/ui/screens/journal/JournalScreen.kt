package com.example.newstart.ui.screens.journal

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.newstart.R
import com.example.newstart.domain.model.JournalEntry
import com.example.newstart.ui.theme.NewStartTheme
import com.example.newstart.ui.util.AppCombinedPreviews
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalScreen(
    modifier: Modifier = Modifier,
    viewModel: JournalViewModel = hiltViewModel()
) {
    val entries by viewModel.entries.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    
    JournalContent(
        modifier = modifier,
        entries = entries,
        selectedDate = selectedDate,
        onDateSelected = { viewModel.onDateSelected(it) },
        onDeleteEntry = { viewModel.deleteEntry(it) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalContent(
    modifier: Modifier = Modifier,
    entries: List<JournalEntry>,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onDeleteEntry: (String) -> Unit
) {
    var selectedImageUrl by remember { mutableStateOf<String?>(null) }
    var entryToDelete by remember { mutableStateOf<JournalEntry?>(null) }
    var showMonthPicker by remember { mutableStateOf(false) }
    
    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val dateFormatter = remember { SimpleDateFormat("dd MMMM, yyyy", Locale("vi")) }

    val isDark = isSystemInDarkTheme()
    val today = LocalDate.now()
    val pagerState = rememberPagerState(pageCount = { 1000 }, initialPage = 500)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val locale = context.resources.configuration.locales[0]
    val isVietnamese = locale.language == "vi"
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = if (isDark) {
                        listOf(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            MaterialTheme.colorScheme.background
                        )
                    } else {
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.background
                        )
                    }
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Header Section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    val hour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
                    val greetingRes = when (hour) {
                        in 5..10 -> R.string.journal_greeting_morning
                        in 11..13 -> R.string.journal_greeting_noon
                        in 14..17 -> R.string.journal_greeting_afternoon
                        else -> R.string.journal_greeting_evening
                    }
                    Text(
                        text = stringResource(id = greetingRes),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isDark) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    
                    val headerDateText = remember(selectedDate, locale) {
                        if (selectedDate == today) dateFormatter.format(Date())
                        else {
                            val pattern = if (isVietnamese) "dd MMMM, yyyy" else "MMMM dd, yyyy"
                            selectedDate.format(DateTimeFormatter.ofPattern(pattern, locale))
                        }
                    }
                    Text(
                        text = headerDateText,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.clickable {
                            onDateSelected(today)
                            scope.launch {
                                pagerState.animateScrollToPage(500)
                            }
                        }
                    )
                }
                
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(36.dp),
                    onClick = { showMonthPicker = true }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "Month Picker",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Horizontal Date Picker
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 0.dp, bottom = 4.dp)
            ) { page ->
                val weekStart = remember(page) {
                    today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                        .plusWeeks((page - 500).toLong())
                }
                val weekDays = remember(weekStart) { (0..6).map { weekStart.plusDays(it.toLong()) } }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    weekDays.forEach { day ->
                        val isSelected = day == selectedDate
                        val isToday = day == today

                        val dayName = remember(day, locale) {
                            if (isVietnamese) {
                                when (day.dayOfWeek) {
                                    DayOfWeek.MONDAY -> "T2"
                                    DayOfWeek.TUESDAY -> "T3"
                                    DayOfWeek.WEDNESDAY -> "T4"
                                    DayOfWeek.THURSDAY -> "T5"
                                    DayOfWeek.FRIDAY -> "T6"
                                    DayOfWeek.SATURDAY -> "T7"
                                    DayOfWeek.SUNDAY -> "CN"
                                }
                            } else {
                                day.dayOfWeek.getDisplayName(TextStyle.SHORT, locale).uppercase()
                            }
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onDateSelected(day) }
                        ) {
                            Text(
                                text = dayName,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))

                            Surface(
                                modifier = Modifier.size(32.dp),
                                shape = CircleShape,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                border = if (!isSelected && isToday) BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary) else null
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = day.dayOfMonth.toString(),
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Journal List Surface
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)),
                color = MaterialTheme.colorScheme.surface
            ) {
                if (entries.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "Chưa có nhật ký nào.\nHãy bắt đầu ghi lại khoảnh khắc nhé!",
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp, start = 24.dp, end = 24.dp)
                    ) {
                        items(
                            items = entries,
                            key = { it.id }
                        ) { entry ->
                            val dismissState = rememberSwipeToDismissBoxState(
                                confirmValueChange = {
                                    if (it == SwipeToDismissBoxValue.EndToStart) {
                                        entryToDelete = entry
                                        false
                                    } else false
                                }
                            )

                            LaunchedEffect(entryToDelete) {
                                if (entryToDelete == null) {
                                    dismissState.reset()
                                }
                            }

                            SwipeToDismissBox(
                                state = dismissState,
                                enableDismissFromStartToEnd = false,
                                backgroundContent = {
                                    val color = when (dismissState.dismissDirection) {
                                        SwipeToDismissBoxValue.EndToStart -> Color.Red.copy(alpha = 0.8f)
                                        else -> Color.Transparent
                                    }
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(bottom = 32.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(color),
                                        contentAlignment = Alignment.CenterEnd
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = Color.White,
                                            modifier = Modifier.padding(end = 16.dp)
                                        )
                                    }
                                },
                                content = {
                                    AnimatedVisibility(
                                        visible = true,
                                        enter = fadeIn() + slideInVertically(),
                                        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                                    ) {
                                        TimelineEntryItem(
                                            entry = entry,
                                            timeFormatted = entry.timestamp?.let { timeFormatter.format(it) } ?: "--:--",
                                            isLast = entries.lastOrNull()?.id == entry.id,
                                            onImageClick = { selectedImageUrl = it }
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        // Monthly Calendar Dialog
        if (showMonthPicker) {
            MonthPickerDialog(
                selectedDate = selectedDate,
                onDateSelected = { date ->
                    onDateSelected(date)
                    showMonthPicker = false
                    val weekDiff = (date.toEpochDay() - today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toEpochDay()) / 7
                    scope.launch {
                        pagerState.scrollToPage(500 + weekDiff.toInt())
                    }
                },
                onDismiss = { showMonthPicker = false }
            )
        }

        // Delete Confirmation Dialog
        entryToDelete?.let { entry ->
            AlertDialog(
                onDismissRequest = { entryToDelete = null },
                title = { Text("Xóa nhật ký?") },
                text = { Text("Hành động này không thể hoàn tác. Bạn có chắc chắn muốn xóa khoảnh khắc này không?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onDeleteEntry(entry.id)
                            entryToDelete = null
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Xóa")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { entryToDelete = null }) {
                        Text("Hủy")
                    }
                }
            )
        }

        // Image Viewer Dialog
        selectedImageUrl?.let { url ->
            Dialog(
                onDismissRequest = { selectedImageUrl = null },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.9f))
                        .clickable { selectedImageUrl = null },
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = url,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(32.dp)),
                        contentScale = ContentScale.Crop
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
    var stagedMonth by remember { mutableStateOf(YearMonth.from(selectedDate)) }
    var isYearPickerMode by remember { mutableStateOf(false) }
    var yearPageStart by remember { mutableStateOf((stagedMonth.year / 12) * 12) }
    
    val context = LocalContext.current
    val locale = context.resources.configuration.locales[0]

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.8f))
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .clip(RoundedCornerShape(32.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable(enabled = false) {}
                    .padding(24.dp)
            ) {
                Text(
                    text = stringResource(R.string.journal_memories_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { 
                            if (isYearPickerMode) yearPageStart -= 12 else stagedMonth = stagedMonth.minusMonths(1)
                        }
                    ) {
                        Icon(Icons.Default.ChevronLeft, null, tint = MaterialTheme.colorScheme.primary)
                    }
                    
                    val headerTitle = remember(stagedMonth, locale, isYearPickerMode, yearPageStart) {
                        if (isYearPickerMode) "$yearPageStart - ${yearPageStart + 11}"
                        else stagedMonth.format(DateTimeFormatter.ofPattern(if (locale.language == "vi") "'tháng' M yyyy" else "MMMM yyyy", locale))
                    }
                    
                    Surface(
                        onClick = { 
                            isYearPickerMode = !isYearPickerMode 
                            if (isYearPickerMode) yearPageStart = (stagedMonth.year / 12) * 12
                        },
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(text = headerTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Icon(imageVector = if (isYearPickerMode) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }

                    IconButton(
                        onClick = { 
                            if (isYearPickerMode) yearPageStart += 12 else stagedMonth = stagedMonth.plusMonths(1)
                        }
                    ) {
                        Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.primary)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (isYearPickerMode) {
                    val currentYear = remember { LocalDate.now().year }
                    val years = remember(yearPageStart) { (yearPageStart..(yearPageStart + 11)).toList() }
                    
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.height(280.dp)
                    ) {
                        items(years.size) { index ->
                            val year = years[index]
                            val isFocusedYear = year == stagedMonth.year
                            val isTodayYear = year == currentYear
                            
                            Surface(
                                onClick = {
                                    stagedMonth = YearMonth.of(year, stagedMonth.month)
                                    isYearPickerMode = false
                                },
                                shape = RoundedCornerShape(16.dp),
                                color = if (isFocusedYear) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = if (isTodayYear && !isFocusedYear) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
                            ) {
                                Box(modifier = Modifier.padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                                    Text(text = year.toString(), style = MaterialTheme.typography.bodyLarge, fontWeight = if (isFocusedYear || isTodayYear) FontWeight.Bold else FontWeight.Medium, color = if (isFocusedYear) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                    }
                } else {
                    val daysInMonth = stagedMonth.lengthOfMonth()
                    val offset = stagedMonth.atDay(1).dayOfWeek.value - 1
                    val totalCells = ((daysInMonth + offset + 6) / 7) * 7

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row {
                            val dayLabels = if (locale.language == "vi") listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN")
                                           else listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                            dayLabels.forEach { label ->
                                Text(text = label, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                            }
                        }

                        for (row in 0 until totalCells / 7) {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                for (col in 0 until 7) {
                                    val cellIdx = row * 7 + col
                                    val dayNum = cellIdx - offset + 1
                                    Box(modifier = Modifier.weight(1f).aspectRatio(1f)) {
                                        if (dayNum in 1..daysInMonth) {
                                            val date = stagedMonth.atDay(dayNum)
                                            val isSelected = date == selectedDate
                                            val isToday = date == LocalDate.now()
                                            Surface(
                                                modifier = Modifier.fillMaxSize(),
                                                shape = RoundedCornerShape(12.dp),
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                                border = if (isToday && !isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                                                onClick = { onDateSelected(date) }
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text(text = dayNum.toString(), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
                    Text(text = stringResource(R.string.journal_close_button), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun TimelineEntryItem(
    entry: JournalEntry,
    timeFormatted: String,
    isLast: Boolean,
    onImageClick: (String) -> Unit
) {
    val timelineColor = MaterialTheme.colorScheme.primary
    val isDark = isSystemInDarkTheme()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .height(IntrinsicSize.Min) 
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(56.dp)
        ) {
            Text(
                text = timeFormatted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.padding(bottom = 4.dp)
            )
            
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.TopCenter
            ) {
                if (!isLast) {
                    Canvas(modifier = Modifier.fillMaxHeight()) {
                        drawLine(
                            color = timelineColor.copy(alpha = 0.15f),
                            start = Offset(size.width / 2, 0f),
                            end = Offset(size.width / 2, size.height + 32f),
                            strokeWidth = 2.dp.toPx()
                        )
                    }
                }
                
                Surface(
                    modifier = Modifier.size(10.dp),
                    shape = CircleShape,
                    color = timelineColor,
                    border = BorderStroke(2.dp, timelineColor.copy(alpha = 0.2f)),
                ) {}
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Card(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 16.dp, end = 4.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f) 
                                 else MaterialTheme.colorScheme.surface,
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 1.dp),
            border = BorderStroke(
                width = 0.5.dp, 
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = timelineColor.copy(alpha = 0.08f),
                        shape = CircleShape,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(text = entry.emoji, fontSize = 20.sp)
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(10.dp))
                    
                    Text(
                        text = entry.text,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 20.sp,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (entry.imageUrl != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clickable { onImageClick(entry.imageUrl) },
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        AsyncImage(
                            model = entry.imageUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            error = painterResource(id = R.drawable.ic_launcher_foreground)
                        )
                    }
                }
            }
        }
    }
}

@AppCombinedPreviews
@Composable
fun JournalScreenPreview() {
    NewStartTheme {
        JournalContent(
            entries = listOf(
                JournalEntry(id = "1", emoji = "😊", text = "Một ngày tuyệt vời tại UIT!", timestamp = Date()),
                JournalEntry(id = "2", emoji = "🥰", text = "Học Compose thú vị quá", timestamp = Date())
            ),
            selectedDate = LocalDate.now(),
            onDateSelected = {},
            onDeleteEntry = {}
        )
    }
}
