package com.example.newstart.ui.features.journal

import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import android.content.Intent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
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
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.newstart.ui.navigation.Screen
import androidx.compose.ui.platform.LocalView
import android.view.WindowManager
import android.graphics.Color as AndroidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import coil.compose.AsyncImage
import com.example.newstart.R
import com.example.newstart.domain.model.JournalEntry
import com.example.newstart.ui.components.AdvancedDatePickerDialog
import com.example.newstart.ui.features.journal.components.TimelineEntryItem
import com.example.newstart.ui.theme.LocalDarkTheme
import com.example.newstart.ui.theme.NewStartTheme
import com.example.newstart.ui.util.AppCombinedPreviews
import com.example.newstart.ui.util.ImageDownloader
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

@Composable
fun JournalScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    viewModel: JournalViewModel = hiltViewModel()
) {
    val entries by viewModel.entries.collectAsStateWithLifecycle()
    val socialFeed by viewModel.socialFeed.collectAsStateWithLifecycle()
    val selectedDateRange by viewModel.selectedDateRange.collectAsStateWithLifecycle()
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    
    JournalContent(
        modifier = modifier,
        entries = entries,
        socialFeed = socialFeed,
        selectedDateRange = selectedDateRange,
        currentTab = currentTab,
        onTabSelected = { viewModel.onTabSelected(it) },
        onDateRangeSelected = { start, end -> viewModel.onDateRangeSelected(start, end) },
        onQuickFilterSelected = { viewModel.setQuickFilter(it) },
        onDeleteEntry = { viewModel.deleteEntry(it) },
        onArchiveClick = { navController.navigate(Screen.JournalArchive.route) }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun JournalContent(
    modifier: Modifier = Modifier,
    entries: List<JournalEntry>,
    socialFeed: List<JournalEntry>,
    selectedDateRange: Pair<LocalDate, LocalDate?>,
    currentTab: Int,
    onTabSelected: (Int) -> Unit,
    onDateRangeSelected: (LocalDate, LocalDate?) -> Unit,
    onQuickFilterSelected: (String) -> Unit,
    onDeleteEntry: (String) -> Unit,
    onArchiveClick: () -> Unit
) {
    val isDark = LocalDarkTheme.current
    var selectedImageUrl by remember { mutableStateOf<String?>(null) }
    var entryToDelete by remember { mutableStateOf<JournalEntry?>(null) }
    var entryForOptions by remember { mutableStateOf<JournalEntry?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var previousDateRange by remember { mutableStateOf<Pair<LocalDate, LocalDate?>?>(null) }
    
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val isInspectionMode = LocalInspectionMode.current
    val today = LocalDate.now()
    
    val pagerState = rememberPagerState(pageCount = { 1000 }, initialPage = 500)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val locale = remember(context) { context.resources.configuration.locales[0] }
    val isVietnamese = locale.language == "vi"
    val timeFormatter = remember { SimpleDateFormat("HH:mm", locale) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            entryForOptions?.imageUrl?.let { url ->
                scope.launch { ImageDownloader.downloadImage(context, url) }
            }
        }
    }

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
                brush = remember(isDark, backgroundColor) {
                    Brush.verticalGradient(
                        colors = if (isDark) listOf(Color.Black, Color.Black)
                        else listOf(backgroundColor, backgroundColor) // Trắng đồng bộ ở chế độ sáng
                    )
                }
            )
    ) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            // Header với hiệu ứng Search Icon chạy từ phải sang trái
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 16.dp)
            ) {
                val constraintsScope = this
                val availableWidth = constraintsScope.maxWidth
                val searchIconSize = 44.dp

                // Vị trí bắt đầu (bên phải, trước icon lịch) và kết thúc (bên trái)
                val startOffset = availableWidth - (searchIconSize * 2)
                val endOffset = (-8).dp // Chỉnh một chút để khớp với padding của TextField

                val searchOffset by animateDpAsState(
                    targetValue = if (isSearchActive) endOffset else startOffset,
                    animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
                    label = "search_offset"
                )

                // 1. Tiêu đề (Fade out khi search)
                androidx.compose.animation.AnimatedVisibility(
                    visible = !isSearchActive,
                    enter = fadeIn(tween(200)),
                    exit = fadeOut(tween(200)),
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Text(
                        text = if (isVietnamese) "Nhật ký" else "Journal",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                // 2. Icon Lịch & Thư viện (Fade out khi search)
                androidx.compose.animation.AnimatedVisibility(
                    visible = !isSearchActive,
                    enter = fadeIn(tween(200)),
                    exit = fadeOut(tween(200)),
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onArchiveClick) {
                            Icon(
                                imageVector = Icons.Default.CollectionsBookmark,
                                contentDescription = "Archive",
                                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                            )
                        }
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = "Calendar",
                                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                            )
                        }
                    }
                }

                // 3. Khu vực Search
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .offset(x = searchOffset),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (isSearchActive) {
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier
                                .width(availableWidth + 12.dp)
                                .focusRequester(focusRequester),
                            placeholder = {
                                Text(
                                    stringResource(R.string.home_search_placeholder),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Search,
                                    null,
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            trailingIcon = {
                                IconButton(onClick = { 
                                    if (searchQuery.isNotEmpty()) searchQuery = ""
                                    else {
                                        isSearchActive = false
                                        focusManager.clearFocus()
                                    }
                                }) {
                                    Icon(Icons.Default.Close, null, modifier = Modifier.size(22.dp))
                                }
                            },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                cursorColor = MaterialTheme.colorScheme.primary
                            ),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() })
                        )
                    } else {
                        IconButton(onClick = { isSearchActive = true }) {
                            Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                        }
                    }
                }
            }

            // Tabs: Cá nhân | Cộng đồng
            TabRow(
                selectedTabIndex = currentTab,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary,
                divider = {},
                indicator = { tabPositions ->
                    if (currentTab < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[currentTab]),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                modifier = Modifier.padding(horizontal = 16.dp).height(44.dp)
            ) {
                Tab(
                    selected = currentTab == 0,
                    onClick = { onTabSelected(0) },
                    text = { Text(if (isVietnamese) "Cá nhân" else "Personal", fontWeight = FontWeight.Bold, fontSize = 14.sp) }
                )
                Tab(
                    selected = currentTab == 1,
                    onClick = { onTabSelected(1) },
                    text = { Text(if (isVietnamese) "Cộng đồng" else "Community", fontWeight = FontWeight.Bold, fontSize = 14.sp) }
                )
            }

            // Ngày tháng & Filter (Chỉ hiện khi ở tab Cá nhân)
            AnimatedVisibility(visible = currentTab == 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
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
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { onQuickFilterSelected("Today") }
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    QuickFiltersSection(
                        selectedDateRange = selectedDateRange,
                        onQuickFilterSelected = {
                            onQuickFilterSelected(it)
                            if (isSearchActive) previousDateRange = null
                        },
                        isVietnamese = isVietnamese,
                        isDark = isDark
                    )
                }
            }

            if (currentTab == 0) {
                JournalList(
                    filteredEntries = filteredEntries,
                    isDark = isDark,
                    searchQuery = searchQuery,
                    isVietnamese = isVietnamese,
                    locale = locale,
                    timeFormatter = timeFormatter,
                    onOptionsClick = { entryForOptions = it },
                    onImageClick = { selectedImageUrl = it }
                )
            } else {
                SocialFeedList(
                    socialFeed = socialFeed,
                    isVietnamese = isVietnamese,
                    isDark = isDark,
                    timeFormatter = timeFormatter,
                    onImageClick = { selectedImageUrl = it }
                )
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

        entryForOptions?.let { entry ->
            JournalOptionsSheet(
                onDismiss = { entryForOptions = null },
                onShare = { /* TODO */ },
                onDownload = {
                    entry.imageUrl?.let { url ->
                        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q &&
                            androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                            permissionLauncher.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        } else {
                            scope.launch {
                                ImageDownloader.downloadImage(context, url)
                            }
                        }
                    } ?: run {
                        android.widget.Toast.makeText(context, "Bài viết không có ảnh", android.widget.Toast.LENGTH_SHORT).show()
                    }
                },
                onDelete = {
                    entryToDelete = entry
                    entryForOptions = null
                }
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
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(end = 20.dp)
    ) {
        val filters = listOf("All" to R.string.habits_filter_all, "Month" to null)
        items(filters) { (key, labelRes) ->
            val label = when {
                labelRes != null -> stringResource(labelRes)
                key == "Month" -> if (isVietnamese) "Tháng này" else "This Month"
                else -> key
            }

            val isSelected = remember(selectedDateRange, key) {
                val today = LocalDate.now()
                when (key) {
                    "Month" -> selectedDateRange.first == today.withDayOfMonth(1) && selectedDateRange.second == today.withDayOfMonth(today.lengthOfMonth())
                    "All" -> selectedDateRange.first == LocalDate.of(2000, 1, 1)
                    else -> false
                }
            }

            FilterChip(
                selected = isSelected,
                onClick = { onQuickFilterSelected(key) },
                label = { Text(text = label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium) },
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
    isDark: Boolean,
    searchQuery: String,
    isVietnamese: Boolean,
    locale: Locale,
    timeFormatter: SimpleDateFormat,
    onOptionsClick: (JournalEntry) -> Unit,
    onImageClick: (String) -> Unit
) {
    val focusManager = LocalFocusManager.current
    val listBackgroundColor = if (isDark) Color.Black else MaterialTheme.colorScheme.background
    
    Box(
        modifier = Modifier.fillMaxSize()
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
                contentPadding = PaddingValues(top = 2.dp, bottom = 100.dp, start = 12.dp, end = 12.dp)
            ) {
                groupedEntries.forEach { (date, entriesInDate) ->
                    stickyHeader {
                        Surface(
                            modifier = Modifier.fillMaxWidth(), 
                            color = listBackgroundColor.copy(alpha = 0.98f)
                        ) {
                            Text(
                                text = date.format(DateTimeFormatter.ofPattern(if (isVietnamese) "dd MMMM, yyyy" else "MMMM dd, yyyy", locale)),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                modifier = Modifier.padding(top = 4.dp, bottom = 10.dp, start = 12.dp, end = 12.dp)
                            )
                        }
                    }

                    items(items = entriesInDate, key = { it.id }) { entry ->
                        TimelineEntryItem(
                            entry = entry,
                            timeFormatted = remember(entry.timestamp) { entry.timestamp?.let { timeFormatter.format(it) } ?: "--:--" },
                            isLast = entriesInDate.last().id == entry.id,
                            onImageClick = onImageClick,
                            onOptionsClick = { onOptionsClick(entry) }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JournalOptionsSheet(
    onDismiss: () -> Unit,
    onShare: () -> Unit,
    onDownload: () -> Unit,
    onDelete: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 40.dp, top = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TextButton(
                onClick = { onShare(); onDismiss() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Chia sẻ", style = MaterialTheme.typography.bodyLarge)
            }
            
            TextButton(
                onClick = { onDownload(); onDismiss() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Tải về", style = MaterialTheme.typography.bodyLarge)
            }
            
            TextButton(
                onClick = onDelete,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Xóa", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error)
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Hủy", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val view = LocalView.current

    // Thiết lập Edge-to-Edge cực đoan cho Dialog Window
    SideEffect {
        val window = (view.parent as? DialogWindowProvider)?.window
        window?.let {
            // Cho phép vẽ tràn ra ngoài mọi giới hạn hệ thống
            it.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
            
            // Ép vẽ vào cả vùng tai thỏ (API 28+)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                it.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
            
            // Xóa bỏ hiệu ứng làm mờ nền của Dialog (dim)
            it.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            
            WindowCompat.setDecorFitsSystemWindows(it, false)
            it.statusBarColor = AndroidColor.TRANSPARENT
            it.navigationBarColor = AndroidColor.TRANSPARENT
            it.setBackgroundDrawableResource(android.R.color.transparent)
            
            it.setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT
            )

            WindowCompat.getInsetsController(it, view).isAppearanceLightStatusBars = false
        }
    }

    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var isUiVisible by remember { mutableStateOf(true) }

    // Tự động ẩn/hiện thanh trạng thái giống Messenger
    LaunchedEffect(isUiVisible) {
        val window = (view.parent as? DialogWindowProvider)?.window
        window?.let {
            val controller = WindowCompat.getInsetsController(it, view)
            if (isUiVisible) {
                controller.show(WindowInsetsCompat.Type.systemBars())
            } else {
                controller.hide(WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
    }

    // Tính toán độ mờ của background và độ bo góc dựa trên trạng thái Zoom/UI
    val isZoomed = scale > 1f
    val backgroundAlpha by animateFloatAsState(
        targetValue = if (isUiVisible && !isZoomed) 0.4f else 0f,
        label = "bg_alpha"
    )
    // Bo góc về 0 khi ẩn UI hoặc khi đang Zoom
    val imageCornerRadius by animateDpAsState(
        targetValue = if (isZoomed || !isUiVisible) 0.dp else 24.dp,
        label = "corner_radius"
    )
    // Padding về 0 khi ẩn UI hoặc khi đang Zoom để ảnh tràn màn hình
    val horizontalPadding by animateDpAsState(
        targetValue = if (isZoomed || !isUiVisible) 0.dp else 16.dp,
        label = "padding"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // Blurred Background - Bây giờ sẽ tràn toàn bộ kể cả status bar
            AsyncImage(
                model = url,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        // Tăng scale mạnh hơn để đảm bảo không bị hở viền khi blur cực nặng
                        scaleX = 1.3f
                        scaleY = 1.3f
                    }
                    .blur(40.dp),
                contentScale = ContentScale.Crop,
                alpha = backgroundAlpha
            )

            // Main Image with Zoom and Pan
            var containerSize by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { containerSize = it }
                    .pointerInput(Unit) {
                        detectTransformGestures { centroid, pan, zoom, _ ->
                            val oldScale = scale
                            scale = (scale * zoom).coerceIn(1f, 5f)
                            
                            if (scale > 1f) {
                                isUiVisible = false
                                // Công thức chuẩn: Zoom vào tâm điểm ngón tay
                                // Cần trừ đi một nửa kích thước container để đưa centroid về hệ tọa độ tâm
                                val center = Offset(containerSize.width / 2f, containerSize.height / 2f)
                                val relativeCentroid = centroid - center
                                offset = (offset + pan) * (scale / oldScale) + (relativeCentroid * (1 - scale / oldScale))
                            } else {
                                offset = Offset.Zero
                                isUiVisible = true
                            }
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = { tapOffset ->
                                if (scale > 1f) {
                                    scale = 1f
                                    offset = Offset.Zero
                                    isUiVisible = true
                                } else {
                                    scale = 3f
                                    val center = Offset(containerSize.width / 2f, containerSize.height / 2f)
                                    val relativeTap = tapOffset - center
                                    offset = relativeTap * (1 - 3f)
                                    isUiVisible = false
                                }
                            },
                            onTap = { 
                                if (scale > 1f) {
                                    scale = 1f
                                    offset = Offset.Zero
                                    isUiVisible = true
                                } else {
                                    isUiVisible = !isUiVisible
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = url,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .padding(horizontal = if (isZoomed) 0.dp else 16.dp)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = offset.x
                            translationY = offset.y
                            
                            // Luôn áp dụng bo góc, radius sẽ tự về 0 khi zoom nhờ animateDpAsState
                            shape = RoundedCornerShape(imageCornerRadius)
                            clip = true
                        },
                    contentScale = ContentScale.Fit
                )
            }

            // Messenger Style Top Bar
            AnimatedVisibility(
                visible = isUiVisible && !isZoomed,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                scope.launch {
                                    ImageDownloader.downloadImage(context, url)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "Download",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, "Xem khoảnh khắc từ NewStart Journal: $url")
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, null))
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SocialFeedList(
    socialFeed: List<JournalEntry>,
    isVietnamese: Boolean,
    isDark: Boolean,
    timeFormatter: SimpleDateFormat,
    onImageClick: (String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)),
        color = if (isDark) Color.Black else MaterialTheme.colorScheme.background
    ) {
        if (socialFeed.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.Group, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                Spacer(Modifier.height(16.dp))
                Text(
                    if (isVietnamese) "Bảng tin cộng đồng sắp ra mắt!" else "Community feed coming soon!",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    if (isVietnamese) "Kết nối với bạn bè để cùng nhau kỷ luật" else "Connect with friends to stay disciplined together",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp, start = 12.dp, end = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(items = socialFeed, key = { it.id }) { entry ->
                    SocialFeedItem(
                        entry = entry,
                        timeFormatted = remember(entry.timestamp) { entry.timestamp?.let { timeFormatter.format(it) } ?: "--:--" },
                        onImageClick = onImageClick
                    )
                }
            }
        }
    }
}

@Composable
fun SocialFeedItem(
    entry: JournalEntry,
    timeFormatted: String,
    onImageClick: (String) -> Unit
) {
    val isDark = LocalDarkTheme.current
    
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (isDark) MaterialTheme.colorScheme.surface 
                             else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = if (isDark) 2.dp else 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(36.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Person, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "User ${entry.userId.take(5)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = timeFormatted,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
                
                val moodIcon = remember(entry.emoji) {
                    when (entry.emoji) {
                        "😫" -> R.drawable.ic_mood_very_bad
                        "😔" -> R.drawable.ic_mood_bad
                        "😐" -> R.drawable.ic_mood_neutral
                        "😊" -> R.drawable.ic_mood_good
                        "🥰" -> R.drawable.ic_mood_very_good
                        else -> null
                    }
                }
                
                if (moodIcon != null) {
                    androidx.compose.foundation.Image(
                        painter = painterResource(id = moodIcon),
                        contentDescription = null,
                        modifier = Modifier.size(28.dp)
                    )
                } else {
                    Text(entry.emoji, fontSize = 24.sp)
                }
            }
            
            if (entry.text.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = entry.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            entry.imageUrl?.let { url ->
                Spacer(modifier = Modifier.height(12.dp))
                AsyncImage(
                    model = url,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(24.dp))
                        .clickable { onImageClick(url) },
                    contentScale = ContentScale.Crop
                )
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
                JournalEntry(id = "1", emoji = "😊", text = "Một ngày tuyệt vời!", timestamp = Date()),
                JournalEntry(id = "2", emoji = "🥰", text = "Học Compose thú vị quá", timestamp = Date())
            ),
            socialFeed = emptyList(),
            selectedDateRange = LocalDate.now() to null,
            currentTab = 0,
            onTabSelected = {},
            onDateRangeSelected = { _, _ -> },
            onQuickFilterSelected = {},
            onDeleteEntry = {},
            onArchiveClick = {}
        )
    }
}
