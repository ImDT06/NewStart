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

@HiltViewModel
class SocialViewModel @Inject constructor(
    private val socialRepository: SocialRepository,
    private val userRepository: UserRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

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

    private val _searchResults = MutableStateFlow<List<User>>(emptyList())
    val searchResults: StateFlow<List<User>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    fun searchUsers(query: String) {
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }
        viewModelScope.launch {
            _isSearching.value = true
            _searchResults.value = userRepository.searchUsers(query)
            _isSearching.value = false
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

    fun getSquadMessages(squadId: String) = socialRepository.getSquadMessages(squadId)

    fun sendSquadMessage(squadId: String, text: String) {
        viewModelScope.launch {
            socialRepository.sendSquadMessage(squadId, text)
        }
    }

    fun getUserById(userId: String) = userRepository.getUserById(userId)
}
