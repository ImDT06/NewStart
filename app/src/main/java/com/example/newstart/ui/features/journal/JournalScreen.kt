package com.example.newstart.ui.features.journal

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.newstart.R
import com.example.newstart.domain.model.JournalEntry
import com.example.newstart.ui.components.AdvancedDatePickerDialog
import com.example.newstart.ui.features.journal.components.TimelineEntryItem
import com.example.newstart.ui.theme.LocalDarkTheme
import com.example.newstart.ui.theme.NewStartTheme
import com.example.newstart.ui.util.AppCombinedPreviews
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.*

@Composable
fun JournalScreen(
    modifier: Modifier = Modifier,
    viewModel: JournalViewModel = hiltViewModel()
) {
    val entries by viewModel.entries.collectAsStateWithLifecycle()
    val selectedDateRange by viewModel.selectedDateRange.collectAsStateWithLifecycle()
    
    JournalContent(
        modifier = modifier,
        entries = entries,
        selectedDateRange = selectedDateRange,
        onDateRangeSelected = { start, end -> viewModel.onDateRangeSelected(start, end) },
        onQuickFilterSelected = { viewModel.setQuickFilter(it) },
        onDeleteEntry = { viewModel.deleteEntry(it) }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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
    val isDark = LocalDarkTheme.current
    val today = LocalDate.now()

    LaunchedEffect(isSearchActive) {
        if (isSearchActive) {
            previousDateRange = selectedDateRange
            onQuickFilterSelected("All")
            if (!isInspectionMode) focusRequester.requestFocus()
        } else {
            previousDateRange?.let {
                if (selectedDateRange.first == LocalDate.of(2000, 1, 1)) {
                    onDateRangeSelected(it.first, it.second)
                }
                previousDateRange = null
            }
        }
    }
    
    val pagerState = rememberPagerState(pageCount = { 1000 }, initialPage = 500)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val locale = remember(context) { context.resources.configuration.locales[0] }
    val isVietnamese = locale.language == "vi"
    val timeFormatter = remember { SimpleDateFormat("HH:mm", locale) }

    val filteredEntries by remember(entries, searchQuery) {
        derivedStateOf {
            if (searchQuery.isEmpty()) entries
            else entries.filter { it.text.contains(searchQuery, ignoreCase = true) || it.emoji.contains(searchQuery) }
        }
    }
    
    val backgroundColor = MaterialTheme.colorScheme.background
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = remember(isDark, backgroundColor, primaryContainer) {
                    Brush.verticalGradient(
                        colors = if (isDark) listOf(Color(0xFF001A33), backgroundColor)
                        else listOf(primaryContainer.copy(alpha = 0.4f), backgroundColor)
                    )
                }
            )
    ) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            JournalHeader(
                isSearchActive = isSearchActive,
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                onSearchToggle = { isSearchActive = it },
                onShowDatePicker = { showDatePicker = true },
                focusRequester = focusRequester,
                focusManager = focusManager,
                selectedDateRange = selectedDateRange,
                onQuickFilterSelected = onQuickFilterSelected,
                isVietnamese = isVietnamese,
                locale = locale
            )

            QuickFiltersSection(
                selectedDateRange = selectedDateRange,
                onQuickFilterSelected = { 
                    onQuickFilterSelected(it)
                    if (isSearchActive) previousDateRange = null
                },
                isVietnamese = isVietnamese,
                isDark = isDark
            )

            JournalList(
                filteredEntries = filteredEntries,
                searchQuery = searchQuery,
                isVietnamese = isVietnamese,
                locale = locale,
                timeFormatter = timeFormatter,
                onDeleteRequest = { entryToDelete = it },
                onImageClick = { selectedImageUrl = it }
            )
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
                    scope.launch { pagerState.scrollToPage(500 + weekDiff.toInt()) }
                }
            )
        }

        entryToDelete?.let { entry ->
            DeleteConfirmDialog(
                onDismiss = { entryToDelete = null },
                onConfirm = {
                    onDeleteEntry(entry.id)
                    entryToDelete = null
                }
            )
        }

        selectedImageUrl?.let { url ->
            ImagePreviewDialog(url = url, onDismiss = { selectedImageUrl = null })
        }
    }
}

