package com.example.newstart.ui.features.social

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.newstart.domain.model.FriendRequest
import com.example.newstart.domain.model.Friendship
import com.example.newstart.domain.model.Squad
import com.example.newstart.domain.model.User
import com.example.newstart.domain.repository.SocialRepository
import com.example.newstart.domain.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.example.newstart.domain.model.SquadMessage
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import java.util.Date

@HiltViewModel
class SocialViewModel @Inject constructor(
    private val socialRepository: SocialRepository,
    private val userRepository: UserRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _sendingMessages = MutableStateFlow<Map<String, List<SquadMessage>>>(emptyMap())

    val friends: StateFlow<List<Friendship>> = socialRepository.getFriends()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val incomingRequests: StateFlow<List<FriendRequest>> = socialRepository.getIncomingRequests()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sentRequests: StateFlow<List<FriendRequest>> = socialRepository.getSentRequests()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentUserId: String?
        get() = auth.currentUser?.uid

    val squads: StateFlow<List<Squad>> = socialRepository.getSquads()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<User>>(emptyList())
    val searchResults: StateFlow<List<User>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private var searchJob: kotlinx.coroutines.Job? = null

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        searchJob?.cancel()
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            _isSearching.value = false
            return
        }
        searchJob = viewModelScope.launch {
            kotlinx.coroutines.delay(500)
            _isSearching.value = true
            try {
                _searchResults.value = userRepository.searchUsers(query)
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun sendRequest(userId: String) {
        viewModelScope.launch {
            socialRepository.sendFriendRequest(userId)
        }
    }

    fun acceptRequest(requestId: String) {
        viewModelScope.launch {
            socialRepository.acceptFriendRequest(requestId)
        }
    }

    fun removeFriend(friendshipId: String) {
        viewModelScope.launch {
            socialRepository.removeFriend(friendshipId)
        }
    }

    fun declineRequest(requestId: String) {
        viewModelScope.launch {
            socialRepository.declineFriendRequest(requestId)
        }
    }

    fun createSquad(name: String, description: String, members: List<String>) {
        viewModelScope.launch {
            socialRepository.createSquad(Squad(name = name, description = description, members = members))
        }
    }

    fun updateSquad(squadId: String, name: String, description: String) {
        viewModelScope.launch {
            socialRepository.updateSquad(squadId, name, description)
        }
    }

    fun addMemberToSquad(squadId: String, memberId: String) {
        viewModelScope.launch {
            socialRepository.addMemberToSquad(squadId, memberId)
        }
    }

    fun removeMemberFromSquad(squadId: String, memberId: String) {
        viewModelScope.launch {
            socialRepository.removeMemberFromSquad(squadId, memberId)
        }
    }

    fun getSquadMessages(squadId: String): Flow<List<SquadMessage>> {
        return socialRepository.getSquadMessages(squadId).combine(_sendingMessages) { realMessages, sendingMap ->
            val sendingForSquad = sendingMap[squadId] ?: emptyList()
            val realTexts = realMessages.takeLast(10).map { it.text }
            val uniqueSending = sendingForSquad.filter { it.text !in realTexts }
            realMessages + uniqueSending
        }
    }

    fun sendSquadMessage(squadId: String, text: String) {
        val uid = currentUserId ?: return
        val tempMsg = SquadMessage(
            id = UUID.randomUUID().toString(),
            senderId = uid,
            senderName = "Tôi",
            text = text,
            timestamp = Date()
        )
        
        // Add to sending messages map
        val currentMap = _sendingMessages.value.toMutableMap()
        val currentList = currentMap[squadId] ?: emptyList()
        currentMap[squadId] = currentList + tempMsg
        _sendingMessages.value = currentMap
        
        viewModelScope.launch {
            try {
                socialRepository.sendSquadMessage(squadId, text)
            } finally {
                // Clean up the temp message after send is complete/failed
                val updatedMap = _sendingMessages.value.toMutableMap()
                val updatedList = updatedMap[squadId] ?: emptyList()
                updatedMap[squadId] = updatedList.filter { it.id != tempMsg.id }
                _sendingMessages.value = updatedMap
            }
        }
    }

    fun getUserById(userId: String) = userRepository.getUserById(userId)
}
