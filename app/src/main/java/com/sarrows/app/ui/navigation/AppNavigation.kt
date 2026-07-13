package com.sarrows.app.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.navigation.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sarrows.app.ui.screens.auth.LoginScreen
import com.sarrows.app.ui.screens.auth.SignUpScreen
import com.sarrows.app.ui.screens.browse.BrowseScreen
import com.sarrows.app.ui.screens.detail.DetailScreen
import com.sarrows.app.ui.screens.home.HomeScreen
import com.sarrows.app.ui.screens.profile.ProfileScreen
import com.sarrows.app.ui.screens.requests.RequestsScreen
import com.sarrows.app.ui.screens.search.SearchScreen

object Routes {
    const val LOGIN      = "login"
    const val SIGNUP     = "signup"
    const val HOME       = "home"
    const val BROWSE     = "browse/{tab}"
    const val SEARCH     = "search"
    const val DETAIL     = "detail/{type}/{id}"
    const val PROFILE    = "profile"
    const val REQUESTS   = "requests"

    fun browse(tab: String = "movies") = "browse/$tab"
    fun detail(type: String, id: String) = "detail/$type/$id"
}

@Composable
fun AppNavigation(startDestination: String = Routes.HOME) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition  = { fadeIn(tween(220)) + slideInHorizontally(tween(220)) { it / 6 } },
        exitTransition   = { fadeOut(tween(160)) },
        popEnterTransition  = { fadeIn(tween(220)) + slideInHorizontally(tween(220)) { -it / 6 } },
        popExitTransition   = { fadeOut(tween(160)) + slideOutHorizontally(tween(220)) { it / 6 } }
    ) {
        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess   = { navController.navigate(Routes.HOME) { popUpTo(0) } },
                onNavigateSignUp = { navController.navigate(Routes.SIGNUP) }
            )
        }

        composable(Routes.SIGNUP) {
            SignUpScreen(
                onSignUpSuccess  = { navController.navigate(Routes.LOGIN) { popUpTo(Routes.SIGNUP) { inclusive = true } } },
                onNavigateLogin  = { navController.popBackStack() }
            )
        }

        composable(Routes.HOME) {
            HomeScreen(
                navController = navController,
                onNavigateLogin = { navController.navigate(Routes.LOGIN) }
            )
        }

        composable(
            route = Routes.BROWSE,
            arguments = listOf(navArgument("tab") { defaultValue = "movies" })
        ) { entry ->
            BrowseScreen(
                tab = entry.arguments?.getString("tab") ?: "movies",
                navController = navController
            )
        }

        composable(Routes.SEARCH) {
            SearchScreen(navController = navController)
        }

        composable(
            route = Routes.DETAIL,
            arguments = listOf(
                navArgument("type") {},
                navArgument("id") {}
            )
        ) { entry ->
            DetailScreen(
                contentType = entry.arguments?.getString("type") ?: "movie",
                contentId   = entry.arguments?.getString("id") ?: "",
                navController = navController
            )
        }

        composable(Routes.PROFILE) {
            ProfileScreen(
                navController = navController,
                onLogout = { navController.navigate(Routes.LOGIN) { popUpTo(0) } }
            )
        }

        composable(Routes.REQUESTS) {
            RequestsScreen(navController = navController)
        }
    }
}
