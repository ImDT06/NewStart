package com.example.newstart.ui.screens.journal

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.newstart.domain.model.JournalEntry
import com.example.newstart.domain.repository.JournalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
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

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val entries: StateFlow<List<JournalEntry>> = _selectedDate
        .flatMapLatest { date ->
            journalRepository.getJournalEntries().map { allEntries ->
                allEntries.filter { entry ->
                    entry.timestamp?.let { timestamp ->
                        val entryDate = timestamp.toInstant()
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                        entryDate == date
                    } ?: false
                }.sortedByDescending { it.timestamp }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun onDateSelected(date: LocalDate) {
        _selectedDate.value = date
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
