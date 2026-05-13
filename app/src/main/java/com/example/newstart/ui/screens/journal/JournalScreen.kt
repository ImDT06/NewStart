package com.example.newstart.ui.screens.journal

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.newstart.R
import com.example.newstart.ui.theme.NewStartTheme
import com.example.newstart.ui.util.LanguagePreviews

private val JournalBlue = Color(0xFF1D5FE2)
private val SkyBlue = Color(0xFFE0F2FE)
private val DeepSkyBlue = Color(0xFF7DD3FC)

data class JournalEntry(
    val time: String,
    val moodEmoji: String,
    val content: String,
    val imageUrl: String? = null,
    val color: Color = JournalBlue
)

@Composable
fun JournalScreen(
    modifier: Modifier = Modifier
) {
    val entries = listOf(
        JournalEntry("09:17", "😊", "Có việc để làm rồi", "https://example.com/img1.jpg"),
        JournalEntry("08:05", "😴", "Chẳng biết nên làm gì"),
        JournalEntry("07:59", "😫", "Đã đến chỗ làm"),
        JournalEntry("07:44", "🚵", "Đi làm thôi", "https://example.com/img2.jpg"),
        JournalEntry("07:12", "😴", "Buổi đầu tiên đi làm sau kì nghỉ, không muốn dậy tí nào"),
        JournalEntry("00:21", "😴", "Đi ngủ thôi")
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = if (isSystemInDarkTheme()) {
                        listOf(Color(0xFF0F172A), Color(0xFF1E293B))
                    } else {
                        listOf(DeepSkyBlue, SkyBlue, Color.White)
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
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.journal_greeting),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isSystemInDarkTheme()) Color.White else Color(0xFF0036D6)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(id = R.string.journal_subtitle),
                    fontSize = 15.sp,
                    color = (if (isSystemInDarkTheme()) Color.White else Color(0xFF1D5FE2)).copy(alpha = 0.8f),
                    lineHeight = 22.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Journal Card
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)),
                color = MaterialTheme.colorScheme.surface
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(24.dp)
                ) {
                    item {
                        // Date Header
                        Surface(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.padding(bottom = 24.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "04 Tháng 5, 2026",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    itemsIndexed(entries) { index, entry ->
                        TimelineEntryItem(
                            entry = entry,
                            isLast = index == entries.size - 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TimelineEntryItem(
    entry: JournalEntry,
    isLast: Boolean
) {
    val timelineColor = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.primary else JournalBlue

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min) // Important for the line height
    ) {
        // Timeline Column
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(48.dp)
        ) {
            Text(
                text = entry.time,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = timelineColor
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.TopCenter
            ) {
                if (!isLast) {
                    Canvas(modifier = Modifier.fillMaxHeight()) {
                        drawLine(
                            color = timelineColor.copy(alpha = 0.3f),
                            start = Offset(size.width / 2, 0f),
                            end = Offset(size.width / 2, size.height),
                            strokeWidth = 2.dp.toPx()
                        )
                    }
                }
                
                Surface(
                    modifier = Modifier.size(10.dp),
                    shape = CircleShape,
                    color = timelineColor
                ) {}
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Content Column
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 32.dp),
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = entry.moodEmoji,
                fontSize = 24.sp,
                modifier = Modifier.padding(top = 12.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.content,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }

            if (entry.imageUrl != null) {
                Spacer(modifier = Modifier.width(12.dp))
                // Placeholder for Image
                Box(
                    modifier = Modifier
                        .size(width = 120.dp, height = 80.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                ) {
                    // Actual Image would go here
                }
            }
        }
    }
}

@LanguagePreviews
@Composable
fun JournalScreenPreview() {
    NewStartTheme {
        JournalScreen()
    }
}
