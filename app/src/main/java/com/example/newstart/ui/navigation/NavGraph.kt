package com.example.newstart.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
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
import com.example.newstart.ui.screens.home.HomeScreen
import com.example.newstart.ui.screens.settings.SettingsScreen
import com.example.newstart.ui.screens.journal.JournalScreen
import com.example.newstart.ui.screens.habits.HabitsScreen
import com.example.newstart.ui.screens.PlaceholderScreen

@Composable
fun NavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Welcome.route,
        modifier = modifier,
        enterTransition = {
            fadeIn(animationSpec = tween(300)) + slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(300)
            )
        },
        exitTransition = {
            fadeOut(animationSpec = tween(300)) + slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(300)
            )
        },
        popEnterTransition = {
            fadeIn(animationSpec = tween(300)) + slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(300)
            )
        },
        popExitTransition = {
            fadeOut(animationSpec = tween(300)) + slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(300)
            )
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
                }
            )
        }

        // Màn hình Login
        composable(route = Screen.Login.route) {
            LoginScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                },
                onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                }
            )
        }

        // Màn hình Register
        composable(route = Screen.Register.route) {
            RegisterScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onRegisterSuccess = {
                    // Sau khi đăng ký thành công (Get Set to Explore), chuyển sang trang Login
                    navController.navigate(Screen.Login.route) {
                        // Xóa trang Register khỏi stack để không bị quay lại trang này khi ấn back ở Login
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }
                }
            )
        }

        // Màn hình Home
        composable(route = Screen.Home.route) {
            HomeScreen(
                onNavigateToDetail = { userId ->
                    navController.navigate(Screen.Detail.createRoute(userId))
                }
            )
        }

        composable(route = Screen.Journal.route) {
            JournalScreen()
        }

        composable(route = Screen.Scan.route) {
            PlaceholderScreen(titleRes = R.string.nav_scan)
        }

        composable(route = Screen.Habits.route) {
            HabitsScreen()
        }

        composable(route = Screen.Profile.route) {
            SettingsScreen(
                onLogout = {
                    navController.navigate(Screen.Welcome.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                }
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
