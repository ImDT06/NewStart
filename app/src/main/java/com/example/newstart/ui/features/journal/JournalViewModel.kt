package com.example.newstart.ui.features.journal

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.newstart.domain.model.JournalEntry
import com.example.newstart.domain.repository.JournalRepository
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
    private val journalRepository: JournalRepository
) : ViewModel() {

    private val _selectedDateRange = MutableStateFlow<Pair<LocalDate, LocalDate?>>(LocalDate.now() to null)
    val selectedDateRange: StateFlow<Pair<LocalDate, LocalDate?>> = _selectedDateRange.asStateFlow()

    private val _currentTab = MutableStateFlow(0) // 0: Cá nhân, 1: Cộng đồng
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
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

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
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

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
            "All" -> {
                _selectedDateRange.value = LocalDate.of(2000, 1, 1) to LocalDate.of(2100, 12, 31)
            }
            "Year" -> {
                val start = today.withDayOfYear(1)
                val end = today.withDayOfYear(today.lengthOfYear())
                _selectedDateRange.value = start to end
            }
            "Month" -> {
                val start = today.withDayOfMonth(1)
                val end = today.withDayOfMonth(today.lengthOfMonth())
                _selectedDateRange.value = start to end
            }
            "Week" -> {
                val start = today.with(java.time.DayOfWeek.MONDAY)
                val end = today.with(java.time.DayOfWeek.SUNDAY)
                _selectedDateRange.value = start to end
            }
            "Today" -> {
                _selectedDateRange.value = today to null
            }
        }
    }

    fun addEntry(emoji: String, text: String, imageUri: Uri?, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isUploading.value = true
            val result = journalRepository.saveJournalEntry(emoji, text, imageUri)
            _isUploading.value = false
            if (result.isSuccess) {
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
