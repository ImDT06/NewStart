package com.example.newstart.ui.features.journal

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.newstart.R
import com.example.newstart.domain.model.JournalEntry
import com.example.newstart.domain.model.JournalType
import com.example.newstart.ui.theme.LocalDarkTheme
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalArchiveScreen(
    navController: NavController,
    viewModel: JournalArchiveViewModel = hiltViewModel()
) {
    val isDark = LocalDarkTheme.current
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val movies by viewModel.movieGroups.collectAsStateWithLifecycle()
    val books by viewModel.bookGroups.collectAsStateWithLifecycle()
    val subjects by viewModel.subjectGroups.collectAsStateWithLifecycle()
    val tags by viewModel.tagGroups.collectAsStateWithLifecycle()
    val moodAnalytics by viewModel.moodAnalytics.collectAsStateWithLifecycle()

    var selectedImageUrl by remember { mutableStateOf<String?>(null) }
    var expandedMovieGroup by remember { mutableStateOf<MovieGroup?>(null) }
    var expandedBookGroup by remember { mutableStateOf<BookGroup?>(null) }
    var expandedSubjectGroup by remember { mutableStateOf<SubjectGroup?>(null) }
    var expandedTagGroup by remember { mutableStateOf<TagGroup?>(null) }

    val context = LocalContext.current
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isVietnamese = remember(configuration) {
        configuration.locales[0].language == "vi"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDark) Color.Black else MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Text(
                    text = if (isVietnamese) "Thư viện lưu trữ" else "Library Archive",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // Tabs
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary,
                edgePadding = 16.dp,
                divider = {},
                indicator = { tabPositions ->
                    if (selectedTab < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { viewModel.selectTab(0) },
                    text = {
                        val isSelected = selectedTab == 0
                        Text(
                            text = if (isVietnamese) "Phim (${movies.size})" else "Movies (${movies.size})",
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { viewModel.selectTab(1) },
                    text = {
                        val isSelected = selectedTab == 1
                        Text(
                            text = if (isVietnamese) "Sách (${books.size})" else "Books (${books.size})",
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { viewModel.selectTab(2) },
                    text = {
                        val isSelected = selectedTab == 2
                        Text(
                            text = if (isVietnamese) "Môn học (${subjects.size})" else "Subjects (${subjects.size})",
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { viewModel.selectTab(3) },
                    text = {
                        val isSelected = selectedTab == 3
                        Text(
                            text = if (isVietnamese) "Nhãn (${tags.size})" else "Tags (${tags.size})",
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 4,
                    onClick = { viewModel.selectTab(4) },
                    text = {
                        val isSelected = selectedTab == 4
                        Text(
                            text = if (isVietnamese) "Phân tích" else "Insights",
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Content Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (selectedTab) {
                    0 -> MovieArchiveGrid(movies, onMovieClick = { expandedMovieGroup = it })
                    1 -> BookArchiveGrid(books, onBookClick = { expandedBookGroup = it })
                    2 -> SubjectArchiveGrid(subjects, onSubjectClick = { expandedSubjectGroup = it })
                    3 -> TagArchiveGrid(tags, onTagClick = { expandedTagGroup = it })
                    4 -> MoodAnalyticsView(moodAnalytics, isVietnamese, isDark)
                }
            }
        }

        // Expanded Bottom Sheet Overlays
        expandedMovieGroup?.let { group ->
            ExpandedGroupSheet(
                title = group.title,
                subtitle = if (isVietnamese) "Phim ảnh" else "Movie",
                rating = group.averageRating,
                entries = group.entries,
                isDark = isDark,
                onDismiss = { expandedMovieGroup = null },
                onImageClick = { selectedImageUrl = it }
            )
        }

        expandedBookGroup?.let { group ->
            ExpandedGroupSheet(
                title = group.title,
                subtitle = if (isVietnamese) "Sách đọc" else "Book",
                rating = group.averageRating,
                entries = group.entries,
                isDark = isDark,
                onDismiss = { expandedBookGroup = null },
                onImageClick = { selectedImageUrl = it }
            )
        }

        expandedSubjectGroup?.let { group ->
            ExpandedGroupSheet(
                title = group.name,
                subtitle = if (isVietnamese) "Môn học" else "Subject",
                rating = group.averageUnderstanding,
                entries = group.entries,
                isDark = isDark,
                isSubject = true,
                onDismiss = { expandedSubjectGroup = null },
                onImageClick = { selectedImageUrl = it }
            )
        }

        expandedTagGroup?.let { group ->
            ExpandedGroupSheet(
                title = group.tag,
                subtitle = if (isVietnamese) "Nhãn" else "Tag",
                rating = 0f,
                entries = group.entries,
                isDark = isDark,
                onDismiss = { expandedTagGroup = null },
                onImageClick = { selectedImageUrl = it }
            )
        }

        selectedImageUrl?.let { url ->
            ImagePreviewDialog(url = url, onDismiss = { selectedImageUrl = null })
        }
    }
}

@Composable
private fun MovieArchiveGrid(
    movies: List<MovieGroup>,
    onMovieClick: (MovieGroup) -> Unit
) {
    if (movies.isEmpty()) {
        EmptyStateView(message = "Chưa có phim nào được lưu")
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(movies, key = { it.title }) { movie ->
            ArchiveCard(
                title = movie.title,
                rating = movie.averageRating,
                imageCount = movie.entries.count { it.imageUrl != null },
                totalCount = movie.entries.size,
                latestEntry = movie.entries.firstOrNull(),
                onClick = { onMovieClick(movie) }
            )
        }
    }
}

@Composable
private fun BookArchiveGrid(
    books: List<BookGroup>,
    onBookClick: (BookGroup) -> Unit
) {
    if (books.isEmpty()) {
        EmptyStateView(message = "Chưa có sách nào được lưu")
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(books, key = { it.title }) { book ->
            ArchiveCard(
                title = book.title,
                rating = book.averageRating,
                imageCount = book.entries.count { it.imageUrl != null },
                totalCount = book.entries.size,
                latestEntry = book.entries.firstOrNull(),
                onClick = { onBookClick(book) }
            )
        }
    }
}

@Composable
private fun SubjectArchiveGrid(
    subjects: List<SubjectGroup>,
    onSubjectClick: (SubjectGroup) -> Unit
) {
    if (subjects.isEmpty()) {
        EmptyStateView(message = "Chưa có môn học nào được lưu")
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(subjects, key = { it.name }) { subject ->
            ArchiveCard(
                title = subject.name,
                rating = subject.averageUnderstanding,
                imageCount = subject.entries.count { it.imageUrl != null },
                totalCount = subject.entries.size,
                latestEntry = subject.entries.firstOrNull(),
                isSubject = true,
                onClick = { onSubjectClick(subject) }
            )
        }
    }
}

@Composable
private fun ArchiveCard(
    title: String,
    rating: Float,
    imageCount: Int,
    totalCount: Int,
    latestEntry: JournalEntry?,
    isSubject: Boolean = false,
    onClick: () -> Unit
) {
    val isDark = LocalDarkTheme.current
    val hasImage = latestEntry?.imageUrl != null
    val cardShape = RoundedCornerShape(24.dp)

    val borderStroke = if (hasImage) {
        BorderStroke(
            1.dp,
            if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.05f)
        )
    } else {
        BorderStroke(
            1.dp,
            if (isDark) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            else MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
        )
    }

    val shadowElevation = if (hasImage) {
        0.dp
    } else {
        if (isDark) 0.dp else 3.dp
    }

    Surface(
        onClick = onClick,
        shape = cardShape,
        color = if (hasImage) {
            if (isDark) Color(0xFF1E1E1E) else Color(0xFFF2F2F2)
        } else {
            Color.Transparent
        },
        border = borderStroke,
        shadowElevation = shadowElevation,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.9f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (!hasImage) {
                        if (isDark) {
                            Modifier.background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.05f)
                                    )
                                )
                            )
                        } else {
                            Modifier.background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                                        MaterialTheme.colorScheme.surface
                                    )
                                )
                            )
                        }
                    } else {
                        Modifier
                    }
                )
        ) {
            // Background preview image if available
            if (latestEntry?.imageUrl != null) {
                AsyncImage(
                    model = latestEntry.imageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                // Gradient scrim overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.15f),
                                    Color.Black.copy(alpha = 0.85f)
                                )
                            )
                        )
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header: Total logs
                if (hasImage) {
                    // Glassmorphic translucent dark badge for image cards
                    Row(
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (imageCount > 0) Icons.Default.PhotoLibrary else Icons.Default.Description,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$totalCount ghi chép",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    // Modern styled color tag for non-image cards
                    val badgeBgColor = if (isDark) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    } else {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                    }
                    val badgeTextColor = if (isDark) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.primary
                    }

                    Row(
                        modifier = Modifier
                            .background(badgeBgColor, CircleShape)
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (imageCount > 0) Icons.Default.PhotoLibrary else Icons.Default.Description,
                            contentDescription = null,
                            tint = badgeTextColor,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$totalCount ghi chép",
                            color = badgeTextColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Bottom: Title & Rating
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = if (hasImage) Color.White else MaterialTheme.colorScheme.onSurface
                    )

                    if (rating > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = if (isSubject) Color(0xFF00C851) else Color(0xFFFFCC00),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = String.format(Locale.US, "%.1f", rating),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (hasImage) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpandedGroupSheet(
    title: String,
    subtitle: String,
    rating: Float,
    entries: List<JournalEntry>,
    isDark: Boolean,
    isSubject: Boolean = false,
    onDismiss: () -> Unit,
    onImageClick: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = if (isDark) Color(0xFF121212) else MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
        ) {
            // Header info
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
            ) {
                Text(
                    text = subtitle.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (rating > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        (1..5).forEach { i ->
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = if (i <= rating) (if (isSubject) Color(0xFF00C851) else Color(0xFFFFCC00)) else Color.Gray.copy(alpha = 0.25f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = String.format("%.1f", rating),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
            )

            // Compiled Timeline List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(entries, key = { it.id }) { entry ->
                    ArchiveEntryItem(entry, isDark, isSubject, onImageClick)
                }
            }
        }
    }
}

@Composable
private fun ArchiveEntryItem(
    entry: JournalEntry,
    isDark: Boolean,
    isSubject: Boolean,
    onImageClick: (String) -> Unit
) {
    val context = LocalContext.current
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val locale = remember(configuration) { configuration.locales[0] }
    val dateFormatter = remember(locale) { SimpleDateFormat("dd MMMM, yyyy HH:mm", locale) }
    val formattedDate = remember(entry.timestamp) {
        entry.timestamp?.let { dateFormatter.format(it) } ?: ""
    }

    val moodIcons = remember {
        mapOf(
            "😫" to R.drawable.ic_mood_very_bad,
            "😔" to R.drawable.ic_mood_bad,
            "😐" to R.drawable.ic_mood_neutral,
            "😊" to R.drawable.ic_mood_good,
            "🥰" to R.drawable.ic_mood_very_good
        )
    }

    Surface(
        color = if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.12f)
        else MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(
            1.dp,
            if (isDark) Color.White.copy(alpha = 0.06f)
            else MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
        ),
        shadowElevation = if (isDark) 0.dp else 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            // Left Accent Strip
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.primary)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(14.dp)
            ) {
                // Header: Date & Mood
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Date Chip Badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.06f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = formattedDate,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        Spacer(modifier = Modifier.width(6.dp))
                        
                        val privacyIcon = when (entry.privacy) {
                            com.example.newstart.domain.model.JournalPrivacy.PRIVATE -> Icons.Default.Lock
                            com.example.newstart.domain.model.JournalPrivacy.FRIENDS -> Icons.Default.Groups
                            com.example.newstart.domain.model.JournalPrivacy.SQUAD -> Icons.Default.GroupWork
                        }
                        
                        Icon(
                            imageVector = privacyIcon,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                    }

                    // Mood Icon
                    val icon = moodIcons[entry.emoji]
                    if (icon != null) {
                        Image(
                            painter = painterResource(id = icon),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    } else if (entry.emoji.isNotEmpty()) {
                        Text(text = entry.emoji, fontSize = 14.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Body text
                if (entry.text.isNotEmpty()) {
                    Text(
                        text = entry.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 20.sp
                    )
                }

                // Image Preview (scenes/slides/quotes snapshot)
                if (entry.imageUrl != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    AsyncImage(
                        model = entry.imageUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1.5f)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onImageClick(entry.imageUrl) },
                        contentScale = ContentScale.Crop
                    )
                }

                // Specific rating info if different from group
                val entryRating = when (entry.type) {
                    JournalType.MOVIE -> entry.movieDetails?.rating
                    JournalType.BOOK -> entry.bookDetails?.rating
                    JournalType.SUBJECT -> entry.subjectDetails?.understandingLevel?.toFloat()
                    else -> null
                }

                if (entryRating != null && entryRating > 0) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(
                                if (isSubject) Color(0xFF00C851).copy(alpha = 0.08f)
                                else Color(0xFFFFCC00).copy(alpha = 0.1f),
                                RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        (1..5).forEach { i ->
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = if (i <= entryRating) (if (isSubject) Color(0xFF00C851) else Color(0xFFFFCC00)) else Color.Gray.copy(alpha = 0.25f),
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyStateView(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CollectionsBookmark,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
                modifier = Modifier.size(64.dp)
            )
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun TagArchiveGrid(
    tags: List<TagGroup>,
    onTagClick: (TagGroup) -> Unit
) {
    if (tags.isEmpty()) {
        EmptyStateView(message = "Chưa có nhãn tag nào được sử dụng (ví dụ: #game, #chill)")
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(tags, key = { it.tag }) { tagGroup ->
            ArchiveCard(
                title = tagGroup.tag,
                rating = 0f,
                imageCount = tagGroup.entries.count { it.imageUrl != null },
                totalCount = tagGroup.entries.size,
                latestEntry = tagGroup.entries.firstOrNull(),
                onClick = { onTagClick(tagGroup) }
            )
        }
    }
}

@Composable
private fun MoodAnalyticsView(
    analytics: MoodAnalytics?,
    isVietnamese: Boolean,
    isDark: Boolean
) {
    if (analytics == null) {
        EmptyStateView(message = if (isVietnamese) "Chưa có ghi chép nào để phân tích" else "No entries to analyze")
        return
    }

    val totalLogs = analytics.moodCounts.values.sum()
    if (totalLogs == 0) {
        EmptyStateView(message = if (isVietnamese) "Chưa có ghi chép nào để phân tích" else "No entries to analyze")
        return
    }

    val moodIcons = remember {
        mapOf(
            "😫" to R.drawable.ic_mood_very_bad,
            "😔" to R.drawable.ic_mood_bad,
            "😐" to R.drawable.ic_mood_neutral,
            "😊" to R.drawable.ic_mood_good,
            "🥰" to R.drawable.ic_mood_very_good
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Section 1: Mood Distribution Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                    else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                ),
                border = BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = if (isVietnamese) "Phân bổ tâm trạng" else "Mood Distribution",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isVietnamese) "Dựa trên $totalLogs ghi chép cảm xúc" else "Based on $totalLogs mood logs",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    val moodList = listOf(
                        Triple("🥰", if (isVietnamese) "Rất vui" else "Excellent", Color(0xFF00C851)),
                        Triple("😊", if (isVietnamese) "Vui vẻ" else "Good", Color(0xFFFFCC00)),
                        Triple("😐", if (isVietnamese) "Bình thường" else "Neutral", Color(0xFF33B5E5)),
                        Triple("😔", if (isVietnamese) "Tâm trạng tệ" else "Bad", Color(0xFFFF8800)),
                        Triple("😫", if (isVietnamese) "Rất tệ" else "Awful", Color(0xFFCC0000))
                    )

                    moodList.forEach { (emoji, label, color) ->
                        val count = analytics.moodCounts[emoji] ?: 0
                        val percentage = analytics.moodPercentages[emoji] ?: 0f
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
                        ) {
                            val icon = moodIcons[emoji]
                            if (icon != null) {
                                Image(
                                    painter = painterResource(id = icon),
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp)
                                )
                            } else {
                                Text(text = emoji, fontSize = 20.sp)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = String.format("%.0f%% (%d)", percentage * 100, count),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                // Custom Progress Bar
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(fraction = percentage.coerceAtLeast(0.01f))
                                            .clip(CircleShape)
                                            .background(color)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section 2: Tag correlation Insights
        if (analytics.tagMoodCorrelations.isNotEmpty()) {
            item {
                Text(
                    text = if (isVietnamese) "Mối tương quan cảm xúc theo Nhãn" else "Tag-Mood Correlation",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                )
            }

            items(analytics.tagMoodCorrelations) { correlation ->
                val rating = correlation.averageMood
                val feedback = when {
                    rating >= 4.2f -> if (isVietnamese) "Rất tích cực! Nhãn này đem lại niềm vui lớn." else "Highly positive! This tag brings great joy."
                    rating >= 3.5f -> if (isVietnamese) "Tâm trạng tích cực khi gắn thẻ này." else "Positive mood when logging this tag."
                    rating >= 2.5f -> if (isVietnamese) "Cảm xúc trung tính, ổn định." else "Stable, neutral mood."
                    else -> if (isVietnamese) "Nhãn này thường gắn liền với sự trầm lắng/mệt mỏi." else "This tag is associated with reflective/low energy mood."
                }

                Surface(
                    color = if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.12f)
                    else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.03f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Tag Icon
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "#",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = correlation.tag,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (isVietnamese) "${correlation.entryCount} ghi chép" else "${correlation.entryCount} logs",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            // Rating indicators (stars)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val roundedStars = (rating + 0.5f).toInt()
                                (1..5).forEach { i ->
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = if (i <= roundedStars) Color(0xFFFFCC00) else Color.Gray.copy(alpha = 0.2f),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = String.format(java.util.Locale.US, "%.1f/5.0", rating),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = feedback,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}
