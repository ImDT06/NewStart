package com.example.newstart.ui.features.social

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.newstart.domain.model.Squad
import com.example.newstart.domain.model.User
import kotlinx.coroutines.flow.Flow
import com.example.newstart.ui.MainViewModel
import com.example.newstart.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SocialScreen(
    mainViewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    viewModel: SocialViewModel = hiltViewModel()
) {
    var searchQuery by remember { mutableStateOf("") }
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val incomingRequests by viewModel.incomingRequests.collectAsStateWithLifecycle()
    val squads by viewModel.squads.collectAsStateWithLifecycle()
    val isSearching by viewModel.isSearching.collectAsStateWithLifecycle()
    val friends by viewModel.friends.collectAsStateWithLifecycle()
    val sentRequests by viewModel.sentRequests.collectAsStateWithLifecycle()
    val currentUserId = viewModel.currentUserId
    
    var selectedTab by remember { mutableStateOf(0) } // 0: Bạn bè, 1: Squads
    var activeSquadDetail by remember { mutableStateOf<Squad?>(null) }
    val activeSquad = squads.find { it.id == activeSquadDetail?.id } ?: activeSquadDetail

    if (activeSquad != null) {
        SquadDetailView(
            squad = activeSquad,
            currentUserId = currentUserId ?: "",
            friends = friends,
            viewModel = viewModel,
            mainViewModel = mainViewModel,
            onBack = { activeSquadDetail = null }
        )
    } else {
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
                        text = { Text(stringResource(R.string.social_tab_friends), fontWeight = FontWeight.Bold, fontSize = 14.sp) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text(stringResource(R.string.social_tab_squads), fontWeight = FontWeight.Bold, fontSize = 14.sp) }
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
                            sentRequests = sentRequests,
                            friends = friends,
                            currentUserId = currentUserId ?: "",
                            onSendRequest = { viewModel.sendRequest(it) },
                            onAcceptRequest = { viewModel.acceptRequest(it) },
                            onDeclineRequest = { viewModel.declineRequest(it) },
                            onRemoveFriend = { viewModel.removeFriend(it) },
                            getUserFlow = { viewModel.getUserById(it) }
                        )
                    } else {
                        SquadsTab(
                            squads = squads,
                            friends = friends,
                            currentUserId = currentUserId ?: "",
                            getUserFlow = { viewModel.getUserById(it) },
                            onCreateSquad = { name, desc, members -> viewModel.createSquad(name, desc, members) },
                            onUpdateSquad = { id, name, desc -> viewModel.updateSquad(id, name, desc) },
                            onSquadClick = { activeSquadDetail = it }
                        )
                    }
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
    sentRequests: List<com.example.newstart.domain.model.FriendRequest>,
    friends: List<com.example.newstart.domain.model.Friendship>,
    currentUserId: String,
    onSendRequest: (String) -> Unit,
    onAcceptRequest: (String) -> Unit,
    onDeclineRequest: (String) -> Unit,
    onRemoveFriend: (String) -> Unit,
    getUserFlow: (String) -> Flow<User>
) {
    Column {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(stringResource(R.string.social_search_placeholder), fontSize = 14.sp) },
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
                item { SectionTitle(stringResource(R.string.social_search_results)) }
                items(searchResults) { user ->
                    val isCurrent = user.id == currentUserId
                    val isFriend = friends.any { it.userIds.contains(user.id) }
                    val isSentPending = sentRequests.any { it.toUserId == user.id }
                    val incomingRequest = incomingRequests.find { it.fromUserId == user.id }

                    UserItem(
                        user = user,
                        action = {
                            when {
                                isCurrent -> {
                                    Text(stringResource(R.string.social_friend_me), color = Color.Gray, fontSize = 13.sp)
                                }
                                isFriend -> {
                                    Text(stringResource(R.string.social_friend_already), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                                isSentPending -> {
                                    Text(stringResource(R.string.social_friend_sent_pending), color = Color.Gray, fontSize = 13.sp)
                                }
                                incomingRequest != null -> {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedButton(
                                            onClick = { onDeclineRequest(incomingRequest.id) },
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                            modifier = Modifier.height(32.dp)
                                        ) {
                                            Text(stringResource(R.string.social_btn_decline), fontSize = 11.sp)
                                        }
                                        Button(
                                            onClick = { onAcceptRequest(incomingRequest.id) },
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                            modifier = Modifier.height(32.dp)
                                        ) {
                                            Text(stringResource(R.string.social_btn_accept), fontSize = 11.sp)
                                        }
                                    }
                                }
                                else -> {
                                    Button(
                                        onClick = { onSendRequest(user.id) },
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Text(stringResource(R.string.social_friend_add), fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    )
                }
            } else {
                if (incomingRequests.isNotEmpty()) {
                    item { SectionTitle(stringResource(R.string.social_incoming_requests_title, incomingRequests.size)) }
                    items(incomingRequests) { request ->
                        RequestItem(
                            fromUserId = request.fromUserId,
                            onAccept = { onAcceptRequest(request.id) },
                            onDecline = { onDeclineRequest(request.id) },
                            getUserFlow = getUserFlow
                        )
                    }
                }

                item { SectionTitle(stringResource(R.string.social_friends_list_title, friends.size)) }
                if (friends.isNotEmpty()) {
                    items(friends) { friendship ->
                        FriendItem(
                            friendship = friendship,
                            currentUserId = currentUserId,
                            getUserFlow = getUserFlow,
                            onRemoveFriend = onRemoveFriend
                        )
                    }
                } else {
                    item {
                        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text(stringResource(R.string.social_friends_empty), color = Color.Gray, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SquadsTab(
    squads: List<Squad>,
    friends: List<com.example.newstart.domain.model.Friendship>,
    currentUserId: String,
    getUserFlow: (String) -> Flow<User>,
    onCreateSquad: (String, String, List<String>) -> Unit,
    onUpdateSquad: (String, String, String) -> Unit,
    onSquadClick: (Squad) -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var squadName by remember { mutableStateOf("") }
    var squadDesc by remember { mutableStateOf("") }
    var selectedMembers by remember { mutableStateOf(setOf<String>()) }
    
    var squadToEdit by remember { mutableStateOf<Squad?>(null) }
    var editSquadName by remember(squadToEdit) { mutableStateOf(squadToEdit?.name ?: "") }
    var editSquadDesc by remember(squadToEdit) { mutableStateOf(squadToEdit?.description ?: "") }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionTitle(stringResource(R.string.social_tab_squads))
            TextButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.social_btn_create_squad), fontSize = 13.sp)
            }
        }

        if (squads.isEmpty()) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Diversity3, null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.social_squads_empty), color = Color.Gray)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 20.dp)
            ) {
                items(squads) { squad ->
                    Box(modifier = Modifier.fillMaxWidth().clickable { onSquadClick(squad) }) {
                        SquadItem(
                            squad = squad,
                            currentUserId = currentUserId,
                            onEditClick = { squadToEdit = squad }
                        )
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { 
                showCreateDialog = false 
                squadName = ""
                squadDesc = ""
                selectedMembers = emptySet()
            },
            title = { Text(stringResource(R.string.squad_create_new_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = squadName,
                        onValueChange = { squadName = it },
                        label = { Text(stringResource(R.string.squad_name)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = squadDesc,
                        onValueChange = { squadDesc = it },
                        label = { Text(stringResource(R.string.squad_desc)) },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                    
                    Text(
                        text = stringResource(R.string.squad_add_friends_label),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    
                    if (friends.isEmpty()) {
                        Text(
                            text = stringResource(R.string.squad_no_friends_to_add),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 150.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                .padding(6.dp)
                        ) {
                            items(friends) { friendship ->
                                val friendId = friendship.userIds.firstOrNull { it != currentUserId } ?: return@items
                                val friendState by remember(friendId) {
                                    getUserFlow(friendId)
                                }.collectAsState(initial = User(name = stringResource(R.string.squad_loading)))
                                
                                val isSelected = selectedMembers.contains(friendId)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedMembers = if (isSelected) {
                                                selectedMembers - friendId
                                            } else {
                                                selectedMembers + friendId
                                            }
                                        }
                                        .padding(vertical = 6.dp, horizontal = 4.dp)
                                ) {
                                    // Custom Circular Check Box
                                    Surface(
                                        modifier = Modifier.size(20.dp),
                                        shape = CircleShape,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        border = if (isSelected) null else BorderStroke(1.5.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.padding(2.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    // Friend Avatar
                                    Surface(
                                        modifier = Modifier.size(32.dp),
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            if (!friendState.avatarUrl.isNullOrEmpty()) {
                                                AsyncImage(
                                                    model = friendState.avatarUrl,
                                                    contentDescription = null,
                                                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                                                    contentScale = ContentScale.Crop
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Default.Person,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = friendState.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (squadName.isNotBlank()) {
                            onCreateSquad(squadName, squadDesc, selectedMembers.toList())
                            showCreateDialog = false
                            squadName = ""
                            squadDesc = ""
                            selectedMembers = emptySet()
                        }
                    },
                    enabled = squadName.isNotBlank()
                ) {
                    Text(stringResource(R.string.squad_btn_create))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        showCreateDialog = false 
                        squadName = ""
                        squadDesc = ""
                        selectedMembers = emptySet()
                    }
                ) {
                    Text(stringResource(R.string.settings_cancel))
                }
            }
        )
    }

    if (squadToEdit != null) {
        AlertDialog(
            onDismissRequest = { squadToEdit = null },
            title = { Text(stringResource(R.string.squad_edit_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = editSquadName,
                        onValueChange = { editSquadName = it },
                        label = { Text(stringResource(R.string.squad_name)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = editSquadDesc,
                        onValueChange = { editSquadDesc = it },
                        label = { Text(stringResource(R.string.squad_desc)) },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val squadId = squadToEdit?.id
                        if (squadId != null && editSquadName.isNotBlank()) {
                            onUpdateSquad(squadId, editSquadName, editSquadDesc)
                            squadToEdit = null
                        }
                    },
                    enabled = editSquadName.isNotBlank()
                ) {
                    Text(stringResource(R.string.social_btn_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { squadToEdit = null }) {
                    Text(stringResource(R.string.settings_cancel))
                }
            }
        )
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
fun RequestItem(
    fromUserId: String,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    getUserFlow: (String) -> Flow<User>
) {
    val userState by remember(fromUserId) {
        getUserFlow(fromUserId)
    }.collectAsState(initial = User(name = stringResource(R.string.squad_loading)))

    val displayName = if (userState.name.isNotBlank()) userState.name else stringResource(R.string.social_default_user_name)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!userState.avatarUrl.isNullOrEmpty()) {
            AsyncImage(
                model = userState.avatarUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(Icons.Default.Person, null, modifier = Modifier.size(32.dp))
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = stringResource(R.string.social_friend_request_from, displayName),
            modifier = Modifier.weight(1f),
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = onDecline,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text(stringResource(R.string.social_btn_decline), fontSize = 11.sp)
            }
            Button(
                onClick = onAccept,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text(stringResource(R.string.social_btn_accept), fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun FriendItem(
    friendship: com.example.newstart.domain.model.Friendship,
    currentUserId: String,
    getUserFlow: (String) -> Flow<User>,
    onRemoveFriend: (String) -> Unit
) {
    val friendId = friendship.userIds.firstOrNull { it != currentUserId } ?: return
    val userState by remember(friendId) {
        getUserFlow(friendId)
    }.collectAsState(initial = User(name = stringResource(R.string.squad_loading)))

    var showConfirmDialog by remember { mutableStateOf(false) }
    val displayName = if (userState.name.isNotBlank()) userState.name else stringResource(R.string.social_default_user_name)

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text(stringResource(R.string.social_btn_unfriend)) },
            text = { Text(stringResource(R.string.social_unfriend_confirm_msg, displayName)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onRemoveFriend(friendship.id)
                        showConfirmDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.social_btn_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text(stringResource(R.string.settings_cancel))
                }
            }
        )
    }

    UserItem(
        user = userState,
        action = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.social_friend_already),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                IconButton(
                    onClick = { showConfirmDialog = true },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.social_btn_unfriend),
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    )
}

@Composable
fun SquadItem(
    squad: Squad,
    currentUserId: String,
    onEditClick: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(squad.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    if (squad.habitCategory.isNotBlank()) {
                        Text(squad.habitCategory, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
                if (squad.adminId == currentUserId && onEditClick != null) {
                    IconButton(onClick = onEditClick, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(R.string.squad_edit_title),
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(squad.description, style = MaterialTheme.typography.bodySmall, maxLines = 2)
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Group, null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.social_members_count, squad.members.size), fontSize = 11.sp)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SquadDetailView(
    squad: Squad,
    currentUserId: String,
    friends: List<com.example.newstart.domain.model.Friendship>,
    viewModel: SocialViewModel,
    mainViewModel: MainViewModel,
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Chat, 1: Members & Habits
    val messages by viewModel.getSquadMessages(squad.id).collectAsState(initial = emptyList())
    var messageText by remember { mutableStateOf("") }
    
    var showAddMemberDialog by remember { mutableStateOf(false) }
    var showCreateHabitDialog by remember { mutableStateOf(false) }
    val lazyListState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            lazyListState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(squad.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(stringResource(R.string.social_members_count, squad.members.size), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
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
                .imePadding()
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
                    text = { Text(stringResource(R.string.squad_tab_messages), fontWeight = FontWeight.Bold, fontSize = 14.sp) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text(stringResource(R.string.squad_tab_members_habits), fontWeight = FontWeight.Bold, fontSize = 14.sp) }
                )
            }

            if (selectedTab == 0) {
                Column(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp)) {
                        if (messages.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(stringResource(R.string.squad_chat_empty), color = Color.Gray, fontSize = 13.sp)
                            }
                        } else {
                            LazyColumn(
                                state = lazyListState,
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(vertical = 12.dp)
                            ) {
                                items(messages) { message ->
                                    val isMe = message.senderId == currentUserId
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
                                    ) {
                                        if (!isMe) {
                                            Surface(
                                                modifier = Modifier.size(28.dp),
                                                shape = CircleShape,
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                            ) {
                                                val userFlow = remember(message.senderId) { viewModel.getUserById(message.senderId) }
                                                val userState by userFlow.collectAsState(initial = User())
                                                Box(contentAlignment = Alignment.Center) {
                                                    if (!userState.avatarUrl.isNullOrEmpty()) {
                                                        AsyncImage(
                                                            model = userState.avatarUrl,
                                                            contentDescription = null,
                                                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                                                            contentScale = ContentScale.Crop
                                                        )
                                                    } else {
                                                        Icon(Icons.Default.Person, null, modifier = Modifier.size(16.dp))
                                                    }
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                        }

                                        Column(
                                            horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
                                        ) {
                                            if (!isMe) {
                                                Text(message.senderName, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                                Spacer(modifier = Modifier.height(2.dp))
                                            }
                                            Surface(
                                                color = if (isMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                                shape = RoundedCornerShape(
                                                    topStart = 12.dp,
                                                    topEnd = 12.dp,
                                                    bottomStart = if (isMe) 12.dp else 0.dp,
                                                    bottomEnd = if (isMe) 0.dp else 12.dp
                                                )
                                            ) {
                                                Text(
                                                    text = message.text,
                                                    color = if (isMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = messageText,
                            onValueChange = { messageText = it },
                            placeholder = { Text(stringResource(R.string.squad_chat_placeholder)) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(24.dp),
                            maxLines = 3
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                if (messageText.isNotBlank()) {
                                    viewModel.sendSquadMessage(squad.id, messageText.trim())
                                    messageText = ""
                                }
                            },
                            enabled = messageText.isNotBlank(),
                            modifier = Modifier.background(MaterialTheme.colorScheme.primary, CircleShape)
                        ) {
                            Icon(Icons.Default.Send, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SectionTitle(stringResource(R.string.squad_members_title))
                            if (squad.adminId == currentUserId) {
                                TextButton(onClick = { showAddMemberDialog = true }) {
                                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(stringResource(R.string.squad_add_member), fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    items(squad.members) { memberId ->
                        val userFlow = remember(memberId) { viewModel.getUserById(memberId) }
                        val userState by userFlow.collectAsState(initial = User(name = stringResource(R.string.squad_loading)))
                        val isAdmin = memberId == squad.adminId

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                modifier = Modifier.size(32.dp),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
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
                                        Icon(Icons.Default.Person, null, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(userState.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(
                                    text = if (isAdmin) stringResource(R.string.squad_role_admin) else stringResource(R.string.squad_role_member),
                                    fontSize = 11.sp,
                                    color = if (isAdmin) MaterialTheme.colorScheme.primary else Color.Gray
                                )
                            }
                            if (squad.adminId == currentUserId && memberId != currentUserId) {
                                IconButton(
                                    onClick = { viewModel.removeMemberFromSquad(squad.id, memberId) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SectionTitle(stringResource(R.string.squad_shared_habits_title))
                            Button(
                                onClick = { showCreateHabitDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(stringResource(R.string.squad_dialog_create_shared_habit), fontSize = 12.sp)
                            }
                        }
                        Text(
                            text = stringResource(R.string.squad_shared_habits_desc),
                            fontSize = 11.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }

    if (showAddMemberDialog) {
        val nonMembers = friends.mapNotNull { friendship ->
            val friendId = friendship.userIds.firstOrNull { it != currentUserId }
            if (friendId != null && !squad.members.contains(friendId)) friendId else null
        }

        AlertDialog(
            onDismissRequest = { showAddMemberDialog = false },
            title = { Text(stringResource(R.string.squad_add_member)) },
            text = {
                if (nonMembers.isEmpty()) {
                    Text(stringResource(R.string.squad_add_member_empty), color = Color.Gray)
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(nonMembers) { friendId ->
                            val userFlow = remember(friendId) { viewModel.getUserById(friendId) }
                            val userState by userFlow.collectAsState(initial = User(name = stringResource(R.string.squad_loading)))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.addMemberToSquad(squad.id, friendId)
                                        showAddMemberDialog = false
                                    }
                                    .padding(vertical = 8.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    modifier = Modifier.size(32.dp),
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
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
                                            Icon(Icons.Default.Person, null, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(userState.name, style = MaterialTheme.typography.bodyMedium)
                                Spacer(modifier = Modifier.weight(1f))
                                Text(stringResource(R.string.social_btn_add), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAddMemberDialog = false }) {
                    Text(stringResource(R.string.journal_close_button))
                }
            }
        )
    }

    if (showCreateHabitDialog) {
        CreateSharedHabitDialog(
            squadId = squad.id,
            onDismiss = { showCreateHabitDialog = false },
            onConfirm = { name, icon, reminderTime, colorHex ->
                mainViewModel.saveHabit(
                    name = name,
                    icon = icon,
                    goal = "1",
                    colorHex = colorHex,
                    reminderTime = reminderTime,
                    squadId = squad.id,
                    onSuccess = {
                        showCreateHabitDialog = false
                    }
                )
            }
        )
    }
}

@Composable
fun CreateSharedHabitDialog(
    squadId: String,
    onDismiss: () -> Unit,
    onConfirm: (name: String, icon: String, reminderTime: String?, colorHex: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var icon by remember { mutableStateOf("✨") }
    var reminderTime by remember { mutableStateOf("08:00") }
    var colorHex by remember { mutableStateOf("#1D5FE2") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.squad_dialog_create_shared_habit)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.squad_habit_name)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = icon,
                    onValueChange = { icon = it },
                    label = { Text(stringResource(R.string.squad_habit_icon)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = reminderTime,
                    onValueChange = { reminderTime = it },
                    label = { Text(stringResource(R.string.squad_habit_reminder)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name, icon, reminderTime, colorHex)
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text(stringResource(R.string.squad_btn_create))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.settings_cancel))
            }
        }
    )
}
