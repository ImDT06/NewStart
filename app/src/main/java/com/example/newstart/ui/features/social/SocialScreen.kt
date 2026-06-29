package com.example.newstart.ui.features.social

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.newstart.domain.model.Squad
import com.example.newstart.domain.model.User
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date
import com.example.newstart.ui.MainViewModel
import com.example.newstart.R
import com.example.newstart.ui.theme.LocalDarkTheme
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SocialScreen(
    mainViewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    viewModel: SocialViewModel = hiltViewModel()
) {
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val incomingRequests by viewModel.incomingRequests.collectAsStateWithLifecycle()
    val squads by viewModel.squads.collectAsStateWithLifecycle()
    val isSearching by viewModel.isSearching.collectAsStateWithLifecycle()
    val friends by viewModel.friends.collectAsStateWithLifecycle()
    val sentRequests by viewModel.sentRequests.collectAsStateWithLifecycle()
    val currentUserId = viewModel.currentUserId
    var selectedTab by remember { mutableStateOf(0) }
    var activeSquadDetail by remember { mutableStateOf<Squad?>(null) }
    val activeSquad = squads.find { it.id == activeSquadDetail?.id } ?: activeSquadDetail
    val isDark = LocalDarkTheme.current
    
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
                // Modern Segmented Control Style Tabs for Friends vs Squads
                val tabs = listOf(
                    stringResource(R.string.social_tab_friends),
                    stringResource(R.string.social_tab_squads)
                )

                BoxWithConstraints(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .fillMaxWidth()
                        .height(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isDark) Color(0xFF1E1E1E) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    val totalWidth = maxWidth
                    val tabWidth = totalWidth / tabs.size
                    
                    val indicatorOffset by animateDpAsState(
                        targetValue = tabWidth * selectedTab,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        ),
                        label = "TabIndicatorOffset"
                    )

                    Box(
                        modifier = Modifier
                            .padding(3.dp)
                            .offset(x = indicatorOffset)
                            .width(tabWidth - 6.dp)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(9.dp))
                            .background(MaterialTheme.colorScheme.primary)
                    )

                    Row(
                        modifier = Modifier.fillMaxSize().padding(3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        tabs.forEachIndexed { index, title ->
                            val isSelected = selectedTab == index
                            val textColor by animateColorAsState(
                                targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary 
                                              else MaterialTheme.colorScheme.onSurfaceVariant,
                                animationSpec = tween(durationMillis = 200),
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
                                        onClick = { selectedTab = index }
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

                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    if (selectedTab == 0) {
                        FriendsTabWrapper(
                            searchQuery = searchQuery,
                            onSearchQueryChange = { 
                                viewModel.onSearchQueryChange(it)
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
                        SquadsTabWrapper(
                            squads = squads,
                            friends = friends,
                            currentUserId = currentUserId ?: "",
                            getUserFlow = { id -> viewModel.getUserById(id) },
                            onCreateSquad = { n, d, m -> viewModel.createSquad(n, d, m) },
                            onUpdateSquad = { id, n, d -> viewModel.updateSquad(id, n, d) },
                            onSquadClick = { activeSquadDetail = it }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FriendsTabWrapper(
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
        android.util.Log.d("SocialScreen", "FriendsTabWrapper recomposed with searchQuery = '$searchQuery'")
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = if (LocalDarkTheme.current) Color(0xFF1E1E1E) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(16.dp)
                ),
            placeholder = { Text(stringResource(R.string.social_search_placeholder), fontSize = 14.sp) },
            leadingIcon = { 
                Icon(
                    imageVector = Icons.Default.Search, 
                    contentDescription = null, 
                    modifier = Modifier.size(20.dp),
                    tint = if (searchQuery.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                ) 
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Default.Clear, 
                            contentDescription = "Clear",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color.Transparent,
                disabledBorderColor = Color.Transparent,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent
            ),
            shape = RoundedCornerShape(16.dp),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium
        )

        Box(modifier = Modifier.fillMaxWidth().height(14.dp), contentAlignment = Alignment.Center) {
            if (isSearching) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(2.dp))
            }
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
fun SquadsTabWrapper(
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
                            getUserFlow = getUserFlow,
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
    val isDark = LocalDarkTheme.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF161618) else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (isDark) Color(0xFF252528) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(46.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (!user.avatarUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = user.avatarUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person, 
                            contentDescription = null, 
                            modifier = Modifier.size(24.dp), 
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.name.ifBlank { stringResource(R.string.social_default_user_name) }, 
                    fontWeight = FontWeight.ExtraBold, 
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Box(modifier = Modifier.padding(start = 8.dp)) {
                action()
            }
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
    val isDark = LocalDarkTheme.current
    val userState by remember(fromUserId) {
        getUserFlow(fromUserId)
    }.collectAsState(initial = User(name = stringResource(R.string.squad_loading)))

    val displayName = if (userState.name.isNotBlank()) userState.name else stringResource(R.string.social_default_user_name)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF1B1B1E) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
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
                        Icon(Icons.Default.Person, null, modifier = Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayName,
                    fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = stringResource(R.string.social_friend_request_from, ""),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                IconButton(
                    onClick = onDecline,
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            color = if (isDark) Color(0xFF2C2C2F) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Close, 
                        contentDescription = stringResource(R.string.social_btn_decline),
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                }
                IconButton(
                    onClick = onAccept,
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Check, 
                        contentDescription = stringResource(R.string.social_btn_accept),
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
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
            IconButton(
                onClick = { showConfirmDialog = true },
                modifier = Modifier
                    .size(30.dp)
                    .background(
                        color = if (LocalDarkTheme.current) Color(0xFF2C2C2F) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.social_btn_unfriend),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    )
}

@Composable
fun SquadItem(
    squad: Squad,
    currentUserId: String,
    getUserFlow: (String) -> Flow<User>,
    onEditClick: (() -> Unit)? = null
) {
    val isDark = LocalDarkTheme.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF161618) else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (isDark) Color(0xFF252528) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = squad.name, 
                        style = MaterialTheme.typography.titleMedium, 
                        fontWeight = FontWeight.ExtraBold
                    )
                    if (squad.habitCategory.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                            contentColor = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                text = squad.habitCategory,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                if (squad.adminId == currentUserId && onEditClick != null) {
                    IconButton(
                        onClick = onEditClick, 
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                color = if (isDark) Color(0xFF252528) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(R.string.squad_edit_title),
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = squad.description, 
                style = MaterialTheme.typography.bodyMedium, 
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                maxLines = 2
            )
            Spacer(Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Overlapping Avatar Stack of members
                val firstMembers = squad.members.take(3)
                val memberUsers = firstMembers.map { memberId ->
                    remember(memberId) { getUserFlow(memberId) }.collectAsState(initial = User())
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy((-10).dp)
                ) {
                    memberUsers.forEach { userState ->
                        val user = userState.value
                        Surface(
                            modifier = Modifier.size(28.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.5.dp, if (isDark) Color(0xFF161618) else MaterialTheme.colorScheme.surface)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (!user.avatarUrl.isNullOrEmpty()) {
                                    AsyncImage(
                                        model = user.avatarUrl,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Person, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }
                    
                    if (squad.members.size > 3) {
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = "+${squad.members.size - 3}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(
                            color = if (isDark) Color(0xFF252528) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Group, 
                        contentDescription = null, 
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.social_members_count, squad.members.size), 
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
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

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun SquadDetailView(
    squad: Squad,
    currentUserId: String,
    friends: List<com.example.newstart.domain.model.Friendship>,
    viewModel: SocialViewModel,
    mainViewModel: MainViewModel,
    onBack: () -> Unit,
    hasBottomBar: Boolean = false
) {
    val isDark = LocalDarkTheme.current
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    var selectedTab by remember { mutableStateOf(0) } // 0: Chat, 1: Members & Habits
    val messages by viewModel.getSquadMessages(squad.id).collectAsState(initial = emptyList())
    var messageText by remember { mutableStateOf("") }
    var activeTimestampMessageId by remember { mutableStateOf<String?>(null) }

    var showAddMemberDialog by remember { mutableStateOf(false) }
    var showCreateHabitDialog by remember { mutableStateOf(false) }
    val lazyListState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            lazyListState.animateScrollToItem(messages.size - 1)
        }
    }

    LaunchedEffect(lazyListState.isScrollInProgress) {
        if (lazyListState.isScrollInProgress) {
            focusManager.clearFocus()
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
        },
        bottomBar = {
            if (selectedTab == 0) {
                // Floating input capsule aligned at the bottom using Scaffold's bottomBar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(bottom = if (hasBottomBar) 64.dp else 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .shadow(8.dp, shape = RoundedCornerShape(24.dp))
                            .background(
                                color = if (isDark) Color(0xFF1E1E22) else Color.White,
                                shape = RoundedCornerShape(24.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = if (isDark) Color(0xFF2D2D32) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(24.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.foundation.text.BasicTextField(
                            value = messageText,
                            onValueChange = { messageText = it },
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            maxLines = 4,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
                            decorationBox = { innerTextField ->
                                Box(contentAlignment = Alignment.CenterStart) {
                                    if (messageText.isEmpty()) {
                                        Text(
                                            text = stringResource(R.string.squad_chat_placeholder),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )
                        
                        val sendButtonScale by animateFloatAsState(
                            targetValue = if (messageText.isNotBlank()) 1f else 0.85f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            ),
                            label = "SendButtonScale"
                        )
                        
                        IconButton(
                            onClick = {
                                if (messageText.isNotBlank()) {
                                    viewModel.sendSquadMessage(squad.id, messageText.trim())
                                    messageText = ""
                                }
                            },
                            enabled = messageText.isNotBlank(),
                            modifier = Modifier
                                .scale(sendButtonScale)
                                .size(40.dp)
                                .background(
                                    color = if (messageText.isNotBlank()) MaterialTheme.colorScheme.primary 
                                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                                    shape = CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send, 
                                contentDescription = "Send", 
                                tint = if (messageText.isNotBlank()) MaterialTheme.colorScheme.onPrimary 
                                       else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f), 
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
                .padding(bottom = if (selectedTab == 0) 0.dp else padding.calculateBottomPadding())
        ) {
            // Modern Segmented Control Style Tabs for Chat vs Members & Habits
            val detailTabs = listOf(
                stringResource(R.string.squad_tab_messages),
                stringResource(R.string.squad_tab_members_habits)
            )

            BoxWithConstraints(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .fillMaxWidth()
                    .height(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isDark) Color(0xFF1E1E1E) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                val totalWidth = maxWidth
                val tabWidth = totalWidth / detailTabs.size
                
                val indicatorOffset by animateDpAsState(
                    targetValue = tabWidth * selectedTab,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    label = "SquadDetailTabIndicatorOffset"
                )

                Box(
                    modifier = Modifier
                        .padding(3.dp)
                        .offset(x = indicatorOffset)
                        .width(tabWidth - 6.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(9.dp))
                        .background(MaterialTheme.colorScheme.primary)
                )

                Row(
                    modifier = Modifier.fillMaxSize().padding(3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    detailTabs.forEachIndexed { index, title ->
                        val isSelected = selectedTab == index
                        val textColor by animateColorAsState(
                            targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary 
                                          else MaterialTheme.colorScheme.onSurfaceVariant,
                            animationSpec = tween(durationMillis = 200),
                            label = "SquadDetailTabTextColor"
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(9.dp))
                                .clickable(
                                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                    indication = null,
                                    onClick = { selectedTab = index }
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

            if (selectedTab == 0) {
                val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .pointerInput(Unit) {
                            detectTapGestures(onTap = { focusManager.clearFocus() })
                        }
                ) {
                    if (messages.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(stringResource(R.string.squad_chat_empty), color = Color.Gray, fontSize = 13.sp)
                        }
                    } else {
                        LazyColumn(
                            state = lazyListState,
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(Unit) {
                                    detectTapGestures(onTap = { focusManager.clearFocus() })
                                },
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                top = 12.dp,
                                bottom = padding.calculateBottomPadding() + 8.dp
                            )
                        ) {
                            items(messages) { message ->
                                val isMe = message.senderId == currentUserId
                                val timeString = remember(message.timestamp) { timeFormatter.format(message.timestamp) }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
                                ) {
                                    if (!isMe) {
                                        Surface(
                                            modifier = Modifier.size(32.dp),
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
                                            Text(
                                                text = message.senderName, 
                                                style = MaterialTheme.typography.labelSmall, 
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                        }
                                        
                                        val isTimestampVisible = activeTimestampMessageId == message.id
                                        Surface(
                                            color = if (isMe) MaterialTheme.colorScheme.primary else {
                                                if (isDark) Color(0xFF252528) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                            },
                                            shape = RoundedCornerShape(
                                                topStart = 16.dp,
                                                topEnd = 16.dp,
                                                bottomStart = if (isMe) 16.dp else 4.dp,
                                                bottomEnd = if (isMe) 4.dp else 16.dp
                                            ),
                                            modifier = Modifier.clickable(
                                                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                                indication = null,
                                                onClick = {
                                                    activeTimestampMessageId = if (isTimestampVisible) null else message.id
                                                }
                                            )
                                        ) {
                                            Text(
                                                text = message.text,
                                                color = if (isMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp)
                                            )
                                        }
                                        
                                        androidx.compose.animation.AnimatedVisibility(
                                            visible = isTimestampVisible,
                                            enter = androidx.compose.animation.expandVertically() + androidx.compose.animation.fadeIn(),
                                            exit = androidx.compose.animation.shrinkVertically() + androidx.compose.animation.fadeOut()
                                        ) {
                                            Text(
                                                text = timeString,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                                modifier = Modifier.padding(top = 2.dp, start = if (isMe) 0.dp else 4.dp, end = if (isMe) 4.dp else 0.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
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

@Composable
fun SquadDetailViewWrapper(
    squad: Squad,
    currentUserId: String,
    friends: List<com.example.newstart.domain.model.Friendship>,
    viewModel: SocialViewModel,
    mainViewModel: MainViewModel,
    onBack: () -> Unit,
    hasBottomBar: Boolean = false
) {
    SquadDetailView(
        squad = squad,
        currentUserId = currentUserId,
        friends = friends,
        viewModel = viewModel,
        mainViewModel = mainViewModel,
        onBack = onBack,
        hasBottomBar = hasBottomBar
    )
}
