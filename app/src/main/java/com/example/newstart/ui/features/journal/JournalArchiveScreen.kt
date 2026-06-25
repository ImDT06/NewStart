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

    var expandedMovieGroup by remember { mutableStateOf<MovieGroup?>(null) }
    var expandedBookGroup by remember { mutableStateOf<BookGroup?>(null) }
    var expandedSubjectGroup by remember { mutableStateOf<SubjectGroup?>(null) }

    val context = LocalContext.current
    val isVietnamese = remember(context) {
        context.resources.configuration.locales[0].language == "vi"
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
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary,
                divider = {},
                indicator = { tabPositions ->
                    if (selectedTab < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                modifier = Modifier.padding(horizontal = 16.dp).height(48.dp)
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { viewModel.selectTab(0) },
                    text = {
                        Text(
                            text = if (isVietnamese) "Phim (${movies.size})" else "Movies (${movies.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { viewModel.selectTab(1) },
                    text = {
                        Text(
                            text = if (isVietnamese) "Sách (${books.size})" else "Books (${books.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { viewModel.selectTab(2) },
                    text = {
                        Text(
                            text = if (isVietnamese) "Môn học (${subjects.size})" else "Subjects (${subjects.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
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
                onDismiss = { expandedMovieGroup = null }
            )
        }

        expandedBookGroup?.let { group ->
            ExpandedGroupSheet(
                title = group.title,
                subtitle = if (isVietnamese) "Sách đọc" else "Book",
                rating = group.averageRating,
                entries = group.entries,
                isDark = isDark,
                onDismiss = { expandedBookGroup = null }
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
                onDismiss = { expandedSubjectGroup = null }
            )
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
    val cardColor = if (isDark) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
    } else {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = cardColor,
        border = BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f)),
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.9f)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Background preview image if available
            if (latestEntry?.imageUrl != null) {
                AsyncImage(
                    model = latestEntry.imageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    alpha = 0.25f
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header: Total logs
                Row(
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.35f), CircleShape)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (imageCount > 0) Icons.Default.PhotoLibrary else Icons.Default.Description,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "$totalCount ghi chép",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Bottom: Title & Rating
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
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
                                text = String.format("%.1f", rating),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
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
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = if (isDark) Color(0xFF121212) else MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
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
                    ArchiveEntryItem(entry, isDark, isSubject)
                }
            }
        }
    }
}

@Composable
private fun ArchiveEntryItem(
    entry: JournalEntry,
    isDark: Boolean,
    isSubject: Boolean
) {
    val context = LocalContext.current
    val locale = remember(context) { context.resources.configuration.locales[0] }
    val dateFormatter = remember { SimpleDateFormat("dd MMMM, yyyy HH:mm", locale) }
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
        else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.03f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Date & Mood
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                )

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

            Spacer(modifier = Modifier.height(8.dp))

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
                Spacer(modifier = Modifier.height(10.dp))
                AsyncImage(
                    model = entry.imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.2f)
                        .clip(RoundedCornerShape(14.dp)),
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
                Row(verticalAlignment = Alignment.CenterVertically) {
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
