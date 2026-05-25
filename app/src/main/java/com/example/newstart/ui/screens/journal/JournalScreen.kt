package com.example.newstart.ui.screens.journal

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
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
    val selectedDateRange by viewModel.selectedDateRange.collectAsState()
    
    JournalContent(
        modifier = modifier,
        entries = entries,
        selectedDateRange = selectedDateRange,
        onDateRangeSelected = { start, end -> viewModel.onDateRangeSelected(start, end) },
        onQuickFilterSelected = { viewModel.setQuickFilter(it) },
        onDeleteEntry = { viewModel.deleteEntry(it) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalContent(
    modifier: Modifier = Modifier,
    entries: List<JournalEntry>,
    selectedDateRange: Pair<LocalDate, LocalDate?>,
    onDateRangeSelected: (LocalDate, LocalDate?) -> Unit,
    onQuickFilterSelected: (String) -> Unit,
    onDeleteEntry: (String) -> Unit
) {
    var selectedImageUrl by remember { mutableStateOf<String?>(null) }
    var entryToDelete by remember { mutableStateOf<JournalEntry?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var previousDateRange by remember { mutableStateOf<Pair<LocalDate, LocalDate?>?>(null) }
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val isInspectionMode = LocalInspectionMode.current

    // Khi bật/tắt tìm kiếm
    LaunchedEffect(isSearchActive) {
        if (isSearchActive) {
            previousDateRange = selectedDateRange
            onQuickFilterSelected("All")
            if (!isInspectionMode) {
                focusRequester.requestFocus()
            }
        } else {
            previousDateRange?.let {
                val isStillAll = selectedDateRange.first == LocalDate.of(2000, 1, 1)
                if (isStillAll) {
                    onDateRangeSelected(it.first, it.second)
                }
                previousDateRange = null
            }
        }
    }
    
    val pagerState = rememberPagerState(pageCount = { 1000 }, initialPage = 500)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val locale = remember(context) {
        val locales = context.resources.configuration.locales
        if (!locales.isEmpty) locales[0] else Locale.getDefault()
    }
    val isVietnamese = locale.language == "vi"

    val timeFormatter = remember { SimpleDateFormat("HH:mm", locale) }
    val isDark = isSystemInDarkTheme()
    val today = LocalDate.now()

    val filteredEntries = remember(entries, searchQuery) {
        if (searchQuery.isEmpty()) entries
        else entries.filter { it.text.contains(searchQuery, ignoreCase = true) || it.emoji.contains(searchQuery) }
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = if (isDark) {
                        listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.background
                        )
                    } else {
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 0.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AnimatedContent(
                        targetState = isSearchActive,
                        transitionSpec = {
                            (fadeIn() + expandHorizontally()).togetherWith(fadeOut() + shrinkHorizontally())
                        },
                        modifier = Modifier.weight(1f),
                        label = "search_header"
                    ) { searching ->
                        if (searching) {
                            TextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(focusRequester),
                                placeholder = { Text(stringResource(R.string.home_search_placeholder), fontSize = 14.sp) },
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    disabledContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                ),
                                trailingIcon = {
                                    IconButton(onClick = { 
                                        isSearchActive = false
                                        searchQuery = ""
                                        focusManager.clearFocus()
                                    }) {
                                        Icon(Icons.Default.Close, null)
                                    }
                                },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyLarge,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() })
                            )
                        } else {
                            val hour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
                            val greetingRes = when (hour) {
                                in 5..10 -> R.string.journal_greeting_morning
                                in 11..13 -> R.string.journal_greeting_noon
                                in 14..17 -> R.string.journal_greeting_afternoon
                                else -> R.string.journal_greeting_evening
                            }
                            Text(
                                text = stringResource(id = greetingRes),
                                style = MaterialTheme.typography.titleLarge, 
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onBackground,
                                letterSpacing = (-0.5).sp
                            )
                        }
                    }
                    
                    if (!isSearchActive) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { isSearchActive = true }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    modifier = Modifier.size(24.dp), // Match HabitsScreen icon size
                                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                )
                            }
                            IconButton(
                                onClick = { showDatePicker = true }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = "Date Picker",
                                    modifier = Modifier.size(24.dp), // Match HabitsScreen icon size
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                if (!isSearchActive) {
                    val headerDateText = remember(selectedDateRange, locale) {
                        val start = selectedDateRange.first
                        val end = selectedDateRange.second
                        if (end == null) {
                            if (start == today) if (isVietnamese) "Hôm nay" else "Today"
                            else {
                                val pattern = if (isVietnamese) "dd MMMM" else "MMMM dd"
                                start.format(DateTimeFormatter.ofPattern(pattern, locale))
                            }
                        } else {
                            val pattern = if (isVietnamese) "dd/MM" else "MM/dd"
                            "${start.format(DateTimeFormatter.ofPattern(pattern, locale))} - ${end.format(DateTimeFormatter.ofPattern(pattern, locale))}"
                        }
                    }
                    Text(
                        text = headerDateText,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .offset(y = (-8).dp) // Pull closer to greeting
                            .clickable { onQuickFilterSelected("Today") }
                    )
                }
            }

            // Quick Filters Section
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 0.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(end = 20.dp)
            ) {
                val filters = listOf(
                    "All" to R.string.habits_filter_all,
                    "Year" to null,
                    "Month" to null,
                    "Week" to null,
                    "Today" to R.string.habits_today
                )
                
                items(filters) { (key, labelRes) ->
                    val label = when {
                        labelRes != null -> stringResource(labelRes)
                        key == "Year" -> if (isVietnamese) "Năm này" else "This Year"
                        key == "Month" -> if (isVietnamese) "Tháng này" else "This Month"
                        key == "Week" -> if (isVietnamese) "Tuần này" else "This Week"
                        else -> key
                    }

                    val isSelected = remember(selectedDateRange, key) {
                        val today = LocalDate.now()
                        when (key) {
                            "Today" -> selectedDateRange.first == today && selectedDateRange.second == null
                            "Week" -> selectedDateRange.first == today.with(DayOfWeek.MONDAY) && selectedDateRange.second == today.with(DayOfWeek.SUNDAY)
                            "Month" -> selectedDateRange.first == today.withDayOfMonth(1) && selectedDateRange.second == today.withDayOfMonth(today.lengthOfMonth())
                            "Year" -> selectedDateRange.first == today.withDayOfYear(1) && selectedDateRange.second == today.withDayOfYear(today.lengthOfYear())
                            "All" -> selectedDateRange.first == LocalDate.of(2000, 1, 1)
                            else -> false
                        }
                    }

                    FilterChip(
                        selected = isSelected,
                        onClick = { 
                            onQuickFilterSelected(key)
                            if (isSearchActive) {
                                previousDateRange = null
                            }
                        },
                        label = { 
                            Text(
                                text = label, 
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            ) 
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            containerColor = if (isDark) Color.White.copy(alpha = 0.05f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        border = null
                    )
                }
            }

            // Journal List Surface
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp // Creates the "layered" effect
            ) {
                if (filteredEntries.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        val emptyMsg = if (searchQuery.isEmpty()) stringResource(R.string.journal_empty_message)
                                      else if (isVietnamese) "Không tìm thấy nhật ký phù hợp" else "No matching journal found"
                        Text(
                            emptyMsg,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    val entriesWithHeaders = remember(filteredEntries) {
                        val result = mutableListOf<Any>()
                        var lastDate: LocalDate? = null
                        filteredEntries.forEach { entry ->
                            val date = entry.timestamp?.let {
                                java.time.Instant.ofEpochMilli(it.time)
                                    .atZone(java.time.ZoneId.systemDefault())
                                    .toLocalDate()
                            }
                            if (date != null && date != lastDate) {
                                result.add(date)
                                lastDate = date
                            }
                            result.add(entry)
                        }
                        result
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTapGestures(onTap = { focusManager.clearFocus() })
                            },
                        contentPadding = PaddingValues(top = 8.dp, bottom = 100.dp, start = 16.dp, end = 16.dp)
                    ) {
                        items(
                            items = entriesWithHeaders,
                            key = { if (it is JournalEntry) it.id else it.toString() }
                        ) { item ->
                            if (item is LocalDate) {
                                val pattern = if (isVietnamese) "dd MMMM, yyyy" else "MMMM dd, yyyy"
                                Text(
                                    text = item.format(DateTimeFormatter.ofPattern(pattern, locale)),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 16.dp, bottom = 8.dp, start = 8.dp)
                                )
                            } else if (item is JournalEntry) {
                                val dismissState = rememberSwipeToDismissBoxState(
                                    confirmValueChange = {
                                        if (it == SwipeToDismissBoxValue.EndToStart) {
                                            entryToDelete = item
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
                                            SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f)
                                            else -> Color.Transparent
                                        }
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(vertical = 4.dp)
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(color),
                                            contentAlignment = Alignment.CenterEnd
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Delete",
                                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                                modifier = Modifier.padding(end = 20.dp)
                                            )
                                        }
                                    },
                                    content = {
                                        TimelineEntryItem(
                                            entry = item,
                                            timeFormatted = item.timestamp?.let { timeFormatter.format(it) } ?: "--:--",
                                            isLast = entries.lastOrNull()?.id == item.id,
                                            onImageClick = { selectedImageUrl = it }
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Dialogs
        if (showDatePicker) {
            AdvancedDatePickerDialog(
                initialStartDate = selectedDateRange.first,
                initialEndDate = selectedDateRange.second,
                onDismiss = { showDatePicker = false },
                onDateRangeSelected = { start, end ->
                    onDateRangeSelected(start, end)
                    showDatePicker = false
                    val weekDiff = (start.toEpochDay() - today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toEpochDay()) / 7
                    scope.launch {
                        pagerState.scrollToPage(500 + weekDiff.toInt())
                    }
                }
            )
        }

        entryToDelete?.let { entry ->
            AlertDialog(
                onDismissRequest = { entryToDelete = null },
                title = { Text(stringResource(R.string.journal_delete_confirm_title)) },
                text = { Text(stringResource(R.string.journal_delete_confirm_message)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onDeleteEntry(entry.id)
                            entryToDelete = null
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text(stringResource(R.string.journal_delete_confirm_button))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { entryToDelete = null }) {
                        Text(stringResource(R.string.journal_cancel_button))
                    }
                }
            )
        }

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
            .height(IntrinsicSize.Min) 
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(44.dp) // Tightened
        ) {
            Text(
                text = timeFormatted,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.padding(bottom = 2.dp)
            )
            
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.TopCenter
            ) {
                if (!isLast) {
                    Canvas(modifier = Modifier.fillMaxHeight()) {
                        drawLine(
                            color = timelineColor.copy(alpha = 0.1f),
                            start = Offset(size.width / 2, 0f),
                            end = Offset(size.width / 2, size.height + 16f),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                }
                
                Surface(
                    modifier = Modifier.size(6.dp),
                    shape = CircleShape,
                    color = timelineColor.copy(alpha = 0.5f)
                ) {}
            }
        }

        Spacer(modifier = Modifier.width(4.dp))

        Card(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f) 
                                 else MaterialTheme.colorScheme.surface,
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 0.5.dp),
            border = BorderStroke(
                width = 0.5.dp, 
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (isDark) 0.1f else 0.2f)
            )
        ) {
            Column(
                modifier = Modifier.padding(10.dp)
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Surface(
                        color = timelineColor.copy(alpha = 0.08f),
                        shape = CircleShape,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(text = entry.emoji, fontSize = 18.sp)
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(10.dp))
                    
                    Text(
                        text = entry.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 18.sp,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (entry.imageUrl != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clickable { onImageClick(entry.imageUrl) },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        AsyncImage(
                            model = coil.request.ImageRequest.Builder(LocalContext.current)
                                .data(entry.imageUrl)
                                .crossfade(true)
                                .build(),
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
                                androidx.compose.foundation.pager.HorizontalPager(
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
                                                } else if (date == selectedStartDate) {
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
                        Text("HUỶ", fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    TextButton(
                        onClick = { onDateRangeSelected(selectedStartDate, selectedEndDate) }
                    ) {
                        Text("XÁC NHẬN", fontWeight = FontWeight.Bold)
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
    val offset = remember(displayMonth) { displayMonth.atDay(1).dayOfWeek.value - 1 }
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

@AppCombinedPreviews
@Composable
fun JournalScreenPreview() {
    NewStartTheme {
        JournalContent(
            entries = listOf(
                JournalEntry(id = "1", emoji = "😊", text = "Một ngày tuyệt vời tại UIT!", timestamp = Date()),
                JournalEntry(id = "2", emoji = "🥰", text = "Học Compose thú vị quá", timestamp = Date())
            ),
            selectedDateRange = LocalDate.now() to null,
            onDateRangeSelected = { _, _ -> },
            onQuickFilterSelected = {},
            onDeleteEntry = {}
        )
    }
}
