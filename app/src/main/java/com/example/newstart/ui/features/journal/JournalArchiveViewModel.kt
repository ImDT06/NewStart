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

data class TagGroup(
    val tag: String,
    val entries: List<JournalEntry>
)

data class MoodAnalytics(
    val moodCounts: Map<String, Int>,
    val moodPercentages: Map<String, Float>,
    val tagMoodCorrelations: List<TagMoodCorrelation>
)

data class TagMoodCorrelation(
    val tag: String,
    val averageMood: Float,
    val entryCount: Int
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
            entries.filter { it.type == JournalType.MOVIE && it.movieDetails != null && it.movieDetails.title.trim().isNotEmpty() }
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
            entries.filter { it.type == JournalType.BOOK && it.bookDetails != null && it.bookDetails.title.trim().isNotEmpty() }
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
            entries.filter { it.type == JournalType.SUBJECT && it.subjectDetails != null && it.subjectDetails.name.trim().isNotEmpty() }
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

    val tagGroups: StateFlow<List<TagGroup>> = allEntries
        .map { entries ->
            val groups = mutableMapOf<String, MutableList<JournalEntry>>()
            entries.forEach { entry ->
                val hashtags = extractHashtags(entry.text)
                hashtags.forEach { tag ->
                    groups.getOrPut(tag) { mutableListOf() }.add(entry)
                }
            }
            groups.map { (tag, groupEntries) ->
                TagGroup(
                    tag = tag,
                    entries = groupEntries.sortedByDescending { it.timestamp }
                )
            }.sortedBy { it.tag }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val moodAnalytics: StateFlow<MoodAnalytics?> = allEntries
        .map { entries ->
            if (entries.isEmpty()) return@map null

            val moodIcons = listOf("😫", "😔", "😐", "😊", "🥰")
            val counts = moodIcons.associateWith { emoji ->
                entries.count { it.emoji == emoji }
            }
            val totalWithMood = counts.values.sum()
            val percentages = counts.mapValues { (_, count) ->
                if (totalWithMood > 0) count.toFloat() / totalWithMood else 0f
            }

            val tagMoodMap = mutableMapOf<String, MutableList<Float>>()
            val moodValues = mapOf("😫" to 1f, "😔" to 2f, "😐" to 3f, "😊" to 4f, "🥰" to 5f)

            entries.forEach { entry ->
                val moodValue = moodValues[entry.emoji] ?: return@forEach
                val hashtags = extractHashtags(entry.text)
                hashtags.forEach { tag ->
                    tagMoodMap.getOrPut(tag) { mutableListOf() }.add(moodValue)
                }
            }

            val correlations = tagMoodMap.map { (tag, moods) ->
                TagMoodCorrelation(
                    tag = tag,
                    averageMood = moods.average().toFloat(),
                    entryCount = moods.size
                )
            }.sortedByDescending { it.averageMood }

            MoodAnalytics(
                moodCounts = counts,
                moodPercentages = percentages,
                tagMoodCorrelations = correlations
            )
        }
        .flowOn(kotlinx.coroutines.Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    private fun extractHashtags(text: String): List<String> {
        val regex = Regex("#[a-zA-Z0-9_\\-áàảãạâấầẩẫậăắằẳẵặéèẻẽẹêếềểễệíìỉĩịóòỏõọôốồổỗộơớờởỡợúùủũụưứừửữựýỳỷỹỵ]+")
        return regex.findAll(text).map { it.value.lowercase().trim() }.distinct().toList()
    }

    fun selectTab(index: Int) {
        _selectedTab.value = index
    }
}
