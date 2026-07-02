package com.example.newstart.ui.features.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.newstart.domain.model.JournalEntry
import com.example.newstart.domain.model.User
import com.example.newstart.ui.MainViewModel
import com.example.newstart.ui.features.habits.SocialFeedItem
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    mainViewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val allUsers by mainViewModel.allUsers.collectAsStateWithLifecycle()
    val blockedUsers by mainViewModel.blockedUsers.collectAsStateWithLifecycle()
    val blockedReasons by mainViewModel.blockedReasons.collectAsStateWithLifecycle()
    val socialFeed by mainViewModel.adminSocialFeed.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val locale = remember(context) { context.resources.configuration.locales[0] }
    val isVi = remember(locale) { locale.language == "vi" }
    val dateTimeFormatter = remember { SimpleDateFormat("HH:mm - dd/MM/yyyy", locale) }
    
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Người dùng, 1: Bài đăng
    val tabs = listOf(
        if (isVi) "Người dùng" else "Users",
        if (isVi) "Bài đăng" else "Posts"
    )

    var userFilter by remember { mutableIntStateOf(0) } // 0: Tất cả, 1: Đang hoạt động, 2: Bị khóa

    val filteredUsers = remember(allUsers, blockedUsers, userFilter) {
        when (userFilter) {
            1 -> allUsers.filter { !blockedUsers.contains(it.id) }
            2 -> allUsers.filter { blockedUsers.contains(it.id) }
            else -> allUsers
        }
    }

    var showConfirmDeletePostId by remember { mutableStateOf<String?>(null) }
    var showConfirmBlockUser by remember { mutableStateOf<Pair<User, Boolean>?>(null) } // User to (true = block, false = unblock)
    var blockReasonInput by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        text = if (isVi) "Quản trị hệ thống" else "Admin Dashboard",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Elegant Tab Selector
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { 
                            Text(
                                text = title, 
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 14.sp
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            when (selectedTab) {
                0 -> {
                    // Users Tab
                    Column(modifier = Modifier.fillMaxSize()) {
                        // User Filter Chips Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val filters = listOf(
                                if (isVi) "Tất cả" else "All",
                                if (isVi) "Đang hoạt động" else "Active",
                                if (isVi) "Bị khóa" else "Blocked"
                            )
                            filters.forEachIndexed { index, label ->
                                val isSelected = userFilter == index
                                Surface(
                                    onClick = { userFilter = index },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(
                                        width = 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                    ),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.padding(horizontal = 12.dp)
                                    ) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        if (filteredUsers.isEmpty()) {
                            val emptyTitle = when (userFilter) {
                                1 -> if (isVi) "Không có người dùng hoạt động" else "No active users"
                                2 -> if (isVi) "Không có người dùng bị khóa" else "No blocked users"
                                else -> if (isVi) "Không có người dùng nào" else "No users found"
                            }
                            val emptySubtitle = when (userFilter) {
                                1 -> if (isVi) "Hiện tại tất cả người dùng đều đã bị khóa." else "All users are currently blocked."
                                2 -> if (isVi) "Hệ thống hiện tại không có người dùng nào bị khóa." else "No users are currently blocked."
                                else -> if (isVi) "Hệ thống chưa ghi nhận người dùng nào." else "No users registered yet."
                            }
                            EmptyStateView(
                                icon = if (userFilter == 2) Icons.Default.Block else Icons.Default.Group,
                                title = emptyTitle,
                                subtitle = emptySubtitle
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(filteredUsers, key = { it.id }) { user ->
                                    val isBlocked = blockedUsers.contains(user.id)
                                    val reason = blockedReasons[user.id]
                                    UserAdminItem(
                                        user = user,
                                        isBlocked = isBlocked,
                                        isVi = isVi,
                                        blockedReason = reason,
                                        onBlockClick = { 
                                            showConfirmBlockUser = user to !isBlocked
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                1 -> {
                    // Posts Tab
                    if (socialFeed.isEmpty()) {
                        EmptyStateView(
                            icon = Icons.Default.Forum,
                            title = if (isVi) "Không có bài viết nào" else "No posts found",
                            subtitle = if (isVi) "Bảng tin cộng đồng hiện đang trống." else "The community feed is currently empty."
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(socialFeed, key = { it.id }) { entry ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = if (isVi) "Bài viết ID: ${entry.id.take(8)}..." else "Post ID: ${entry.id.take(8)}...",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                            )
                                            IconButton(
                                                onClick = { showConfirmDeletePostId = entry.id },
                                                colors = IconButtonDefaults.iconButtonColors(
                                                    contentColor = MaterialTheme.colorScheme.error
                                                ),
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Delete Post",
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                        SocialFeedItem(
                                            entry = entry,
                                            timeFormatted = remember(entry.timestamp) {
                                                entry.timestamp?.let { dateTimeFormatter.format(it) } ?: "--:--"
                                            },
                                            onImageClick = { _ -> },
                                            getUserFlow = { mainViewModel.getUserById(it) },
                                            onReactToPost = { _, _ -> },
                                            onReplyToPost = {}
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

    // Confirmation Dialogs
    if (showConfirmDeletePostId != null) {
        AlertDialog(
            onDismissRequest = { showConfirmDeletePostId = null },
            title = { Text(if (isVi) "Xác nhận xóa" else "Confirm Delete") },
            text = { Text(if (isVi) "Bạn có chắc chắn muốn xóa bài viết này khỏi hệ thống không? Hành động này không thể hoàn tác." else "Are you sure you want to delete this post? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDeletePostId?.let { mainViewModel.adminDeletePost(it) }
                        showConfirmDeletePostId = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(if (isVi) "Xóa" else "Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDeletePostId = null }) {
                    Text(if (isVi) "Hủy" else "Cancel")
                }
            }
        )
    }

    if (showConfirmBlockUser != null) {
        val (user, block) = showConfirmBlockUser!!
        AlertDialog(
            onDismissRequest = { 
                showConfirmBlockUser = null
                blockReasonInput = ""
            },
            title = { 
                Text(
                    if (isVi) {
                        if (block) "Khóa tài khoản" else "Mở khóa tài khoản"
                    } else {
                        if (block) "Block User" else "Unblock User"
                    }
                ) 
            },
            text = { 
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        if (isVi) {
                            if (block) "Bạn có chắc chắn muốn khóa tài khoản ${user.name} (${user.email})? Họ sẽ không thể đăng tin lên cộng đồng."
                            else "Bạn có muốn mở khóa tài khoản cho ${user.name}?"
                        } else {
                            if (block) "Are you sure you want to block user ${user.name} (${user.email})?"
                            else "Do you want to unblock user ${user.name}?"
                        }
                    ) 
                    if (block) {
                        OutlinedTextField(
                            value = blockReasonInput,
                            onValueChange = { blockReasonInput = it },
                            label = { Text(if (isVi) "Lý do khóa" else "Reason for blocking") },
                            placeholder = { Text(if (isVi) "Nhập lý do khóa tài khoản…" else "Enter reason…") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 3
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        mainViewModel.blockUser(user.id, block, if (block) blockReasonInput else "")
                        showConfirmBlockUser = null
                        blockReasonInput = ""
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (block) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    ),
                    enabled = !block || blockReasonInput.isNotBlank()
                ) {
                    Text(if (isVi) "Xác nhận" else "Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showConfirmBlockUser = null
                    blockReasonInput = ""
                }) {
                    Text(if (isVi) {
                        "Hủy"
                    } else {
                        "Cancel"
                    })
                }
            }
        )
    }
}

@Composable
fun UserAdminItem(
    user: User,
    isBlocked: Boolean,
    isVi: Boolean,
    blockedReason: String? = null,
    onBlockClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if (isBlocked) Color.Gray.copy(alpha = 0.15f)
                            else Color(0xFFD32F2F).copy(alpha = 0.15f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (!user.avatarUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = user.avatarUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = user.name.take(1).uppercase(Locale.ROOT),
                            fontWeight = FontWeight.Bold,
                            color = if (isBlocked) Color.Gray else Color(0xFFD32F2F),
                            fontSize = 18.sp
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column {
                    Text(
                        text = user.name,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = user.email,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                    if (isBlocked) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Column(
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.errorContainer,
                                modifier = Modifier.wrapContentSize()
                            ) {
                                Text(
                                    text = if (isVi) "Đã khóa" else "Blocked",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (!blockedReason.isNullOrBlank()) {
                                Text(
                                    text = if (isVi) "Lý do: $blockedReason" else "Reason: $blockedReason",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }

            IconButton(
                onClick = onBlockClick,
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = if (isBlocked) Color(0xFF4CAF50) else Color(0xFFD32F2F)
                )
            ) {
                Icon(
                    imageVector = if (isBlocked) Icons.Default.LockOpen else Icons.Default.Block,
                    contentDescription = if (isBlocked) "Unblock" else "Block"
                )
            }
        }
    }
}

@Composable
fun EmptyStateView(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
