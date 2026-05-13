package com.example.newstart.ui.navigation

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.newstart.R
import androidx.compose.ui.graphics.vector.ImageVector

data class BottomNavItem(
    val screen: Screen,
    val labelRes: Int,
    val icon: ImageVector
)

@Composable
fun MainBottomBar(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val items = listOf(
        BottomNavItem(Screen.Home, R.string.nav_home, Icons.Default.Home),
        BottomNavItem(Screen.Journal, R.string.nav_journal, Icons.AutoMirrored.Filled.MenuBook),
        BottomNavItem(Screen.Home, 0, Icons.Default.Home), // Placeholder for center FAB
        BottomNavItem(Screen.Habits, R.string.nav_habits, Icons.Default.CheckCircle),
        BottomNavItem(Screen.Profile, R.string.nav_profile, Icons.Default.Person)
    )

    val showBottomBar = items.any { it.screen.route == currentRoute } || currentRoute == Screen.Scan.route

    if (showBottomBar) {
        NavigationBar(
            containerColor = Color.White,
            tonalElevation = 8.dp
        ) {
            items.forEachIndexed { index, item ->
                if (index == 2) {
                    // Empty space for the floating FAB defined in Scaffold
                    NavigationBarItem(
                        selected = false,
                        onClick = { },
                        icon = { Spacer(Modifier.size(24.dp)) },
                        enabled = false
                    )
                } else {
                    NavigationBarItem(
                        selected = currentRoute == item.screen.route,
                        onClick = {
                            if (currentRoute != item.screen.route) {
                                navController.navigate(item.screen.route) {
                                    popUpTo(Screen.Home.route) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = { Icon(item.icon, contentDescription = null) },
                        label = { 
                            Text(
                                text = stringResource(id = item.labelRes),
                                fontSize = 10.sp, 
                                maxLines = 1,
                                softWrap = false
                            ) 
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF1D5FE2),
                            selectedTextColor = Color(0xFF1D5FE2),
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray,
                            indicatorColor = Color.Transparent
                        )
                    )
                }
            }
        }
    }
}
