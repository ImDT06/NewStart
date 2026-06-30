package com.example.newstart.ui.features.habits

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.newstart.R
import com.example.newstart.domain.model.JournalEntry
import com.example.newstart.domain.model.JournalType
import com.example.newstart.domain.model.Squad
import com.example.newstart.domain.model.User
import com.example.newstart.ui.MainViewModel
import com.example.newstart.ui.components.RatingBar
import com.example.newstart.ui.navigation.Screen
import com.example.newstart.ui.features.journal.JournalViewModel
import com.example.newstart.ui.features.social.SocialViewModel
import com.example.newstart.ui.theme.LocalDarkTheme
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitsScreen(
    mainViewModel: MainViewModel,
    navController: androidx.navigation.NavController,
    modifier: Modifier = Modifier,
    socialViewModel: SocialViewModel = hiltViewModel(),
    journalViewModel: JournalViewModel = hiltViewModel()
) {
    val socialFeed by journalViewModel.socialFeed.collectAsStateWithLifecycle()
    val searchQuery by socialViewModel.searchQuery.collectAsStateWithLifecycle()
    val searchResults by socialViewModel.searchResults.collectAsStateWithLifecycle()
    val incomingRequests by socialViewModel.incomingRequests.collectAsStateWithLifecycle()
    val squads by socialViewModel.squads.collectAsStateWithLifecycle()
    val isSearching by socialViewModel.isSearching.collectAsStateWithLifecycle()
    val friends by socialViewModel.friends.collectAsStateWithLifecycle()
    val sentRequests by socialViewModel.sentRequests.collectAsStateWithLifecycle()
    val isRefreshingFeed by journalViewModel.isRefreshingFeed.collectAsStateWithLifecycle()
    val currentUserId = socialViewModel.currentUserId
    
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Bảng tin, 1: Bạn bè, 2: Nhóm
    var activeSquadDetail by remember { mutableStateOf<Squad?>(null) }
    val activeSquad = squads.find { it.id == activeSquadDetail?.id } ?: activeSquadDetail
    var activeReplyJournal by remember { mutableStateOf<JournalEntry?>(null) }

    val context = LocalContext.current
    val isDark = LocalDarkTheme.current
    
    LaunchedEffect(Unit) {
        journalViewModel.refreshSocialFeed()
    }

    val locale = remember(context) { context.resources.configuration.locales[0] }
    val isVietnamese = locale.language == "vi"
    val dateTimeFormatter = remember { SimpleDateFormat("HH:mm - dd/MM/yyyy", locale) }

    DisposableEffect(activeSquad) {
        if (activeSquad != null) {
            mainViewModel.setBottomBarVisible(false)
        } else {
            mainViewModel.setBottomBarVisible(true)
        }
        onDispose {
            mainViewModel.setBottomBarVisible(true)
        }
    }

    if (activeSquad != null) {
        com.example.newstart.ui.features.social.SquadDetailViewWrapper(
            squad = activeSquad,
            currentUserId = currentUserId ?: "",
            friends = friends,
            viewModel = socialViewModel,
            mainViewModel = mainViewModel,
            onBack = { activeSquadDetail = null },
            hasBottomBar = false
        )
    } else {
        val backgroundColor = MaterialTheme.colorScheme.background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = remember(isDark, backgroundColor) {
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = if (isDark) listOf(Color.Black, Color.Black)
                            else listOf(backgroundColor, backgroundColor)
                        )
                    }
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                    // Left-aligned header title synchronizing with JournalScreen
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = if (isVietnamese) "Cộng đồng" else "Community",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    // Modern Segmented Control Style Tabs with smooth sliding animation
                    val tabs = listOf(
                        if (isVietnamese) "Bảng tin" else "Feed",
                        stringResource(R.string.social_tab_friends),
                        stringResource(R.string.social_tab_squads)
                    )

                    BoxWithConstraints(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .fillMaxWidth()
                            .height(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isDark) Color(0xFF1E1E1E) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        val totalWidth = maxWidth
                        val tabWidth = totalWidth / tabs.size
                        
                        // Animate indicator offset
                        val indicatorOffset by androidx.compose.animation.core.animateDpAsState(
                            targetValue = tabWidth * selectedTab,
                            animationSpec = androidx.compose.animation.core.spring(
                                dampingRatio = androidx.compose.animation.core.Spring.DampingRatioLowBouncy,
                                stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
                            ),
                            label = "TabIndicatorOffset"
                        )

                        // Sliding indicator background
                        Box(
                            modifier = Modifier
                                .padding(3.dp)
                                .offset(x = indicatorOffset)
                                .width(tabWidth - 6.dp)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(9.dp))
                                .background(MaterialTheme.colorScheme.primary)
                        )

                        // Row of clickable tabs
                        Row(
                            modifier = Modifier.fillMaxSize().padding(3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            tabs.forEachIndexed { index, title ->
                                val isSelected = selectedTab == index
                                // Animate text color
                                val textColor by androidx.compose.animation.animateColorAsState(
                                    targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary 
                                                  else MaterialTheme.colorScheme.onSurfaceVariant,
                                    animationSpec = androidx.compose.animation.core.tween(durationMillis = 200),
                                    label = "TabTextColor"
                                )
                                
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(9.dp))
                                        .clickable(
                                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                            indication = null,
                                            onClick = { 
                                                selectedTab = index 
                                                if (index == 0) {
                                                    journalViewModel.refreshSocialFeed()
                                                }
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = textColor
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Content Area with more breathing room
                    Box(modifier = Modifier.fillMaxSize()) {
                        when (selectedTab) {
                            0 -> SocialFeedList(
                                socialFeed = socialFeed,
                                isRefreshing = isRefreshingFeed,
                                onRefresh = { journalViewModel.refreshSocialFeed() },
                                isVietnamese = isVietnamese,
                                isDark = isDark,
                                dateTimeFormatter = dateTimeFormatter,
                                onImageClick = { /* Full screen photo logic */ },
                                getUserFlow = { journalViewModel.getUserById(it) },
                                onReactToPost = { postId, emoji -> journalViewModel.reactToPost(postId, emoji) },
                                onReplyToPost = { entry ->
                                    activeReplyJournal = entry
                                }
                            )
                            1 -> Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                com.example.newstart.ui.features.social.FriendsTabWrapper(
                                    searchQuery = searchQuery,
                                    onSearchQueryChange = { socialViewModel.onSearchQueryChange(it) },
                                    isSearching = isSearching,
                                    searchResults = searchResults,
                                    incomingRequests = incomingRequests,
                                    sentRequests = sentRequests,
                                    friends = friends,
                                    currentUserId = currentUserId ?: "",
                                    onSendRequest = { socialViewModel.sendRequest(it) },
                                    onAcceptRequest = { socialViewModel.acceptRequest(it) },
                                    onDeclineRequest = { socialViewModel.declineRequest(it) },
                                    onRemoveFriend = { socialViewModel.removeFriend(it) },
                                    getUserFlow = { socialViewModel.getUserById(it) },
                                    onFriendChatClick = { friendship ->
                                        val friendId = friendship.userIds.firstOrNull { it != (currentUserId ?: "") }
                                        if (friendId != null) {
                                            mainViewModel.pendingChatUserId = friendId
                                            navController.navigate(Screen.Social.route)
                                        }
                                    }
                                )
                            }
                            2 -> Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                com.example.newstart.ui.features.social.SquadsTabWrapper(
                                    squads = squads,
                                    friends = friends,
                                    currentUserId = currentUserId ?: "",
                                    getUserFlow = { id -> socialViewModel.getUserById(id) },
                                    onCreateSquad = { n, d, m -> socialViewModel.createSquad(n, d, m) },
                                    onUpdateSquad = { id, n, d -> socialViewModel.updateSquad(id, n, d) },
                                    onSquadClick = { activeSquadDetail = it }
                                )
                            }
                        }
                    }
                }
            }

    if (activeReplyJournal != null) {
        val replyJournal = activeReplyJournal!!
        var replyText by remember { mutableStateOf("") }
        val authorState by remember(replyJournal.userId) {
            journalViewModel.getUserById(replyJournal.userId)
        }.collectAsState(initial = User(name = "Đang tải..."))
        
        ModalBottomSheet(
            onDismissRequest = { activeReplyJournal = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = if (isDark) Color(0xFF161618) else MaterialTheme.colorScheme.surface,
            dragHandle = { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 36.dp)
            ) {
                Text(
                    text = "Phản hồi bài viết",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                // Preview of the shared journal
                Surface(
                    color = if (isDark) Color(0xFF252528) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = replyJournal.emoji,
                            fontSize = 24.sp,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Bài viết của ${authorState.name.ifBlank { "Người dùng" }}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (replyJournal.text.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = replyJournal.text,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
                
                // Message Input field
                OutlinedTextField(
                    value = replyText,
                    onValueChange = { replyText = it },
                    placeholder = { Text("Viết bình luận phản hồi gửi vào tin nhắn...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    maxLines = 4,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedContainerColor = if (isDark) Color(0xFF1E1E22) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                        unfocusedContainerColor = if (isDark) Color(0xFF1E1E22) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color.Transparent
                    )
                )
                
                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = { activeReplyJournal = null },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Hủy", fontWeight = FontWeight.Medium)
                    }
                    
                    Button(
                        onClick = {
                            val friendship = friends.find { it.userIds.contains(replyJournal.userId) }
                            if (friendship != null) {
                                mainViewModel.sendDirectMessage(
                                    friendshipId = friendship.id,
                                    text = replyText.trim(),
                                    sharedJournal = replyJournal
                                )
                                android.widget.Toast.makeText(context, "Đã gửi phản hồi vào tin nhắn trực tiếp!", android.widget.Toast.LENGTH_SHORT).show()
                                activeReplyJournal = null
                            } else {
                                android.widget.Toast.makeText(context, "Bạn chỉ có thể phản hồi bài viết của bạn bè!", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = true,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Send", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Gửi phản hồi")
                    }
                }
            }
        }
    }
}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SocialFeedList(
    socialFeed: List<JournalEntry>,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    isVietnamese: Boolean,
    isDark: Boolean,
    dateTimeFormatter: SimpleDateFormat,
    onImageClick: (String) -> Unit,
    getUserFlow: (String) -> Flow<User>,
    onReactToPost: (String, String) -> Unit,
    onReplyToPost: (JournalEntry) -> Unit
) {
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize()
    ) {
        if (socialFeed.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.Diversity3, 
                    null, 
                    modifier = Modifier.size(90.dp), 
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                )
                Spacer(Modifier.height(32.dp))
                Text(
                    if (isVietnamese) "Bảng tin cộng đồng" else "Community Feed",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    if (isVietnamese) "Kết nối với bạn bè để cùng nhau kỷ luật" else "Connect with friends to stay disciplined together",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 8.dp, bottom = 100.dp, start = 16.dp, end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(items = socialFeed, key = { it.id }) { entry ->
                    SocialFeedItem(
                        entry = entry,
                        timeFormatted = remember(entry.timestamp) { entry.timestamp?.let { dateTimeFormatter.format(it) } ?: "--:--" },
                        onImageClick = onImageClick,
                        getUserFlow = getUserFlow,
                        onReactToPost = onReactToPost,
                        onReplyToPost = onReplyToPost
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SocialFeedItem(
    entry: JournalEntry,
    timeFormatted: String,
    onImageClick: (String) -> Unit,
    getUserFlow: (String) -> Flow<User>,
    onReactToPost: (String, String) -> Unit,
    onReplyToPost: (JournalEntry) -> Unit
) {
    val isDark = LocalDarkTheme.current
    val currentUserId = remember {
        com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""
    }
    
    val userState by remember(entry.userId) {
        getUserFlow(entry.userId)
    }.collectAsState(initial = User(name = "Đang tải..."))

    val displayName = userState.name.ifBlank { "Người dùng" }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF161618) 
                             else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (isDark) Color(0xFF252528) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 1.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        modifier = Modifier.size(46.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (!userState.avatarUrl.isNullOrEmpty()) {
                                AsyncImage(
                                    model = userState.avatarUrl,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                                    contentScale = ContentScale.Crop
                               )
                            } else {
                                Icon(Icons.Default.Person, null, modifier = Modifier.size(26.dp), tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = displayName,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = timeFormatted,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
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
                
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(
                            color = if (isDark) Color(0xFF252528) else MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (moodIcon != null) {
                        androidx.compose.foundation.Image(
                            painter = painterResource(id = moodIcon),
                            contentDescription = null,
                            modifier = Modifier.size(28.dp)
                        )
                    } else {
                        Text(entry.emoji, fontSize = 20.sp)
                    }
                }
            }
            
            if (entry.text.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = entry.text,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        lineHeight = 24.sp,
                        letterSpacing = 0.2.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (entry.type != JournalType.NORMAL) {
                val topPadding = if (entry.text.isEmpty()) 4.dp else 12.dp
                Spacer(modifier = Modifier.height(topPadding))
                MetadataSection(entry)
            }
            
            entry.imageUrl?.let { url ->
                val topPadding = if (entry.text.isEmpty() && entry.type == JournalType.NORMAL) 4.dp else 16.dp
                Spacer(modifier = Modifier.height(topPadding))
                AsyncImage(
                    model = url,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.2f)
                        .clip(RoundedCornerShape(22.dp))
                        .clickable { onImageClick(url) }
                        .border(
                            width = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(22.dp)
                        ),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
            
            // Reactions and Comments Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    val commonEmojis = listOf("❤️", "👍", "🔥", "😂", "👏")
                    commonEmojis.forEach { emoji ->
                        val count = entry.reactions.values.count { it == emoji }
                        val hasReacted = entry.reactions[currentUserId] == emoji
                        
                        val scale by androidx.compose.animation.core.animateFloatAsState(
                            targetValue = if (hasReacted) 1.08f else 1f,
                            animationSpec = androidx.compose.animation.core.spring(
                                dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                                stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
                            ),
                            label = "ReactionScale"
                        )

                        val buttonColor = if (hasReacted) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            if (isDark) Color(0xFF252528) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        }
                        
                        val contentColor = if (hasReacted) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        
                        Surface(
                            onClick = { onReactToPost(entry.id, emoji) },
                            shape = RoundedCornerShape(18.dp),
                            color = buttonColor,
                            border = if (hasReacted) null else BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                            modifier = Modifier
                                .height(36.dp)
                                .scale(scale)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(emoji, fontSize = 16.sp)
                                if (count > 0) {
                                    Text(
                                        text = count.toString(),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = contentColor,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Reply/Chat button on the right side of the row!
                Surface(
                    onClick = { onReplyToPost(entry) },
                    shape = RoundedCornerShape(18.dp),
                    color = if (isDark) Color(0xFF252528) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                    modifier = Modifier.height(36.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Chat,
                            contentDescription = "Phản hồi",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Phản hồi",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (entry.reactions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                var showReactionSheet by remember { mutableStateOf(false) }
                val context = LocalContext.current
                val locale = remember(context) { context.resources.configuration.locales[0] }
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null,
                            onClick = { showReactionSheet = true }
                        )
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.height(24.dp)) {
                        val userIds = entry.reactions.keys.take(3).toList()
                        userIds.forEachIndexed { index, rUserId ->
                            val userState by remember(rUserId) { getUserFlow(rUserId) }.collectAsState(initial = null)
                            val avatarUrl = userState?.avatarUrl
                            
                            Surface(
                                modifier = Modifier
                                    .padding(start = (index * 16).dp)
                                    .size(24.dp)
                                    .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            ) {
                                if (!avatarUrl.isNullOrEmpty()) {
                                    AsyncImage(
                                        model = avatarUrl,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.Person, 
                                        null, 
                                        modifier = Modifier.padding(4.dp), 
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Text(
                        text = if (locale.language == "vi") {
                            "${entry.reactions.size} người đã bày tỏ cảm xúc"
                        } else {
                            "${entry.reactions.size} people reacted"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Medium
                    )
                }

                if (showReactionSheet) {
                    ModalBottomSheet(
                        onDismissRequest = { showReactionSheet = false },
                        sheetState = rememberModalBottomSheetState(),
                        containerColor = if (isDark) Color(0xFF161618) else MaterialTheme.colorScheme.surface,
                        dragHandle = { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)) }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp)
                                .padding(bottom = 36.dp)
                        ) {
                            Text(
                                text = if (locale.language == "vi") "Người đã bày tỏ cảm xúc" else "Reactions",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyColumn(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(entry.reactions.toList()) { (rUserId, emoji) ->
                                    ReactedUserRow(
                                        userId = rUserId,
                                        emoji = emoji,
                                        getUserFlow = getUserFlow
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
private fun MetadataSection(entry: JournalEntry) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            when (entry.type) {
                JournalType.MOVIE -> {
                    entry.movieDetails?.let { movie ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Movie, 
                                null, 
                                modifier = Modifier.size(18.dp), 
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = movie.title, 
                                style = MaterialTheme.typography.titleSmall, 
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (movie.rating > 0) {
                            RatingBar(
                                rating = movie.rating,
                                starSize = 14.dp,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                    }
                }
                JournalType.BOOK -> {
                    entry.bookDetails?.let { book ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.MenuBook, 
                                null, 
                                modifier = Modifier.size(18.dp), 
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = book.title, 
                                style = MaterialTheme.typography.titleSmall, 
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (book.rating > 0) {
                            RatingBar(
                                rating = book.rating,
                                starSize = 14.dp,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                    }
                }
                JournalType.SUBJECT -> {
                    entry.subjectDetails?.let { subject ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.School, 
                                null, 
                                modifier = Modifier.size(18.dp), 
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = subject.name, 
                                style = MaterialTheme.typography.titleSmall, 
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Row(modifier = Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Mức độ hiểu: ", 
                                style = MaterialTheme.typography.bodySmall, 
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(4.dp))
                            (1..5).forEach { i ->
                                Icon(
                                    Icons.Default.Star, null,
                                    tint = if (i <= subject.understandingLevel) Color(0xFF00C851) else Color.Gray.copy(alpha = 0.2f),
                                    modifier = Modifier.size(14.dp)
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

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 6.dp)
    )
}

@Composable
private fun ReactedUserRow(
    userId: String,
    emoji: String,
    getUserFlow: (String) -> Flow<User>
) {
    val userState by remember(userId) { getUserFlow(userId) }.collectAsState(initial = User(name = "Đang tải..."))
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            ) {
                if (!userState.avatarUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = userState.avatarUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = userState.name.ifBlank { "Người dùng" },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Text(
            text = emoji,
            fontSize = 20.sp,
            modifier = Modifier.padding(end = 8.dp)
        )
    }
}
