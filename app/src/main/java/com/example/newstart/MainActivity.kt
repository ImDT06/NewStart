package com.example.newstart

import android.content.Context
import android.graphics.Rect
import android.os.Bundle
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
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
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.newstart.ui.MainViewModel
import com.example.newstart.ui.AuthState
import com.example.newstart.ui.navigation.MainBottomBar
import com.example.newstart.ui.navigation.NavGraph
import com.example.newstart.ui.navigation.Screen
import com.example.newstart.ui.theme.NewStartTheme
import com.example.newstart.ui.theme.ThemeMode
import com.example.newstart.ui.util.SheetContent
import com.example.newstart.ui.screens.journal.JournalEntryPanel
import dagger.hilt.android.AndroidEntryPoint
import androidx.appcompat.app.AppCompatDelegate

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private val mainViewModel: MainViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by mainViewModel.themeMode.collectAsState()
            val authState by mainViewModel.authState.collectAsState()
            
            NewStartTheme(themeMode = themeMode) {
                if (authState == AuthState.Loading) {
                    // Màn hình trống hoàn toàn trong lúc xác thực để tránh hiện tượng nháy layout
                    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
                } else {
                    val navController = rememberNavController()
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = navBackStackEntry?.destination?.route

                    var showBottomSheet by remember { mutableStateOf(false) }
                    var sheetContentType by remember { mutableStateOf(SheetContent.None) }

                    // FAB và BottomBar chỉ hiện khi đã đăng nhập và không nằm ở các trang Auth
                    val isAuthRoute = listOf(Screen.Welcome.route, Screen.Login.route, Screen.Register.route).contains(currentRoute)
                    val showShell = authState == AuthState.Authenticated && !isAuthRoute

                    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        bottomBar = {
                            if (showShell) {
                                MainBottomBar(navController = navController)
                            }
                        },
                        floatingActionButtonPosition = FabPosition.Center,
                        floatingActionButton = {
                            if (showShell) {
                                Box(modifier = Modifier.offset(y = 60.dp)) {
                                    FloatingActionButton(
                                        onClick = {
                                            // Logic linh hoạt: Mặc định mở Journal, hoặc tùy biến theo route
                                            when (currentRoute) {
                                                Screen.Habits.route -> {
                                                    // TODO: Mở sheet thêm thói quen
                                                    sheetContentType = SheetContent.JournalEntry // Tạm thời
                                                    showBottomSheet = true
                                                }
                                                else -> {
                                                    sheetContentType = SheetContent.JournalEntry
                                                    showBottomSheet = true
                                                }
                                            }
                                        },
                                        modifier = Modifier.size(64.dp),
                                        shape = CircleShape,
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary,
                                        elevation = FloatingActionButtonDefaults.elevation(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Add",
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                }
                            }
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = if (showShell) innerPadding.calculateBottomPadding() else 0.dp)
                        ) {
                            NavGraph(
                                navController = navController,
                                startDestination = if (authState == AuthState.Authenticated) Screen.Home.route else Screen.Welcome.route
                            )

                            if (showBottomSheet) {
                                ModalBottomSheet(
                                    onDismissRequest = { 
                                        showBottomSheet = false
                                        sheetContentType = SheetContent.None
                                    },
                                    sheetState = sheetState,
                                    dragHandle = { BottomSheetDefaults.DragHandle() },
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
                                ) {
                                    when (sheetContentType) {
                                        SheetContent.JournalEntry -> {
                                            JournalEntryPanel(
                                                onDismiss = { showBottomSheet = false },
                                                onPost = { emoji, text, uri ->
                                                    mainViewModel.saveJournalEntry(emoji, text, uri)
                                                    showBottomSheet = false
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
