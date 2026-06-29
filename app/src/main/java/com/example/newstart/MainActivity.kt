package com.example.newstart

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.newstart.ui.AuthState
import com.example.newstart.ui.MainViewModel
import com.example.newstart.ui.navigation.MainBottomBar
import com.example.newstart.ui.navigation.NavGraph
import com.example.newstart.ui.navigation.Screen
import com.example.newstart.ui.features.habits.components.NewHabitSheet
import com.example.newstart.ui.features.journal.components.JournalEntryPanel
import com.example.newstart.ui.theme.NewStartTheme
import com.example.newstart.ui.util.SheetContent
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private val mainViewModel: MainViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        
        splashScreen.setKeepOnScreenCondition {
            mainViewModel.authState.value == AuthState.Loading
        }
        
        enableEdgeToEdge()
        setContent {
            val themeMode by mainViewModel.themeMode.collectAsStateWithLifecycle()
            val themeColor by mainViewModel.themeColor.collectAsStateWithLifecycle()
            val authState by mainViewModel.authState.collectAsStateWithLifecycle()
            
            NewStartTheme(themeMode = themeMode, themeColor = themeColor) {
                val context = LocalContext.current
                
                val alarmPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult()
                ) { _ -> }

                val notificationPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { _ -> }

                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                var showBottomSheet by remember { mutableStateOf(false) }
                var sheetContentType by remember { mutableStateOf(SheetContent.None) }

                val editingHabit by mainViewModel.editingHabit.collectAsStateWithLifecycle()
                val showJournalSheetState by mainViewModel.showJournalSheet.collectAsStateWithLifecycle()
                val isUploading by mainViewModel.isUploading.collectAsStateWithLifecycle()
                
                LaunchedEffect(editingHabit) {
                    if (editingHabit != null) {
                        sheetContentType = SheetContent.HabitSelection
                        showBottomSheet = true
                    }
                }

                LaunchedEffect(showJournalSheetState) {
                    if (showJournalSheetState) {
                        sheetContentType = SheetContent.JournalEntry
                        showBottomSheet = true
                    }
                }

                val isAuthRoute = listOf(Screen.Welcome.route, Screen.Login.route, Screen.Register.route, Screen.Pomodoro.route).contains(currentRoute)
                
                // Xác định các trang chính có hiển thị Bottom Bar
                val isMainRoute = listOf(
                    Screen.Home.route,
                    Screen.Journal.route,
                    Screen.Habits.route,
                    Screen.Profile.route,
                    Screen.Social.route
                ).contains(currentRoute)

                val showShell = authState == AuthState.Authenticated && !isAuthRoute
                val showFab = showShell && isMainRoute

                // Tối ưu hóa Navbar nổi: Hide on Scroll
                val isBottomBarVisible by mainViewModel.isBottomBarVisible.collectAsState()
                
                // Tự động hiện lại thanh điều hướng khi chuyển trang
                LaunchedEffect(currentRoute) {
                    mainViewModel.setBottomBarVisible(true)
                }

                val nestedScrollConnection = remember(currentRoute) {
                    object : NestedScrollConnection {
                        override fun onPostScroll(
                            consumed: Offset,
                            available: Offset,
                            source: NestedScrollSource
                        ): Offset {
                            // Chỉ xử lý ẩn/hiện nếu trang thực sự có thể cuộn (consumed.y != 0)
                            if (consumed.y < -5f) {
                                mainViewModel.setBottomBarVisible(false)
                            } else if (consumed.y > 5f) {
                                mainViewModel.setBottomBarVisible(true)
                            }
                            return super.onPostScroll(consumed, available, source)
                        }
                    }
                }

                LaunchedEffect(authState, currentRoute) {
                    when (authState) {
                        AuthState.Authenticated -> {
                            val onAuthRoute = currentRoute == null || 
                                            currentRoute == Screen.Welcome.route || 
                                            currentRoute == Screen.Login.route || 
                                            currentRoute == Screen.Register.route
                            
                            if (onAuthRoute) {
                                navController.navigate(Screen.Home.route) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        }
                        AuthState.Unauthenticated -> {
                            val onAppRoute = currentRoute != null && 
                                           currentRoute != Screen.Welcome.route && 
                                           currentRoute != Screen.Login.route && 
                                           currentRoute != Screen.Register.route
                            
                            if (onAppRoute) {
                                navController.navigate(Screen.Welcome.route) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        }
                        else -> {}
                    }
                }

                var isJournalDirty by remember { mutableStateOf(false) }
                
                LaunchedEffect(showBottomSheet) {
                    if (!showBottomSheet) isJournalDirty = false
                }

                val sheetState = rememberModalBottomSheetState(
                    skipPartiallyExpanded = true,
                    confirmValueChange = { !isUploading && !isJournalDirty }
                )

                if (authState != AuthState.Loading) {
                    Scaffold(
                        modifier = Modifier
                            .fillMaxSize()
                            .nestedScroll(nestedScrollConnection),
                        containerColor = MaterialTheme.colorScheme.background,
                        contentColor = MaterialTheme.colorScheme.onBackground
                    ) { _ ->
                        Box(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            NavGraph(
                                navController = navController,
                                startDestination = Screen.Welcome.route,
                                mainViewModel = mainViewModel,
                                modifier = Modifier.fillMaxSize()
                            )

                            if (showShell) {
                                var showMenu by remember { mutableStateOf(false) }
                                val density = LocalDensity.current

                                // Lớp phủ mờ nền khi menu mở
                                AnimatedVisibility(
                                    visible = showMenu,
                                    enter = fadeIn(),
                                    exit = fadeOut()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.4f))
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null
                                            ) {
                                                showMenu = false
                                            }
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxSize(),
                                    contentAlignment = Alignment.BottomCenter
                                ) {
                                    MainBottomBar(
                                        navController = navController,
                                        isVisible = isBottomBarVisible
                                    )

                                    val rotation by animateFloatAsState(
                                        targetValue = if (showMenu) 45f else 0f,
                                        label = "fab_rotation"
                                    )

                                    // Hiệu ứng ẩn hiện cả cụm Menu + FAB khi scroll
                                    AnimatedVisibility(
                                        visible = (isBottomBarVisible && showFab) || showMenu, // Luôn hiện nếu menu đang mở
                                        enter = scaleIn() + fadeIn(),
                                        exit = scaleOut() + fadeOut()
                                    ) {
                                        Box(
                                            modifier = Modifier.fillMaxWidth(),
                                            contentAlignment = Alignment.BottomCenter
                                        ) {
                                            // Menu options (Fan out effect) - Wrapped in single AnimatedVisibility for synchronization
                                            AnimatedVisibility(
                                                visible = showMenu,
                                                enter = fadeIn(tween(200)),
                                                exit = fadeOut(tween(150))
                                            ) {
                                                Box(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    contentAlignment = Alignment.BottomCenter
                                                ) {
                                                    // Left: Camera (Journal)
                                                    FloatingActionButton(
                                                        onClick = {
                                                            showMenu = false
                                                            if (currentRoute != Screen.Journal.route) {
                                                                navController.navigate(Screen.Journal.route) {
                                                                    popUpTo(Screen.Home.route) { saveState = true }
                                                                    launchSingleTop = true
                                                                    restoreState = true
                                                                }
                                                            }
                                                            sheetContentType = SheetContent.JournalEntry
                                                            showBottomSheet = true
                                                        },
                                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                                        shape = CircleShape,
                                                        modifier = Modifier
                                                            .padding(bottom = 70.dp)
                                                            .offset(x = (-80).dp)
                                                            .size(48.dp)
                                                            .animateEnterExit(
                                                                enter = scaleIn(initialScale = 0f, animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy)) + 
                                                                        slideIn(initialOffset = { with(density) { IntOffset(80.dp.roundToPx(), 70.dp.roundToPx()) } }, animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy)),
                                                                exit = scaleOut(targetScale = 0f) + 
                                                                        slideOut(targetOffset = { with(density) { IntOffset(80.dp.roundToPx(), 70.dp.roundToPx()) } })
                                                            )
                                                    ) {
                                                        Icon(Icons.Default.PhotoCamera, "Journal", modifier = Modifier.size(24.dp))
                                                    }

                                                    // Middle: Pomodoro
                                                    FloatingActionButton(
                                                        onClick = {
                                                            showMenu = false
                                                            navController.navigate(Screen.Pomodoro.route)
                                                        },
                                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                                        shape = CircleShape,
                                                        modifier = Modifier
                                                            .padding(bottom = 100.dp)
                                                            .size(56.dp)
                                                            .animateEnterExit(
                                                                enter = scaleIn(initialScale = 0f, animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy)) + 
                                                                        slideIn(initialOffset = { with(density) { IntOffset(0, 100.dp.roundToPx()) } }, animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy)),
                                                                exit = scaleOut(targetScale = 0f) + 
                                                                        slideOut(targetOffset = { with(density) { IntOffset(0, 100.dp.roundToPx()) } })
                                                            )
                                                    ) {
                                                        Icon(Icons.Default.Timer, "Pomodoro", modifier = Modifier.size(28.dp))
                                                    }

                                                    // Right: Habit
                                                    FloatingActionButton(
                                                        onClick = {
                                                            showMenu = false
                                                            if (currentRoute != Screen.Habits.route) {
                                                                navController.navigate(Screen.Habits.route) {
                                                                    popUpTo(Screen.Home.route) { saveState = true }
                                                                    launchSingleTop = true
                                                                    restoreState = true
                                                                }
                                                            }
                                                            sheetContentType = SheetContent.HabitSelection
                                                            showBottomSheet = true
                                                        },
                                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                                        shape = CircleShape,
                                                        modifier = Modifier
                                                            .padding(bottom = 70.dp)
                                                            .offset(x = 80.dp)
                                                            .size(48.dp)
                                                            .animateEnterExit(
                                                                enter = scaleIn(initialScale = 0f, animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy)) + 
                                                                        slideIn(initialOffset = { with(density) { IntOffset((-80).dp.roundToPx(), 70.dp.roundToPx()) } }, animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy)),
                                                                exit = scaleOut(targetScale = 0f) + 
                                                                        slideOut(targetOffset = { with(density) { IntOffset((-80).dp.roundToPx(), 70.dp.roundToPx()) } })
                                                            )
                                                    ) {
                                                        Icon(Icons.Default.AddCircle, "Habit", modifier = Modifier.size(24.dp))
                                                    }
                                                }
                                            }

                                            FloatingActionButton(
                                                onClick = {
                                                    showMenu = !showMenu
                                                },
                                                modifier = Modifier
                                                    .windowInsetsPadding(WindowInsets.navigationBars)
                                                    .padding(bottom = 8.dp) // Căn chỉnh giữa bar 64dp
                                                    .size(width = 56.dp, height = 48.dp),
                                                shape = RoundedCornerShape(16.dp),
                                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
                                            )
{
                                                Icon(
                                                    imageVector = Icons.Default.Add,
                                                    contentDescription = "Add",
                                                    modifier = Modifier
                                                        .size(28.dp)
                                                        .rotate(rotation)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            if (showBottomSheet) {
                                ModalBottomSheet(
                                    onDismissRequest = { 
                                        if (!isUploading) {
                                            showBottomSheet = false
                                            sheetContentType = SheetContent.None
                                            mainViewModel.startEditingHabit(null)
                                            mainViewModel.setShowJournalSheet(false)
                                        }
                                    },
                                    sheetState = sheetState,
                                    dragHandle = { BottomSheetDefaults.DragHandle() },
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                                    contentWindowInsets = { WindowInsets(0, 0, 0, 0) }
                                ) {
                                    when (sheetContentType) {
                                        SheetContent.JournalEntry -> {
                                            val suggestedEmojis by mainViewModel.aiSuggestedEmojis.collectAsStateWithLifecycle()
                                            val isSuggesting by mainViewModel.isSuggestingEmojis.collectAsStateWithLifecycle()
                                            val suggestedMovies by mainViewModel.uniqueMovieTitles.collectAsStateWithLifecycle()
                                            val suggestedBooks by mainViewModel.uniqueBookTitles.collectAsStateWithLifecycle()
                                            val suggestedSubjects by mainViewModel.uniqueSubjectNames.collectAsStateWithLifecycle()
                                            val suggestedTags by mainViewModel.uniqueTags.collectAsStateWithLifecycle()
                                            JournalEntryPanel(
                                                onDismiss = { 
                                                    showBottomSheet = false
                                                    mainViewModel.setShowJournalSheet(false)
                                                },
                                                onPost = { emoji, text, uri, source, type, movie, book, subject, privacy ->
                                                    mainViewModel.saveJournalEntry(
                                                        emoji, text, uri, source, type, movie, book, subject, privacy
                                                    ) {
                                                        showBottomSheet = false
                                                        mainViewModel.setShowJournalSheet(false)
                                                    }
                                                },
                                                isUploading = isUploading,
                                                suggestedEmojis = suggestedEmojis,
                                                isSuggesting = isSuggesting,
                                                suggestedMovieTitles = suggestedMovies,
                                                suggestedBookTitles = suggestedBooks,
                                                suggestedSubjectNames = suggestedSubjects,
                                                suggestedTags = suggestedTags,
                                                onTextChanged = { mainViewModel.getEmojiSuggestions(it) },
                                                onCancelUpload = { mainViewModel.cancelUpload() },
                                                onDirtyStateChanged = { isJournalDirty = it }
                                            )
                                        }
                                        SheetContent.HabitSelection -> {
                                            val selectedHabitDate by mainViewModel.selectedHabitDate.collectAsStateWithLifecycle()
                                            val editingHabitData by mainViewModel.editingHabit.collectAsStateWithLifecycle()
                                            val squads by mainViewModel.squads.collectAsStateWithLifecycle()
                                            
                                            NewHabitSheet(
                                                initialDate = selectedHabitDate,
                                                editingHabit = editingHabitData,
                                                squads = squads,
                                                onDismiss = { 
                                                    showBottomSheet = false
                                                    mainViewModel.startEditingHabit(null)
                                                },
                                                onHabitSelected = { name, icon, time, mins, color, date, squadId ->
                                                    val colorInt = (color.red * 255).toInt() shl 16 or
                                                                  (color.green * 255).toInt() shl 8 or
                                                                  (color.blue * 255).toInt()
                                                    val colorHex = String.format("#%06X", colorInt)

                                                    mainViewModel.saveHabit(
                                                        id = editingHabitData?.id ?: "",
                                                        name = name,
                                                        icon = icon,
                                                        goal = "1",
                                                        colorHex = colorHex,
                                                        reminderTime = time,
                                                        reminderMinutesBefore = mins,
                                                        date = date,
                                                        squadId = squadId
                                                    ) {
                                                        showBottomSheet = false
                                                        mainViewModel.startEditingHabit(null)
                                                    }
                                                }
                                            )
                                        }
                                        else -> Box(Modifier.size(1.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val v = currentFocus
            if (v != null) {
                val outRect = Rect()
                v.getGlobalVisibleRect(outRect)
                if (!outRect.contains(event.rawX.toInt(), event.rawY.toInt())) {
                    v.clearFocus()
                    val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(v.windowToken, 0)
                }
            }
        }
        return super.dispatchTouchEvent(event)
    }
}
