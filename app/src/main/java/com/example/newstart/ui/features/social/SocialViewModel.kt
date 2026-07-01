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
import kotlinx.coroutines.flow.Flow

import android.net.Uri
import kotlinx.coroutines.flow.asStateFlow

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

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<User>>(emptyList())
    val searchResults: StateFlow<List<User>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _isCreatingSquad = MutableStateFlow(false)
    val isCreatingSquad: StateFlow<Boolean> = _isCreatingSquad.asStateFlow()

    private val _isImageUploading = MutableStateFlow(false)
    val isImageUploading: StateFlow<Boolean> = _isImageUploading.asStateFlow()

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
            socialRepository.refreshFriends()
        }
    }

    fun removeFriend(friendshipId: String) {
        viewModelScope.launch {
            socialRepository.removeFriend(friendshipId)
            socialRepository.refreshFriends()
        }
    }

    fun declineRequest(requestId: String) {
        viewModelScope.launch {
            socialRepository.declineFriendRequest(requestId)
        }
    }

    fun createSquad(name: String, description: String, members: List<String>) {
        if (_isCreatingSquad.value) return
        viewModelScope.launch {
            try {
                _isCreatingSquad.value = true
                socialRepository.createSquad(Squad(name = name, description = description, members = members))
                socialRepository.refreshSquads()
            } finally {
                _isCreatingSquad.value = false
            }
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

    fun getSquadMessages(squadId: String): Flow<List<SquadMessage>> = socialRepository.getSquadMessages(squadId)

    fun sendSquadMessage(squadId: String, text: String, imageUris: List<Uri> = emptyList(), imageUri: Uri? = null) {
        viewModelScope.launch {
            val urisToUpload = if (imageUris.isNotEmpty()) imageUris else if (imageUri != null) listOf(imageUri) else emptyList()
            var imageUrls: List<String> = emptyList()
            if (urisToUpload.isNotEmpty()) {
                _isImageUploading.value = true
                try {
                    val uploadedUrls = mutableListOf<String>()
                    urisToUpload.forEach { uri ->
                        socialRepository.uploadImage(uri).onSuccess { url ->
                            uploadedUrls.add(url)
                        }.onFailure { e ->
                            android.util.Log.e("SocialViewModel", "Failed to upload squad image: ${e.message}", e)
                        }
                    }
                    imageUrls = uploadedUrls
                } finally {
                    _isImageUploading.value = false
                }
            }
            val firstImageUrl = imageUrls.firstOrNull()
            socialRepository.sendSquadMessage(squadId, text, imageUrls, firstImageUrl)
        }
    }

    fun refreshFriends() {
        viewModelScope.launch {
            socialRepository.refreshFriends()
        }
    }

    fun refreshSquads() {
        viewModelScope.launch {
            socialRepository.refreshSquads()
        }
    }

    fun getUserById(userId: String) = userRepository.getUserById(userId)
}