@Composable
private fun JournalHeader(
    isSearchActive: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSearchToggle: (Boolean) -> Unit,
    onShowDatePicker: () -> Unit,
    focusRequester: FocusRequester,
    focusManager: FocusManager,
    selectedDateRange: Pair<LocalDate, LocalDate?>,
    onQuickFilterSelected: (String) -> Unit,
    isVietnamese: Boolean,
    locale: Locale
) {
    val today = LocalDate.now()
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
                transitionSpec = { (fadeIn() + expandHorizontally()).togetherWith(fadeOut() + shrinkHorizontally()) },
                modifier = Modifier.weight(1f),
                label = "search_header"
            ) { searching ->
                if (searching) {
                    TextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                        placeholder = { Text(stringResource(R.string.home_search_placeholder), fontSize = 14.sp) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                        ),
                        trailingIcon = {
                            IconButton(onClick = { 
                                onSearchToggle(false)
                                onSearchQueryChange("")
                                focusManager.clearFocus()
                            }) { Icon(Icons.Default.Close, null) }
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
                    IconButton(onClick = { onSearchToggle(true) }) {
                        Icon(Icons.Default.Search, null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                    }
                    IconButton(onClick = onShowDatePicker) {
                        Icon(Icons.Default.CalendarMonth, null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
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
                    else start.format(DateTimeFormatter.ofPattern(if (isVietnamese) "dd MMMM" else "MMMM dd", locale))
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
                modifier = Modifier.offset(y = (-8).dp).clickable { onQuickFilterSelected("Today") }
            )
        }
    }
}

@Composable
private fun QuickFiltersSection(
    selectedDateRange: Pair<LocalDate, LocalDate?>,
    onQuickFilterSelected: (String) -> Unit,
    isVietnamese: Boolean,
    isDark: Boolean
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 0.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(end = 20.dp)
    ) {
        val filters = listOf("Today" to R.string.habits_today, "All" to R.string.habits_filter_all, "Month" to null)
        items(filters) { (key, labelRes) ->
            val label = when {
                labelRes != null -> stringResource(labelRes)
                key == "Month" -> if (isVietnamese) "Tháng này" else "This Month"
                else -> key
            }

            val isSelected = remember(selectedDateRange, key) {
                val today = LocalDate.now()
                when (key) {
                    "Today" -> selectedDateRange.first == today && selectedDateRange.second == null
                    "Month" -> selectedDateRange.first == today.withDayOfMonth(1) && selectedDateRange.second == today.withDayOfMonth(today.lengthOfMonth())
                    "All" -> selectedDateRange.first == LocalDate.of(2000, 1, 1)
                    else -> false
                }
            }

            FilterChip(
                selected = isSelected,
                onClick = { onQuickFilterSelected(key) },
                label = { Text(text = label, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium) },
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
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun JournalList(
    filteredEntries: List<JournalEntry>,
    searchQuery: String,
    isVietnamese: Boolean,
    locale: Locale,
    timeFormatter: SimpleDateFormat,
    onDeleteRequest: (JournalEntry) -> Unit,
    onImageClick: (String) -> Unit
) {
    val focusManager = LocalFocusManager.current
    Surface(
        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        if (filteredEntries.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                val emptyMsg = if (searchQuery.isEmpty()) stringResource(R.string.journal_empty_message)
                              else if (isVietnamese) "Không tìm thấy nhật ký phù hợp" else "No matching journal found"
                Text(emptyMsg, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            val groupedEntries by remember(filteredEntries) {
                derivedStateOf {
                    filteredEntries.groupBy { entry ->
                        entry.timestamp?.let {
                            java.time.Instant.ofEpochMilli(it.time).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                        } ?: LocalDate.now()
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures(onTap = { focusManager.clearFocus() }) },
                contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp, start = 12.dp, end = 12.dp)
            ) {
                groupedEntries.forEach { (date, entriesInDate) ->
                    stickyHeader {
                        Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)) {
                            Text(
                                text = date.format(DateTimeFormatter.ofPattern(if (isVietnamese) "dd MMMM, yyyy" else "MMMM dd, yyyy", locale)),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp)
                            )
                        }
                    }

                    items(items = entriesInDate, key = { it.id }) { entry ->
                        JournalSwipeableItem(
                            entry = entry,
                            timeFormatted = remember(entry.timestamp) { entry.timestamp?.let { timeFormatter.format(it) } ?: "--:--" },
                            isLast = entriesInDate.last().id == entry.id,
                            onDeleteRequest = onDeleteRequest,
                            onImageClick = onImageClick
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JournalSwipeableItem(
    entry: JournalEntry,
    timeFormatted: String,
    isLast: Boolean,
    onDeleteRequest: (JournalEntry) -> Unit,
    onImageClick: (String) -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (it == SwipeToDismissBoxValue.EndToStart) {
                onDeleteRequest(entry)
                false
            } else false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        modifier = Modifier.padding(bottom = 12.dp),
        backgroundContent = {
            val isDismissing = dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart
            val color = if (isDismissing) MaterialTheme.colorScheme.errorContainer else Color.Transparent
            val scale by animateFloatAsState(if (isDismissing) 1.2f else 1f, label = "icon_scale")
            
            Box(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(20.dp)).background(color), contentAlignment = Alignment.CenterEnd) {
                Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(end = 24.dp).scale(scale))
            }
        },
        content = {
            TimelineEntryItem(entry = entry, timeFormatted = timeFormatted, isLast = isLast, onImageClick = onImageClick)
        }
    )
}

@Composable
private fun DeleteConfirmDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.journal_delete_confirm_title)) },
        text = { Text(stringResource(R.string.journal_delete_confirm_message)) },
        confirmButton = {
            TextButton(onClick = onConfirm, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                Text(stringResource(R.string.journal_delete_confirm_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.journal_cancel_button)) }
        }
    )
}

@Composable
private fun ImagePreviewDialog(url: String, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.9f)).clickable { onDismiss() }, contentAlignment = Alignment.Center) {
            AsyncImage(model = url, contentDescription = null, modifier = Modifier.fillMaxWidth(0.95f).clip(RoundedCornerShape(24.dp)), contentScale = ContentScale.Fit)
        }
    }
}

@AppCombinedPreviews
@Composable
fun JournalScreenPreview() {
    NewStartTheme {
        JournalContent(
            entries = listOf(
                JournalEntry(id = "1", emoji = "😊", text = "Một ngày tuyệt vời!", timestamp = Date()),
                JournalEntry(id = "2", emoji = "🥰", text = "Học Compose thú vị quá", timestamp = Date())
            ),
            selectedDateRange = LocalDate.now() to null,
            onDateRangeSelected = { _, _ -> },
            onQuickFilterSelected = {},
            onDeleteEntry = {}
        )
    }
}
