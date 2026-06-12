package com.example.newstart.ui.features.social

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.newstart.domain.model.Squad
import com.example.newstart.domain.model.User

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SocialScreen(
    onNavigateBack: () -> Unit,
    viewModel: SocialViewModel = hiltViewModel()
) {
    var searchQuery by remember { mutableStateOf("") }
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val incomingRequests by viewModel.incomingRequests.collectAsStateWithLifecycle()
    val squads by viewModel.squads.collectAsStateWithLifecycle()
    val isSearching by viewModel.isSearching.collectAsStateWithLifecycle()
    
    var selectedTab by remember { mutableStateOf(0) } // 0: Bạn bè, 1: Squads

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Cộng đồng", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                modifier = Modifier.height(44.dp)
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Bạn bè", fontWeight = FontWeight.Bold, fontSize = 14.sp) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Nhóm (Squads)", fontWeight = FontWeight.Bold, fontSize = 14.sp) }
                )
            }

            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                if (selectedTab == 0) {
                    FriendsTab(
                        searchQuery = searchQuery,
                        onSearchQueryChange = { 
                            searchQuery = it
                            viewModel.searchUsers(it)
                        },
                        isSearching = isSearching,
                        searchResults = searchResults,
                        incomingRequests = incomingRequests,
                        onSendRequest = { viewModel.sendRequest(it) },
                        onAcceptRequest = { viewModel.acceptRequest(it) }
                    )
                } else {
                    SquadsTab(squads = squads)
                }
            }
        }
    }
}

@Composable
private fun FriendsTab(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    isSearching: Boolean,
    searchResults: List<User>,
    incomingRequests: List<com.example.newstart.domain.model.FriendRequest>,
    onSendRequest: (String) -> Unit,
    onAcceptRequest: (String) -> Unit
) {
    Column {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Tìm kiếm tên hoặc email", fontSize = 14.sp) },
            leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(20.dp)) },
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (isSearching) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(2.dp))
            Spacer(modifier = Modifier.height(12.dp))
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 20.dp)
        ) {
            if (searchQuery.isNotEmpty()) {
                item { SectionTitle("Kết quả tìm kiếm") }
                items(searchResults) { user ->
                    UserItem(
                        user = user,
                        action = {
                            Button(onClick = { onSendRequest(user.id) }) {
                                Text("Kết bạn")
                            }
                        }
                    )
                }
            } else {
                if (incomingRequests.isNotEmpty()) {
                    item { SectionTitle("Lời mời kết bạn") }
                    items(incomingRequests) { request ->
                        RequestItem(
                            fromUserId = request.fromUserId,
                            onAccept = { onAcceptRequest(request.id) }
                        )
                    }
                }

                item { SectionTitle("Danh sách bạn bè") }
                // Empty state or list
                if (incomingRequests.isEmpty() && searchQuery.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("Chưa có bạn bè. Hãy tìm kiếm để kết nối!", color = Color.Gray, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SquadsTab(squads: List<Squad>) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionTitle("Squads của bạn")
            TextButton(onClick = { /* TODO: Create Squad */ }) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Tạo mới", fontSize = 13.sp)
            }
        }

        if (squads.isEmpty()) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Diversity3, null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
                    Spacer(Modifier.height(8.dp))
                    Text("Bạn chưa tham gia nhóm nào", color = Color.Gray)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 20.dp)
            ) {
                items(squads) { squad ->
                    SquadItem(squad = squad)
                }
            }
        }
    }
}

@Composable
fun UserItem(user: User, action: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = user.avatarUrl,
            contentDescription = null,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(user.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(user.email, fontSize = 11.sp, color = Color.Gray)
        }
        Box(modifier = Modifier.scale(0.85f)) {
            action()
        }
    }
}

@Composable
fun RequestItem(fromUserId: String, onAccept: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Person, null, modifier = Modifier.size(32.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Text("Lời mời từ ${fromUserId.take(8)}...", modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium, fontSize = 13.sp)
        Button(
            onClick = onAccept,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            modifier = Modifier.height(32.dp)
        ) {
            Text("Chấp nhận", fontSize = 11.sp)
        }
    }
}

@Composable
fun SquadItem(squad: Squad) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(squad.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(squad.habitCategory, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(6.dp))
            Text(squad.description, style = MaterialTheme.typography.bodySmall, maxLines = 2)
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Group, null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("${squad.members.size} thành viên", fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 6.dp)
    )
}
