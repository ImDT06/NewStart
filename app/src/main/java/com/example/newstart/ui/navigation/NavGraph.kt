package com.example.newstart.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.newstart.R
import com.example.newstart.ui.screens.auth.LoginScreen
import com.example.newstart.ui.screens.auth.RegisterScreen
import com.example.newstart.ui.screens.auth.WelcomeScreen
import com.example.newstart.ui.screens.detail.DetailScreen
import com.example.newstart.ui.features.home.HomeScreen
import com.example.newstart.ui.features.settings.SettingsScreen
import com.example.newstart.ui.features.journal.JournalScreen
import com.example.newstart.ui.features.journal.JournalArchiveScreen
import com.example.newstart.ui.features.habits.HabitsScreen
import com.example.newstart.ui.features.habits.StatisticsScreen
import com.example.newstart.ui.features.social.SocialScreen
import com.example.newstart.ui.features.pomodoro.PomodoroScreen
import com.example.newstart.ui.screens.PlaceholderScreen
import com.example.newstart.ui.features.admin.AdminScreen

import com.example.newstart.ui.MainViewModel

@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String,
    mainViewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        enterTransition = {
            fadeIn(animationSpec = tween(300))
        },
        exitTransition = {
            fadeOut(animationSpec = tween(300))
        },
        popEnterTransition = {
            fadeIn(animationSpec = tween(300))
        },
        popExitTransition = {
            fadeOut(animationSpec = tween(300))
        }
    ) {
        // Màn hình Welcome (Khởi đầu)
        composable(route = Screen.Welcome.route) {
            WelcomeScreen(
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route)
                },
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                },
                mainViewModel = mainViewModel
            )
        }

        // Màn hình Login
        composable(route = Screen.Login.route) {
            LoginScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                },
                mainViewModel = mainViewModel
            )
        }

        // Màn hình Register
        composable(route = Screen.Register.route) {
            RegisterScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }
                },
                onRegisterSuccess = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }
                },
                mainViewModel = mainViewModel
            )
        }

        // Màn hình Home
        composable(route = Screen.Home.route) {
            HomeScreen(
                mainViewModel = mainViewModel,
                navController = navController
            )
        }

        composable(route = Screen.Journal.route) {
            JournalScreen(navController = navController)
        }

        composable(route = Screen.JournalArchive.route) {
            JournalArchiveScreen(navController = navController)
        }

        composable(route = Screen.Scan.route) {
            PlaceholderScreen(titleRes = R.string.nav_scan)
        }

        composable(route = Screen.Habits.route) {
            HabitsScreen(
                mainViewModel = mainViewModel,
                navController = navController
            )
        }

        composable(route = Screen.Statistics.route) {
            StatisticsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                mainViewModel = mainViewModel
            )
        }

        composable(route = Screen.Profile.route) {
            SettingsScreen(
                onNavigateToSocial = {
                    navController.navigate(Screen.Social.route)
                },
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = false }
                    }
                },
                onNavigateToAdmin = {
                    navController.navigate(Screen.Admin.route)
                }
            )
        }

        composable(route = Screen.Social.route) {
            SocialScreen(
                mainViewModel = mainViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(route = Screen.Admin.route) {
            AdminScreen(
                mainViewModel = mainViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(route = Screen.Pomodoro.route) {
            PomodoroScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                mainViewModel = mainViewModel
            )
        }

        // Màn hình Detail
        composable(
            route = Screen.Detail.route,
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId")
            DetailScreen(userId = userId)
        }
    }
}
