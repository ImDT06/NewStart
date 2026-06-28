package com.example.newstart.ui.features.journal.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.newstart.R
import com.example.newstart.domain.model.JournalEntry
import com.example.newstart.domain.model.JournalType
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Star
import com.example.newstart.ui.theme.LocalDarkTheme

@Composable
fun TimelineEntryItem(
    entry: JournalEntry,
    timeFormatted: String,
    isLast: Boolean,
    onImageClick: (String) -> Unit,
    onOptionsClick: () -> Unit
) {
    val timelineColor = MaterialTheme.colorScheme.primary
    val isDark = LocalDarkTheme.current

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

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(end = 8.dp, bottom = 8.dp)
    ) {
        // Timeline Column
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(32.dp)
        ) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.TopCenter) {
                if (!isLast) {
                    Spacer(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(top = 28.dp)
                            .width(1.5.dp)
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        timelineColor.copy(alpha = 0.35f),
                                        timelineColor.copy(alpha = 0.1f)
                                    )
                                )
                            )
                    )
                }
                Surface(
                    modifier = Modifier
                        .size(26.dp)
                        .padding(top = 2.dp),
                    shape = CircleShape,
                    color = timelineColor.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, timelineColor.copy(alpha = 0.2f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (moodIcon != null) {
                            Image(
                                painter = painterResource(id = moodIcon),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        } else {
                            Text(text = entry.emoji, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Content Card
        Card(
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDark) MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp)
                else androidx.compose.ui.graphics.lerp(
                    start = MaterialTheme.colorScheme.background,
                    stop = MaterialTheme.colorScheme.primaryContainer,
                    fraction = 0.3f
                ),
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 4.dp else 2.dp),
            border = if (isDark) BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f)) else null
        ) {
            Column {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = timeFormatted,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                        )
                        IconButton(
                            onClick = onOptionsClick,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreHoriz,
                                contentDescription = "Options",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = entry.text,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 22.sp
                    )
                }

                // Hiển thị thông tin mở rộng (Phim, Sách, Môn học)
                if (entry.type != JournalType.NORMAL) {
                    MetadataSection(entry)
                }

                if (entry.imageUrl != null) {
                    AsyncImage(
                        model = entry.imageUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(18.dp))
                            .clickable { onImageClick(entry.imageUrl) },
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    }
}

@Composable
private fun MetadataSection(entry: JournalEntry) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            when (entry.type) {
                JournalType.MOVIE -> {
                    entry.movieDetails?.let { movie ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Movie, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text(movie.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        if (movie.rating > 0) {
                            Row(modifier = Modifier.padding(top = 4.dp)) {
                                (1..5).forEach { i ->
                                    Icon(
                                        Icons.Default.Star, null,
                                        tint = if (i <= movie.rating) Color(0xFFFFCC00) else Color.Gray.copy(alpha = 0.2f),
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                JournalType.BOOK -> {
                    entry.bookDetails?.let { book ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.MenuBook, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text(book.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        if (book.rating > 0) {
                            Row(modifier = Modifier.padding(top = 4.dp)) {
                                (1..5).forEach { i ->
                                    Icon(
                                        Icons.Default.Star, null,
                                        tint = if (i <= book.rating) Color(0xFFFFCC00) else Color.Gray.copy(alpha = 0.2f),
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                JournalType.SUBJECT -> {
                    entry.subjectDetails?.let { subject ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.School, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text(subject.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Row(modifier = Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("Mức độ hiểu: ", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            (1..5).forEach { i ->
                                Icon(
                                    Icons.Default.Star, null,
                                    tint = if (i <= subject.understandingLevel) Color(0xFF00C851) else Color.Gray.copy(alpha = 0.2f),
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }
                else -> {}
            }
        }
    }
}
