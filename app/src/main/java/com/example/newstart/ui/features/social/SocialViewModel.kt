package com.example.newstart.ui.features.social

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.newstart.domain.model.FriendRequest
import com.example.newstart.domain.model.Friendship
import com.example.newstart.domain.model.Squad
import com.example.newstart.domain.model.User
import com.example.newstart.domain.repository.SocialRepository
import com.example.newstart.domain.repository.UserRepository
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
    private val userRepository: UserRepository
) : ViewModel() {

    val friends: StateFlow<List<Friendship>> = socialRepository.getFriends()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val incomingRequests: StateFlow<List<FriendRequest>> = socialRepository.getIncomingRequests()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
}
