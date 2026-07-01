package com.example.newstart.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.newstart.data.preferences.UserPreferencesRepository
import com.example.newstart.domain.model.Habit
import com.example.newstart.domain.model.User
import com.example.newstart.domain.repository.AuthRepository
import com.example.newstart.domain.repository.HabitRepository
import com.example.newstart.domain.repository.JournalRepository
import com.example.newstart.domain.repository.UserRepository
import com.example.newstart.ui.theme.AppThemeColor
import com.example.newstart.ui.theme.ThemeMode
import com.example.newstart.domain.model.JournalType
import com.example.newstart.domain.model.JournalPrivacy
import com.example.newstart.domain.model.JournalEntry
import com.example.newstart.domain.model.MovieDetails
import com.example.newstart.domain.model.BookDetails
import com.example.newstart.domain.model.SubjectDetails
import com.example.newstart.domain.usecase.SaveJournalEntryUseCase
import com.example.newstart.domain.usecase.SuggestEmojiUseCase
import com.example.newstart.domain.usecase.SaveHabitUseCase
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withContext
import androidx.compose.runtime.*

enum class AuthState {
    Loading, Authenticated, Unauthenticated
}

@HiltViewModel
class MainViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val authRepository: AuthRepository,
    private val journalRepository: JournalRepository,
    private val userRepository: UserRepository,
    private val habitRepository: HabitRepository,
    private val socialRepository: com.example.newstart.domain.repository.SocialRepository,
    private val database: com.example.newstart.data.local.NewStartDatabase,
    private val firestore: FirebaseFirestore,
    private val saveJournalEntryUseCase: SaveJournalEntryUseCase,
    private val suggestEmojiUseCase: SuggestEmojiUseCase,
    private val saveHabitUseCase: SaveHabitUseCase,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _selectedHabitDate = MutableStateFlow(LocalDate.now())
    val selectedHabitDate: StateFlow<LocalDate> = _selectedHabitDate.asStateFlow()

    private val _editingHabit = MutableStateFlow<Habit?>(null)
    val editingHabit: StateFlow<Habit?> = _editingHabit.asStateFlow()

    private val _showJournalSheet = MutableStateFlow(false)
    val showJournalSheet: StateFlow<Boolean> = _showJournalSheet.asStateFlow()

    private val _isBottomBarVisible = MutableStateFlow(true)
    val isBottomBarVisible: StateFlow<Boolean> = _isBottomBarVisible.asStateFlow()

    fun setBottomBarVisible(visible: Boolean) {
        _isBottomBarVisible.value = visible
    }

    fun onHabitDateSelected(date: LocalDate) {
        _selectedHabitDate.value = date
    }

    fun startEditingHabit(habit: Habit?) {
        _editingHabit.value = habit
    }

    fun setShowJournalSheet(show: Boolean) {
        _showJournalSheet.value = show
    }

    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading.asStateFlow()

    private val _isSavingHabit = MutableStateFlow(false)
    val isSavingHabit: StateFlow<Boolean> = _isSavingHabit.asStateFlow()

    private val _aiSuggestedEmojis = MutableStateFlow<List<String>>(emptyList())
    val aiSuggestedEmojis: StateFlow<List<String>> = _aiSuggestedEmojis.asStateFlow()

    private val _isSuggestingEmojis = MutableStateFlow(false)
    val isSuggestingEmojis: StateFlow<Boolean> = _isSuggestingEmojis.asStateFlow()

    val uniqueMovieTitles: StateFlow<List<String>> = journalRepository.getJournalEntries()
        .map { entries ->
            entries.filter { it.type == JournalType.MOVIE && it.movieDetails != null }
                .map { it.movieDetails!!.title.trim() }
                .filter { it.isNotEmpty() }
                .distinct()
                .sorted()
        }
        .flowOn(kotlinx.coroutines.Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val uniqueBookTitles: StateFlow<List<String>> = journalRepository.getJournalEntries()
        .map { entries ->
            entries.filter { it.type == JournalType.BOOK && it.bookDetails != null }
                .map { it.bookDetails!!.title.trim() }
                .filter { it.isNotEmpty() }
                .distinct()
                .sorted()
        }
        .flowOn(kotlinx.coroutines.Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val uniqueSubjectNames: StateFlow<List<String>> = journalRepository.getJournalEntries()
        .map { entries ->
            entries.filter { it.type == JournalType.SUBJECT && it.subjectDetails != null }
                .map { it.subjectDetails!!.name.trim() }
                .filter { it.isNotEmpty() }
                .distinct()
                .sorted()
        }
        .flowOn(kotlinx.coroutines.Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val uniqueTags: StateFlow<List<String>> = journalRepository.getJournalEntries()
        .map { entries ->
            val tags = mutableSetOf<String>()
            val regex = Regex("#[a-zA-Z0-9_\\-áàảãạâấầẩẫậăắằẳẵặéèẻẽẹêếềểễệíìỉĩịóòỏõọôốồổỗộơớờởỡợúùủũụưứừửữựýỳỷỹỵ]+")
            entries.forEach { entry ->
                regex.findAll(entry.text).forEach { match ->
                    tags.add(match.value.lowercase().trim())
                }
            }
            tags.toList().sorted()
        }
        .flowOn(kotlinx.coroutines.Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var suggestionJob: Job? = null

    fun getEmojiSuggestions(text: String) {
        if (text.isBlank()) {
            _aiSuggestedEmojis.value = emptyList()
            return
        }
        suggestionJob?.cancel()
        suggestionJob = viewModelScope.launch {
            delay(1000) // Debounce 1 giây
            _isSuggestingEmojis.value = true
            val emojis = suggestEmojiUseCase(text)
            _aiSuggestedEmojis.value = emojis
            _isSuggestingEmojis.value = false
        }
    }

    // Lấy thông tin user từ Firestore dựa trên ID của Auth
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val currentUser: StateFlow<User?> = authRepository.currentUser
        .flatMapLatest { firebaseUser ->
            if (firebaseUser == null) flowOf(null)
            else userRepository.getUserById(firebaseUser.id)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val authState: StateFlow<AuthState> = authRepository.currentUser
        .map { user ->
            if (user != null) AuthState.Authenticated else AuthState.Unauthenticated
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AuthState.Loading
        )

    val themeMode: StateFlow<ThemeMode> = userPreferencesRepository.themeModeFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ThemeMode.SYSTEM
        )

    val themeColor: StateFlow<AppThemeColor> = userPreferencesRepository.themeColorFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppThemeColor.BLACK
        )

    val commonPomoTimes: StateFlow<List<Int>> = userPreferencesRepository.commonPomoTimesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = listOf(25, 40, 60, 180)
        )

    val isJournalPromptEnabled: StateFlow<Boolean> = userPreferencesRepository.isJournalPromptEnabledFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    val isSearchable: StateFlow<Boolean> = userPreferencesRepository.isSearchableFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    val isHabitNotificationsEnabled: StateFlow<Boolean> = userPreferencesRepository.isHabitNotificationsEnabledFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    val isCommunityNotificationsEnabled: StateFlow<Boolean> = userPreferencesRepository.isCommunityNotificationsEnabledFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    val avatarUri: StateFlow<Uri?> = currentUser
        .map { user -> 
            user?.avatarUrl?.let { Uri.parse(it) } 
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val squads: StateFlow<List<com.example.newstart.domain.model.Squad>> = socialRepository.getSquads()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val friends: StateFlow<List<com.example.newstart.domain.model.Friendship>> = socialRepository.getFriends()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            userPreferencesRepository.setThemeMode(mode)
        }
    }

    fun setThemeColor(color: AppThemeColor) {
        viewModelScope.launch {
            userPreferencesRepository.setThemeColor(color)
        }
    }

    fun setJournalPromptEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setJournalPromptEnabled(enabled)
        }
    }

    fun setHabitNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setHabitNotificationsEnabled(enabled)
        }
    }

    fun setCommunityNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setCommunityNotificationsEnabled(enabled)
        }
    }

    fun setSearchable(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setSearchable(enabled)
            try {
                userRepository.updateProfileFields(mapOf("searchable" to enabled.toString()))
            } catch (e: Exception) {
                android.util.Log.e("MainViewModel", "Update searchable status failed: ${e.message}")
            }
        }
    }

    fun addCommonPomoTime(minutes: Int) {
        viewModelScope.launch {
            val currentTimes = commonPomoTimes.value.toMutableList()
            if (!currentTimes.contains(minutes)) {
                currentTimes.add(minutes)
                userPreferencesRepository.setCommonPomoTimes(currentTimes.sorted())
            }
        }
    }

    fun removeCommonPomoTime(minutes: Int) {
        viewModelScope.launch {
            val currentTimes = commonPomoTimes.value.toMutableList()
            if (currentTimes.contains(minutes)) {
                currentTimes.remove(minutes)
                userPreferencesRepository.setCommonPomoTimes(currentTimes.sorted())
            }
        }
    }

    fun updateCommonPomoTime(oldMinutes: Int, newMinutes: Int) {
        viewModelScope.launch {
            val currentTimes = commonPomoTimes.value.toMutableList()
            if (currentTimes.contains(oldMinutes)) {
                currentTimes.remove(oldMinutes)
                if (!currentTimes.contains(newMinutes)) {
                    currentTimes.add(newMinutes)
                }
                userPreferencesRepository.setCommonPomoTimes(currentTimes.sorted())
            }
        }
    }

    fun setAvatarUri(uri: Uri?) {
        val userId = currentUser.value?.id ?: return
        if (uri == null) return
        
        viewModelScope.launch {
            _isUploading.value = true
            userRepository.updateAvatar(userId, uri)
            _isUploading.value = false
        }
    }

    fun updateProfileName(newName: String) {
        val userId = currentUser.value?.id ?: return
        viewModelScope.launch {
            userRepository.updateProfile(userId, newName)
        }
    }

    fun updateBirthday(birthday: String) {
        viewModelScope.launch {
            userRepository.updateProfileFields(mapOf("birthday" to birthday))
        }
    }

    fun updateEmail(newEmail: String) {
        viewModelScope.launch {
            userRepository.updateEmail(newEmail)
        }
    }

    fun updateFcmToken(token: String) {
        viewModelScope.launch {
            userRepository.updateFcmToken(token)
        }
    }
    
    fun logout() {
        viewModelScope.launch {
            try {
                // 1. Clear database on IO dispatcher
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    database.clearAllTables()
                }
                
                // 2. Clear Auth
                authRepository.logout()
                
                // Lưu ý: Navigation được xử lý tự động trong MainActivity thông qua authState
            } catch (e: Exception) {
                android.util.Log.e("MainViewModel", "Lỗi khi logout: ${e.message}")
            }
        }
    }

    private var uploadJob: Job? = null

    fun saveJournalEntry(
        emoji: String,
        text: String,
        imageUri: Uri?,
        imageSource: String? = null,
        type: JournalType = JournalType.NORMAL,
        movieDetails: MovieDetails? = null,
        bookDetails: BookDetails? = null,
        subjectDetails: SubjectDetails? = null,
        privacy: JournalPrivacy = JournalPrivacy.FRIENDS,
        onSuccess: () -> Unit
    ) {
        uploadJob?.cancel()
        uploadJob = viewModelScope.launch {
            try {
                _isUploading.value = true
                val result = saveJournalEntryUseCase(
                    emoji, text, imageUri, imageSource, type, movieDetails, bookDetails, subjectDetails, privacy
                )
                if (result.isSuccess) {
                    socialRepository.refreshSocialFeed()
                    onSuccess()
                }
            } finally {
                _isUploading.value = false
            }
        }
    }

    fun cancelUpload() {
        uploadJob?.cancel()
        _isUploading.value = false
    }

    fun saveHabit(
        id: String = "",
        name: String, 
        icon: String, 
        goal: String, 
        colorHex: String, 
        reminderTime: String? = null,
        reminderMinutesBefore: Int = 0,
        date: LocalDate? = null,
        squadId: String? = null,
        onSuccess: () -> Unit
    ) {
        if (_isSavingHabit.value) return
        viewModelScope.launch {
            try {
                _isSavingHabit.value = true
                val finalDate = date ?: _selectedHabitDate.value
                val newHabit = Habit(
                    id = id,
                    name = name,
                    icon = icon,
                    goal = goal,
                    colorHex = colorHex,
                    date = finalDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
                    reminderTime = reminderTime,
                    reminderMinutesBefore = reminderMinutesBefore,
                    squadId = squadId
                )
                val result = saveHabitUseCase(newHabit)
                if (result.isSuccess) {
                    onSuccess()
                } else {
                    android.util.Log.e("MainViewModel", "Lưu thói quen thất bại: ${result.exceptionOrNull()?.message}")
                }
            } finally {
                _isSavingHabit.value = false
            }
        }
    }

    private val _timerSeconds = MutableStateFlow(25 * 60)
    val timerSeconds: StateFlow<Int> = _timerSeconds.asStateFlow()

    private val _isTimerRunning = MutableStateFlow(false)
    val isTimerRunning: StateFlow<Boolean> = _isTimerRunning.asStateFlow()

    private val _focusTime = MutableStateFlow(25)
    val focusTime = _focusTime.asStateFlow()

    private val _breakTime = MutableStateFlow(5)
    val breakTime = _breakTime.asStateFlow()

    private val _isFocusMode = MutableStateFlow(true)
    val isFocusMode = _isFocusMode.asStateFlow()

    private var timerJob: kotlinx.coroutines.Job? = null

    fun setFocusTime(minutes: Int) {
        _focusTime.value = minutes
        if (!_isTimerRunning.value && _isFocusMode.value) {
            _timerSeconds.value = minutes * 60
        }
    }

    fun setBreakTime(minutes: Int) {
        _breakTime.value = minutes
        if (!_isTimerRunning.value && !_isFocusMode.value) {
            _timerSeconds.value = minutes * 60
        }
    }

    fun startTimer() {
        if (_isTimerRunning.value) return
        
        if (_timerSeconds.value <= 0) {
            _timerSeconds.value = (if (_isFocusMode.value) _focusTime.value else _breakTime.value) * 60
        }
        
        _isTimerRunning.value = true
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            try {
                while (_timerSeconds.value > 0) {
                    kotlinx.coroutines.delay(1000)
                    _timerSeconds.value -= 1
                }
                onTimerFinished()
            } catch (e: Exception) {
                // Timer cancelled
            }
        }
    }

    fun pauseTimer() {
        _isTimerRunning.value = false
        timerJob?.cancel()
    }

    fun resetTimer() {
        _isTimerRunning.value = false
        timerJob?.cancel()
        _timerSeconds.value = (if (_isFocusMode.value) _focusTime.value else _breakTime.value) * 60
    }

    private fun onTimerFinished() {
        _isFocusMode.value = !_isFocusMode.value
        resetTimer()
    }

    fun stopTimer() {
        resetTimer()
    }

    // --- Thống kê người dùng (Real-time Stats) ---

    val journalCount: StateFlow<Int> = journalRepository.getJournalEntries()
        .map { it.size }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    val habitStats: StateFlow<Pair<Int, Int>> = habitRepository.getAllHabits()
        .map { habits ->
            if (habits.isEmpty()) 0 to 0
            else {
                val total = habits.size
                val completed = habits.count { it.isCompleted }
                val percent = (completed.toFloat() / total * 100).toInt()
                
                // Tính toán chuỗi ngày (Streak)
                val completedDates = habits.filter { it.isCompleted }
                    .map { it.date }
                    .distinct()
                    .map { LocalDate.parse(it) }
                    .sortedDescending()

                var streak = 0
                if (completedDates.isNotEmpty()) {
                    var current = LocalDate.now()
                    // Nếu hôm nay chưa hoàn thành gì, kiểm tra từ hôm qua
                    if (!completedDates.contains(current)) {
                        current = current.minusDays(1)
                    }
                    
                    for (date in completedDates) {
                        if (date == current) {
                            streak++
                            current = current.minusDays(1)
                        } else if (date.isBefore(current)) {
                            break
                        }
                    }
                }
                percent to streak
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0 to 0
        )

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val isAdmin: StateFlow<Boolean> = authRepository.currentUser
        .flatMapLatest { user ->
            if (user == null) kotlinx.coroutines.flow.flowOf(false)
            else kotlinx.coroutines.flow.flow { emit(authRepository.checkIsAdmin()) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val allUsers: StateFlow<List<User>> = userRepository.getAllUsers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val blockedUsers: StateFlow<Set<String>> = userRepository.getBlockedUsers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    fun blockUser(userId: String, block: Boolean) {
        viewModelScope.launch {
            userRepository.blockUser(userId, block)
        }
    }

    fun adminDeletePost(postId: String) {
        viewModelScope.launch {
            try {
                firestore.collection("journals").document(postId).delete().await()
                socialRepository.refreshSocialFeed()
            } catch (e: Exception) {
                android.util.Log.e("MainViewModel", "Admin delete post failed: ${e.message}")
            }
        }
    }

    val socialFeed: StateFlow<List<JournalEntry>> = socialRepository.getSocialFeed()
        .map { entries ->
            entries.filter { it.privacy != JournalPrivacy.PRIVATE }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val adminSocialFeed: StateFlow<List<JournalEntry>> = callbackFlow {
        val listener = firestore.collection("journals")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("MainViewModel", "Error fetching admin feed: ${error.message}", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val entries = snapshot.documents.mapNotNull { doc ->
                        try {
                            val id = doc.id
                            val userId = doc.getString("userId") ?: ""
                            val emoji = doc.getString("emoji") ?: ""
                            val text = doc.getString("text") ?: ""
                            val imageUrl = doc.getString("imageUrl")
                            val imageSource = doc.getString("imageSource")
                            val privacyStr = doc.getString("privacy") ?: "FRIENDS"
                            val privacy = try { JournalPrivacy.valueOf(privacyStr) } catch(e: Exception) { JournalPrivacy.FRIENDS }
                            
                            val timestampVal = doc.get("timestamp")
                            val timestamp = when (timestampVal) {
                                is com.google.firebase.Timestamp -> timestampVal.toDate()
                                is Long -> java.util.Date(timestampVal)
                                else -> null
                            }
                            
                            val reactions = doc.get("reactions") as? Map<String, String> ?: emptyMap()
                            
                            JournalEntry(
                                id = id,
                                userId = userId,
                                emoji = emoji,
                                text = text,
                                imageUrl = imageUrl,
                                imageSource = imageSource,
                                privacy = privacy,
                                reactions = reactions,
                                timestamp = timestamp
                            )
                        } catch (e: Exception) {
                            android.util.Log.e("MainViewModel", "Error mapping admin journal: ${e.message}")
                            null
                        }
                    }
                    trySend(entries)
                }
            }
        awaitClose { listener.remove() }
    }
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun getUserById(userId: String) = userRepository.getUserById(userId)

    var pendingChatUserId by mutableStateOf<String?>(null)
    var pendingSharedJournal by mutableStateOf<JournalEntry?>(null)

    fun getDirectMessages(friendshipId: String): Flow<List<com.example.newstart.domain.model.DirectMessage>> {
        return socialRepository.getDirectMessages(friendshipId)
    }

    fun sendDirectMessage(friendshipId: String, text: String, sharedJournal: JournalEntry? = null) {
        viewModelScope.launch {
            val result = socialRepository.sendDirectMessage(friendshipId, text, sharedJournal)
            result.onFailure { e ->
                android.util.Log.e("MainViewModel", "Failed to send direct message: ${e.message}", e)
                withContext(kotlinx.coroutines.Dispatchers.Main) {
                    android.widget.Toast.makeText(
                        context,
                        "Lỗi gửi tin: ${e.localizedMessage}",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    fun getLastMessage(friendshipId: String): Flow<com.example.newstart.domain.model.DirectMessage?> {
        return socialRepository.getLastMessage(friendshipId)
    }
}
