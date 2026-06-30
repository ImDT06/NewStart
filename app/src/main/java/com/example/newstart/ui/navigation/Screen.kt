package com.example.newstart.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String) {
    object Welcome : Screen("welcome_screen")
    object Login : Screen("login_screen")
    object Register : Screen("register_screen")
    
    // Bottom Nav Screens
    object Home : Screen("home_screen")
    object Journal : Screen("journal_screen")
    object Scan : Screen("scan_screen")
    object Habits : Screen("habits_screen")
    object Statistics : Screen("statistics_screen")
    object Profile : Screen("profile_screen")
    object Social : Screen("social_screen")
    object Pomodoro : Screen("pomodoro_screen")
    object JournalArchive : Screen("journal_archive_screen")

    object Detail : Screen("detail_screen/{userId}") {
        fun createRoute(userId: String) = "detail_screen/$userId"
    }
    
    object Admin : Screen("admin_screen")
}
