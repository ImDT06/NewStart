package com.example.newstart.ui.features.pomodoro

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.newstart.ui.MainViewModel
import com.commandiron.wheel_picker_compose.core.WheelTextPicker
import com.commandiron.wheel_picker_compose.core.WheelPickerDefaults
import androidx.compose.ui.window.Dialog
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun PomodoroScreen(
    onNavigateBack: () -> Unit,
    mainViewModel: MainViewModel
) {
    val timeLeft by mainViewModel.timerSeconds.collectAsStateWithLifecycle()
    val isRunning by mainViewModel.isTimerRunning.collectAsStateWithLifecycle()
    val isFocusMode by mainViewModel.isFocusMode.collectAsStateWithLifecycle()
    val focusTime by mainViewModel.focusTime.collectAsStateWithLifecycle()
    val breakTime by mainViewModel.breakTime.collectAsStateWithLifecycle()
    val commonTimes by mainViewModel.commonPomoTimes.collectAsStateWithLifecycle()

    var showQuickMenu by remember { mutableStateOf(false) }
    var showScrollerPopup by remember { mutableStateOf(false) }
    var scrollerStartingTime by remember { mutableIntStateOf(focusTime) }
    var isEditingExisting by remember { mutableStateOf(false) }
    var isEditingTime by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val locale = remember(context) { context.resources.configuration.locales[0] }
    val isVietnamese = locale.language == "vi"

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (isVietnamese) "Pomodoro" else "Pomodoro", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                Spacer(modifier = Modifier.height(40.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 60.dp)
                ) {
                    Text(
                        text = if (isFocusMode) (if (isVietnamese) "Tập trung" else "Focus") else (if (isVietnamese) "Nghỉ ngơi" else "Break"),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp).padding(start = 4.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }

                // Timer Display
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(240.dp)
                        .combinedClickable(
                            enabled = !isRunning,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { 
                                if (isEditingTime) isEditingTime = false 
                                else showQuickMenu = true 
                            },
                            onLongClick = { isEditingTime = true }
                        )
                ) {
                    CircularProgressIndicator(
                        progress = { 
                            val total = (if (isFocusMode) focusTime else breakTime) * 60
                            if (total > 0) 1f - (timeLeft.toFloat() / total) else 0f
                        },
                        modifier = Modifier.fillMaxSize(),
                        strokeWidth = 2.dp,
                        color = if (isFocusMode) MaterialTheme.colorScheme.primary else Color(0xFF4CAF50),
                        trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    )
                    
                    Crossfade(
                        targetState = isEditingTime,
                        animationSpec = tween(durationMillis = 400),
                        label = "timer_display_fade"
                    ) { editing ->
                        if (editing) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                val timeValues = remember { (5..180).toList() }
                                val timeTexts = remember { timeValues.map { it.toString() } }
                                val initialIndex = remember { (focusTime - 5).coerceAtLeast(0) }
                                
                                WheelTextPicker(
                                    startIndex = initialIndex,
                                    texts = timeTexts,
                                    onScrollFinished = { snappedIndex ->
                                        if (snappedIndex in timeValues.indices) {
                                            mainViewModel.setFocusTime(timeValues[snappedIndex])
                                        }
                                        null
                                    },
                                    selectorProperties = WheelPickerDefaults.selectorProperties(enabled = false),
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 32.sp
                                    ),
                                    rowCount = 3,
                                    modifier = Modifier.width(80.dp)
                                )
                                
                                Text(
                                    text = if (isVietnamese) "Phút" else "Mins",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier
                                        .padding(start = 100.dp)
                                        .align(Alignment.Center)
                                )
                            }
                        } else {
                            val mins = timeLeft / 60
                            val secs = timeLeft % 60
                            
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Text(
                                    text = String.format(Locale.getDefault(), "%02d:%02d", mins, secs),
                                    fontSize = 54.sp,
                                    fontWeight = FontWeight.Light,
                                    modifier = Modifier.align(Alignment.Center)
                                )
                                
                                if (!isRunning && timeLeft > 0 && timeLeft < (if (isFocusMode) focusTime else breakTime) * 60) {
                                    Text(
                                        text = if (isVietnamese) "Đã tạm dừng" else "Paused",
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                        modifier = Modifier
                                            .align(Alignment.Center)
                                            .padding(top = 70.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(80.dp))

                // Controls
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        val hasStarted = timeLeft < (if (isFocusMode) focusTime else breakTime) * 60
                        
                        // Left: Sound (Placeholder)
                        AnimatedVisibility(
                            visible = hasStarted,
                            enter = fadeIn() + scaleIn(),
                            exit = fadeOut() + scaleOut()
                        ) {
                            IconButton(onClick = { /* TODO: Sound settings */ }) {
                                Icon(Icons.Default.MusicNote, "Sound", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            }
                        }

                        Spacer(modifier = Modifier.width(if (hasStarted) 16.dp else 0.dp))

                        // Center: Start/Pause
                        val buttonWidth by animateDpAsState(
                            targetValue = if (hasStarted) 64.dp else 140.dp,
                            label = "button_width"
                        )

                        Button(
                            onClick = { 
                                if (isRunning) mainViewModel.pauseTimer() else mainViewModel.startTimer()
                            },
                            modifier = Modifier.size(width = buttonWidth, height = 64.dp),
                            shape = CircleShape,
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            if (hasStarted) {
                                Icon(
                                    imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Pause",
                                    modifier = Modifier.size(32.dp)
                                )
                            } else {
                                Text(if (isVietnamese) "Bắt đầu" else "Start", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }

                        Spacer(modifier = Modifier.width(if (hasStarted) 16.dp else 0.dp))

                        // Right: Stop (Reset to initial)
                        AnimatedVisibility(
                            visible = hasStarted,
                            enter = fadeIn() + scaleIn(),
                            exit = fadeOut() + scaleOut()
                        ) {
                            IconButton(onClick = { mainViewModel.stopTimer() }) {
                                Icon(Icons.Default.Stop, "Stop", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
                            }
                        }
                    }
                }
            }

            if (showQuickMenu) {
                QuickTimeMenu(
                    commonTimes = commonTimes,
                    onDismiss = { showQuickMenu = false },
                    onTimeSelect = {
                        mainViewModel.setFocusTime(it)
                        showQuickMenu = false
                    },
                    onOpenScroller = { initialTime, isEdit ->
                        if (!isEdit && commonTimes.size >= 8) {
                            Toast.makeText(context, "Rất tiếc, số lượng Pomo được sử dụng thường xuyên đã đạt đến giới hạn trên.", Toast.LENGTH_SHORT).show()
                        } else {
                            scrollerStartingTime = initialTime
                            isEditingExisting = isEdit
                            // Không đặt showQuickMenu = false để giữ nó ở phía sau
                            showScrollerPopup = true
                        }
                    }
                )
            }

            if (showScrollerPopup) {
                ScrollerTimePopup(
                    initialMinutes = scrollerStartingTime,
                    isEditMode = isEditingExisting,
                    onDismiss = { 
                        showScrollerPopup = false
                    },
                    onConfirm = { newTime ->
                        if (isEditingExisting) {
                            if (newTime == scrollerStartingTime) {
                                showScrollerPopup = false
                            } else if (commonTimes.contains(newTime)) {
                                Toast.makeText(context, "Bạn đã đặt Pomo thường dùng này.", Toast.LENGTH_SHORT).show()
                            } else {
                                mainViewModel.updateCommonPomoTime(scrollerStartingTime, newTime)
                                showScrollerPopup = false
                            }
                        } else {
                            if (commonTimes.contains(newTime)) {
                                Toast.makeText(context, "Bạn đã đặt Pomo thường dùng này.", Toast.LENGTH_SHORT).show()
                            } else {
                                mainViewModel.addCommonPomoTime(newTime)
                                showScrollerPopup = false
                            }
                        }
                    },
                    onDelete = {
                        mainViewModel.removeCommonPomoTime(scrollerStartingTime)
                        showScrollerPopup = false
                    }
                )
            }
        }
    }
}

@Composable
fun QuickTimeMenu(
    commonTimes: List<Int>,
    onDismiss: () -> Unit,
    onTimeSelect: (Int) -> Unit,
    onOpenScroller: (Int, Boolean) -> Unit
) {
    val locale = androidx.compose.ui.platform.LocalContext.current.resources.configuration.locales[0]
    val isVietnamese = locale.language == "vi"
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Hiển thị danh sách theo lưới 2 cột
                    commonTimes.chunked(2).forEach { rowItems ->
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            rowItems.forEach { time ->
                                TimeMenuItem(time, Modifier.weight(1f), 
                                    onClick = { onTimeSelect(time) },
                                    onLongClick = { onOpenScroller(time, true) }
                                )
                            }
                            if (rowItems.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Surface(
                            modifier = Modifier
                                .weight(0.5f)
                                .height(56.dp)
                                .clickable { onOpenScroller(25, false) },
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Spacer(modifier = Modifier.weight(0.5f))
                    }
                }
                
                Text(
                    text = if (isVietnamese) "Nhấn và giữ thời lượng để thay đổi" else "Long press to edit duration",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 20.dp)
                )
            }
        },
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TimeMenuItem(
    mins: Int, 
    modifier: Modifier = Modifier, 
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(56.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = String.format(Locale.getDefault(), "%02d:00", mins),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ScrollerTimePopup(
    initialMinutes: Int,
    isEditMode: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
    onDelete: () -> Unit
) {
    var selectedMinutes by remember { mutableIntStateOf(initialMinutes) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .width(280.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val locale = androidx.compose.ui.platform.LocalContext.current.resources.configuration.locales[0]
                val isVietnamese = locale.language == "vi"
                Text(
                    if (isVietnamese) "Pomo thường dùng của bạn" else "Your common Pomos",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                
                Box(
                    modifier = Modifier.fillMaxWidth().height(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val timeValues = remember { (5..180).toList() }
                    val timeTexts = remember { timeValues.map { it.toString() } }
                    val initialIndex = remember(initialMinutes) { (initialMinutes - 5).coerceAtLeast(0) }
                    
                    WheelTextPicker(
                        startIndex = initialIndex,
                        texts = timeTexts,
                        onScrollFinished = { snappedIndex ->
                            // Hiệu chỉnh riêng cho Dialog vì thư viện trả về lệch +1 ở đây
                            val correctedIndex = (snappedIndex - 1).coerceIn(0, timeValues.lastIndex)
                            selectedMinutes = timeValues[correctedIndex]
                            null
                        },
                        selectorProperties = WheelPickerDefaults.selectorProperties(enabled = false),
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 36.sp
                        ),
                        rowCount = 3,
                        modifier = Modifier.width(100.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    val locale = androidx.compose.ui.platform.LocalContext.current.resources.configuration.locales[0]
                    val isVietnamese = locale.language == "vi"
                    TextButton(onClick = if (isEditMode) onDelete else onDismiss) {
                        Text(
                            if (isEditMode) (if (isVietnamese) "Loại bỏ" else "Remove") else (if (isVietnamese) "Hủy bỏ" else "Cancel"),
                            color = if (isEditMode) Color.Red else MaterialTheme.colorScheme.primary
                        )
                    }
                    TextButton(onClick = { onConfirm(selectedMinutes) }) {
                        Text("OK", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}
