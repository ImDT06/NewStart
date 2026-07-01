package com.example.newstart.ui.features.journal

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.newstart.domain.model.JournalEntry
import com.example.newstart.domain.model.JournalType
import com.example.newstart.domain.repository.JournalRepository
import com.example.newstart.domain.usecase.SaveJournalEntryUseCase
import com.example.newstart.domain.repository.UserRepository
import com.example.newstart.domain.model.User
import kotlinx.coroutines.flow.Flow
import com.example.newstart.domain.repository.SocialRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class JournalViewModel @Inject constructor(
    private val journalRepository: JournalRepository,
    private val socialRepository: SocialRepository,
    private val saveJournalEntryUseCase: SaveJournalEntryUseCase,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _socialFeed = MutableStateFlow<List<JournalEntry>>(emptyList())
    val socialFeed: StateFlow<List<JournalEntry>> = _socialFeed.asStateFlow()

    private val _isRefreshingFeed = MutableStateFlow(false)
    val isRefreshingFeed: StateFlow<Boolean> = _isRefreshingFeed.asStateFlow()

    init {
        viewModelScope.launch {
            socialRepository.getSocialFeed().collect { entries ->
                _socialFeed.value = entries.filter { 
                    it.privacy != com.example.newstart.domain.model.JournalPrivacy.PRIVATE 
                }
            }
        }
    }

    fun getUserById(userId: String): Flow<User> = userRepository.getUserById(userId)

    fun reactToPost(postId: String, emoji: String) {
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return
        
        // Optimistic Update
        _socialFeed.value = _socialFeed.value.map { entry ->
            if (entry.id == postId) {
                val newReactions = entry.reactions.toMutableMap()
                if (newReactions[uid] == emoji) {
                    newReactions.remove(uid)
                } else {
                    newReactions[uid] = emoji
                }
                entry.copy(reactions = newReactions)
            } else {
                entry
            }
        }

        viewModelScope.launch {
            try {
                socialRepository.reactToPost(postId, emoji)
            } catch (e: Exception) {
                refreshSocialFeed()
            }
        }
    }

    private val _selectedDateRange = MutableStateFlow<Pair<LocalDate, LocalDate?>>(LocalDate.now() to null)
    val selectedDateRange: StateFlow<Pair<LocalDate, LocalDate?>> = _selectedDateRange.asStateFlow()

    private val _currentTab = MutableStateFlow(0) 
    val currentTab: StateFlow<Int> = _currentTab.asStateFlow()

    val selectedDate: StateFlow<LocalDate> = _selectedDateRange.map { it.first }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LocalDate.now())

    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading.asStateFlow()

    val allEntriesWithImages: StateFlow<List<JournalEntry>> = journalRepository.getJournalEntries()
        .map { entries -> 
            entries.filter { it.imageUrl != null }.sortedByDescending { it.timestamp } 
        }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val uniqueMovieTitles: StateFlow<List<String>> = journalRepository.getJournalEntries()
        .map { entries ->
            entries.filter { it.type == JournalType.MOVIE && it.movieDetails != null }
                .map { it.movieDetails!!.title.trim() }
                .filter { it.isNotEmpty() }
                .distinct()
                .sorted()
        }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val uniqueBookTitles: StateFlow<List<String>> = journalRepository.getJournalEntries()
        .map { entries ->
            entries.filter { it.type == JournalType.BOOK && it.bookDetails != null }
                .map { it.bookDetails!!.title.trim() }
                .filter { it.isNotEmpty() }
                .distinct()
                .sorted()
        }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val uniqueSubjectNames: StateFlow<List<String>> = journalRepository.getJournalEntries()
        .map { entries ->
            entries.filter { it.type == JournalType.SUBJECT && it.subjectDetails != null }
                .map { it.subjectDetails!!.name.trim() }
                .filter { it.isNotEmpty() }
                .distinct()
                .sorted()
        }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val entries: StateFlow<List<JournalEntry>> = _selectedDateRange
        .flatMapLatest { range ->
            val start = range.first
            val end = range.second ?: start
            journalRepository.getJournalEntries().map { allEntries ->
                allEntries.filter { entry ->
                    entry.timestamp?.let { timestamp ->
                        val entryDate = timestamp.toInstant()
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                        if (range.second == null) {
                            entryDate == start
                        } else {
                            (entryDate == start || entryDate == end || (entryDate.isAfter(start) && entryDate.isBefore(end)))
                        }
                    } ?: false
                }.sortedByDescending { it.timestamp }
            }.flowOn(Dispatchers.Default)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun refreshSocialFeed() {
        viewModelScope.launch {
            _isRefreshingFeed.value = true
            try {
                socialRepository.refreshSocialFeed()
                kotlinx.coroutines.delay(800)
            } catch (e: Exception) {
                android.util.Log.e("JournalViewModel", "Error refreshing feed: ${e.message}")
            } finally {
                _isRefreshingFeed.value = false
            }
        }
    }

    fun onDateSelected(date: LocalDate) {
        _selectedDateRange.value = date to null
    }

    fun onDateRangeSelected(start: LocalDate, end: LocalDate?) {
        _selectedDateRange.value = start to end
    }

    fun onTabSelected(index: Int) {
        _currentTab.value = index
    }

    fun setQuickFilter(filter: String) {
        val today = LocalDate.now()
        when (filter) {
            "All" -> _selectedDateRange.value = LocalDate.of(2000, 1, 1) to LocalDate.of(2100, 12, 31)
            "Year" -> _selectedDateRange.value = today.withDayOfYear(1) to today.withDayOfYear(today.lengthOfYear())
            "Month" -> _selectedDateRange.value = today.withDayOfMonth(1) to today.withDayOfMonth(today.lengthOfMonth())
            "Week" -> _selectedDateRange.value = today.with(java.time.DayOfWeek.MONDAY) to today.with(java.time.DayOfWeek.SUNDAY)
            "Today" -> _selectedDateRange.value = today to null
        }
    }

    fun addEntry(emoji: String, text: String, imageUri: Uri?, imageSource: String? = null, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isUploading.value = true
            val result = saveJournalEntryUseCase(emoji, text, imageUri, imageSource)
            _isUploading.value = false
            if (result.isSuccess) {
                socialRepository.refreshSocialFeed()
                onSuccess()
            }
        }
    }

    fun deleteEntry(entryId: String) {
        viewModelScope.launch {
            journalRepository.deleteJournalEntry(entryId)
        }
    }
}
