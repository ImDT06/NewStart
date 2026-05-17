package com.example.newstart.ui.screens.journal

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalScreen(
    modifier: Modifier = Modifier,
    viewModel: JournalViewModel = hiltViewModel()
) {
    val entries by viewModel.entries.collectAsState()
    
    JournalContent(
        modifier = modifier,
        entries = entries,
        onDeleteEntry = { viewModel.deleteEntry(it) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalContent(
    modifier: Modifier = Modifier,
    entries: List<JournalEntry>,
    onDeleteEntry: (String) -> Unit
) {
    var selectedImageUrl by remember { mutableStateOf<String?>(null) }
    var entryToDelete by remember { mutableStateOf<JournalEntry?>(null) }
    
    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val dateFormatter = remember { SimpleDateFormat("dd MMMM, yyyy", Locale("vi")) }

    val isDark = isSystemInDarkTheme()
    
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
            // Header Section - More Compact & Modern
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
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
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isDark) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = dateFormatter.format(Date()),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Medium
                    )
                }
                
                // Optional: Profile or Settings mini icon
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

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
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp, start = 24.dp, end = 24.dp)
                    ) {
                        // Removed Date Header Item from list as it's now in the header

                        items(
                            items = entries,
                            key = { it.id }
                        ) { entry ->
                            val dismissState = rememberSwipeToDismissBoxState(
                                confirmValueChange = {
                                    if (it == SwipeToDismissBoxValue.EndToStart) {
                                        entryToDelete = entry
                                        false // Trả về false để mục không biến mất ngay lập tức
                                    } else false
                                }
                            )

                            // Tự động Reset trạng thái swipe khi đóng Dialog mà không xóa
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
                        modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                        contentScale = ContentScale.Fit
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

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .height(IntrinsicSize.Min) 
    ) {
        // Timeline Column
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

        // Content Card
        val isDark = isSystemInDarkTheme()
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
                color = if (isDark) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.1f) 
                        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
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
            onDeleteEntry = {}
        )
    }
}
