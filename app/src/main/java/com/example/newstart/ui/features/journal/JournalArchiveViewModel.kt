package com.example.newstart.ui.features.journal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.newstart.domain.model.JournalEntry
import com.example.newstart.domain.model.JournalType
import com.example.newstart.domain.repository.JournalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class MovieGroup(
    val title: String,
    val averageRating: Float,
    val entries: List<JournalEntry>
)

data class BookGroup(
    val title: String,
    val averageRating: Float,
    val entries: List<JournalEntry>
)

data class SubjectGroup(
    val name: String,
    val averageUnderstanding: Float,
    val entries: List<JournalEntry>
)

@HiltViewModel
class JournalArchiveViewModel @Inject constructor(
    private val journalRepository: JournalRepository
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(0) // 0: Movie, 1: Book, 2: Subject
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val allEntries: Flow<List<JournalEntry>> = journalRepository.getJournalEntries()
        .flowOn(Dispatchers.IO)

    val movieGroups: StateFlow<List<MovieGroup>> = allEntries
        .map { entries ->
            entries.filter { it.type == JournalType.MOVIE && it.movieDetails != null }
                .groupBy { it.movieDetails!!.title.trim().lowercase() }
                .map { (_, groupEntries) ->
                    val displayTitle = groupEntries.first().movieDetails!!.title
                    val ratings = groupEntries.map { it.movieDetails!!.rating }.filter { it > 0f }
                    val avgRating = if (ratings.isNotEmpty()) ratings.average().toFloat() else 0f
                    MovieGroup(
                        title = displayTitle,
                        averageRating = avgRating,
                        entries = groupEntries.sortedByDescending { it.timestamp }
                    )
                }
                .sortedByDescending { it.entries.firstOrNull()?.timestamp }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val bookGroups: StateFlow<List<BookGroup>> = allEntries
        .map { entries ->
            entries.filter { it.type == JournalType.BOOK && it.bookDetails != null }
                .groupBy { it.bookDetails!!.title.trim().lowercase() }
                .map { (_, groupEntries) ->
                    val displayTitle = groupEntries.first().bookDetails!!.title
                    val ratings = groupEntries.map { it.bookDetails!!.rating }.filter { it > 0f }
                    val avgRating = if (ratings.isNotEmpty()) ratings.average().toFloat() else 0f
                    BookGroup(
                        title = displayTitle,
                        averageRating = avgRating,
                        entries = groupEntries.sortedByDescending { it.timestamp }
                    )
                }
                .sortedByDescending { it.entries.firstOrNull()?.timestamp }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val subjectGroups: StateFlow<List<SubjectGroup>> = allEntries
        .map { entries ->
            entries.filter { it.type == JournalType.SUBJECT && it.subjectDetails != null }
                .groupBy { it.subjectDetails!!.name.trim().lowercase() }
                .map { (_, groupEntries) ->
                    val displayName = groupEntries.first().subjectDetails!!.name
                    val levels = groupEntries.map { it.subjectDetails!!.understandingLevel }
                    val avgLevel = if (levels.isNotEmpty()) levels.average().toFloat() else 3f
                    SubjectGroup(
                        name = displayName,
                        averageUnderstanding = avgLevel,
                        entries = groupEntries.sortedByDescending { it.timestamp }
                    )
                }
                .sortedByDescending { it.entries.firstOrNull()?.timestamp }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun selectTab(index: Int) {
        _selectedTab.value = index
    }
}
