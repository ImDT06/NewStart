package com.example.newstart.ui.features.community

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.newstart.ui.features.journal.JournalViewModel
import com.example.newstart.ui.features.habits.SocialFeedItem
import com.example.newstart.ui.theme.LocalDarkTheme
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityScreen(
    viewModel: JournalViewModel = hiltViewModel()
) {
    val socialFeed by viewModel.socialFeed.collectAsStateWithLifecycle()
    val isDark = LocalDarkTheme.current
    val context = LocalContext.current
    val locale = remember(context) { context.resources.configuration.locales[0] }
    val isVietnamese = locale.language == "vi"
    val dateTimeFormatter = remember { SimpleDateFormat("HH:mm - dd/MM/yyyy", locale) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Cộng đồng", fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(top = 4.dp),
            color = Color.Transparent
        ) {
            if (socialFeed.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.Group,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = if (isVietnamese) "Bảng tin cộng đồng sắp ra mắt!" else "Community feed coming soon!",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = if (isVietnamese) "Kết nối với bạn bè để cùng nhau kỷ luật" else "Connect with friends to stay disciplined together",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = 8.dp,
                        bottom = 100.dp,
                        start = 12.dp,
                        end = 12.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(items = socialFeed, key = { it.id }) { entry ->
                        SocialFeedItem(
                            entry = entry,
                            timeFormatted = remember(entry.timestamp) {
                                entry.timestamp?.let { dateTimeFormatter.format(it) } ?: "--:--"
                            },
                            onImageClick = { /* full screen image nếu cần */ },
                            getUserFlow = { id -> viewModel.getUserById(id) },
                            onReactToPost = { postId, emoji -> viewModel.reactToPost(postId, emoji) },
                            onReplyToPost = {}
                        )
                    }
                }
            }
        }
    }
}
