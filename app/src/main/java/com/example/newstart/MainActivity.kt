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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.newstart.ui.AuthState
import com.example.newstart.ui.MainViewModel
import com.example.newstart.ui.navigation.MainBottomBar
import com.example.newstart.ui.navigation.NavGraph
import com.example.newstart.ui.navigation.Screen
import com.example.newstart.ui.screens.habits.NewHabitSheet
import com.example.newstart.ui.screens.journal.JournalEntryPanel
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
        
        // Giữ màn hình Splash cho đến khi authState không còn Loading
        splashScreen.setKeepOnScreenCondition {
            mainViewModel.authState.value == AuthState.Loading
        }
        
        enableEdgeToEdge()
        setContent {
            val themeMode by mainViewModel.themeMode.collectAsState()
            val authState by mainViewModel.authState.collectAsState()
            
            NewStartTheme(themeMode = themeMode) {
                val context = LocalContext.current
                
                // Launcher để xin quyền báo thức chính xác
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

                val editingHabit by mainViewModel.editingHabit.collectAsState()
                
                LaunchedEffect(editingHabit) {
                    if (editingHabit != null) {
                        sheetContentType = SheetContent.HabitSelection
                        showBottomSheet = true
                    }
                }

                val isAuthRoute = listOf(Screen.Welcome.route, Screen.Login.route, Screen.Register.route).contains(currentRoute)
                val showShell = authState == AuthState.Authenticated && !isAuthRoute

                // Determine start destination based on Auth State
                val startDestination = remember(authState) {
                    if (authState == AuthState.Authenticated) Screen.Home.route else Screen.Welcome.route
                }

                val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

                if (authState != AuthState.Loading) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        bottomBar = {
                            if (showShell) {
                                MainBottomBar(
                                    navController = navController
                                )
                            }
                        },
                        floatingActionButtonPosition = FabPosition.Center,
                        floatingActionButton = {
                            if (showShell) {
                                FloatingActionButton(
                                    onClick = {
                                        // 1. Kiểm tra quyền báo thức chính xác (Android 12+)
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                                            if (!alarmManager.canScheduleExactAlarms()) {
                                                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                                    data = Uri.fromParts("package", packageName, null)
                                                }
                                                alarmPermissionLauncher.launch(intent)
                                                return@FloatingActionButton
                                            }
                                        }

                                        // 2. Yêu cầu quyền thông báo nếu cần (Android 13+)
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                            if (ContextCompat.checkSelfPermission(
                                                    context,
                                                    Manifest.permission.POST_NOTIFICATIONS
                                                ) != PackageManager.PERMISSION_GRANTED
                                            ) {
                                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                                return@FloatingActionButton
                                            }
                                        }

                                        when (currentRoute) {
                                            Screen.Habits.route -> {
                                                sheetContentType = SheetContent.HabitSelection
                                                showBottomSheet = true
                                            }
                                            else -> {
                                                sheetContentType = SheetContent.JournalEntry
                                                showBottomSheet = true
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .size(56.dp)
                                        .offset(y = 30.dp), // Reduced offset to prevent it from being hidden
                                    shape = CircleShape,
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = Color.White,
                                    elevation = FloatingActionButtonDefaults.elevation(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Add",
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding) // Correctly handle all insets including top (status bar)
                        ) {
                            NavGraph(
                                navController = navController,
                                startDestination = startDestination,
                                mainViewModel = mainViewModel
                            )

                        if (showBottomSheet) {
                            ModalBottomSheet(
                                onDismissRequest = { 
                                    showBottomSheet = false
                                    sheetContentType = SheetContent.None
                                    mainViewModel.startEditingHabit(null)
                                },
                                sheetState = sheetState,
                                dragHandle = { BottomSheetDefaults.DragHandle() },
                                containerColor = MaterialTheme.colorScheme.surface,
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
                            ) {
                                when (sheetContentType) {
                                    SheetContent.JournalEntry -> {
                                        val isUploading by mainViewModel.isUploading.collectAsState()
                                        JournalEntryPanel(
                                            onDismiss = { showBottomSheet = false },
                                            onPost = { emoji, text, uri ->
                                                mainViewModel.saveJournalEntry(emoji, text, uri) {
                                                    showBottomSheet = false
                                                }
                                            },
                                            isUploading = isUploading
                                        )
                                    }
                                    SheetContent.HabitSelection -> {
                                        val selectedHabitDate by mainViewModel.selectedHabitDate.collectAsState()
                                        val editingHabitData by mainViewModel.editingHabit.collectAsState()
                                        NewHabitSheet(
                                            initialDate = selectedHabitDate,
                                            editingHabit = editingHabitData,
                                            onDismiss = { 
                                                showBottomSheet = false
                                                mainViewModel.startEditingHabit(null)
                                            },
                                            onHabitSelected = { name, icon, time, mins, color, date ->
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
                                                    date = date
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
