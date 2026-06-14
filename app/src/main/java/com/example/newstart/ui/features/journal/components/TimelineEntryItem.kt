package com.example.newstart.ui.features.journal.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.newstart.domain.model.JournalEntry
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

    Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        // Timeline Column
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(40.dp)) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.TopCenter) {
                if (!isLast) {
                    Spacer(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(top = 28.dp)
                            .width(1.5.dp)
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(timelineColor.copy(alpha = 0.25f), timelineColor.copy(alpha = 0.05f))
                                )
                            )
                    )
                }
                Surface(
                    modifier = Modifier.size(30.dp).padding(top = 2.dp),
                    shape = CircleShape,
                    color = timelineColor.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, timelineColor.copy(alpha = 0.2f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = entry.emoji, fontSize = 16.sp)
                    }
                }
            }
        }

        // Content Card
        Card(
            modifier = Modifier.weight(1f).padding(start = 2.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDark) MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp) 
                                 else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 2.dp else 0.dp),
            border = if (isDark) BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f)) else null
        ) {
            Column {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
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
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = entry.text, 
                        style = MaterialTheme.typography.bodyLarge, 
                        color = MaterialTheme.colorScheme.onSurface, 
                        lineHeight = 22.sp
                    )
                }

                if (entry.imageUrl != null) {
                    AsyncImage(
                        model = entry.imageUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
                            .heightIn(min = 160.dp, max = 300.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onImageClick(entry.imageUrl) },
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    }
}
