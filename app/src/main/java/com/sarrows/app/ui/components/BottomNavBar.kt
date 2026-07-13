package com.sarrows.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.sarrows.app.ui.navigation.Routes
import com.sarrows.app.ui.theme.*

data class BottomNavItem(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val route: String
)

val bottomNavItems = listOf(
    BottomNavItem("Home",    Icons.Filled.Home,     Icons.Outlined.Home,        Routes.HOME),
    BottomNavItem("Movies",  Icons.Filled.Movie,    Icons.Outlined.Movie,       Routes.browse("movies")),
    BottomNavItem("Anime",   Icons.Filled.Animation,Icons.Outlined.Animation,   Routes.browse("anime")),
    BottomNavItem("Search",  Icons.Filled.Search,   Icons.Outlined.Search,      Routes.SEARCH),
    BottomNavItem("Profile", Icons.Filled.Person,   Icons.Outlined.PersonOutline, Routes.PROFILE),
)

@Composable
fun SarrowsBottomNavBar(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar(
        containerColor = SarrowsDark,
        contentColor   = SarrowsWhite
    ) {
        bottomNavItems.forEach { item ->
            val selected = currentRoute?.startsWith(item.route.substringBefore("/{")) == true

            NavigationBarItem(
                selected = selected,
                onClick = {
                    if (!selected) {
                        navController.navigate(item.route) {
                            popUpTo(Routes.HOME) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = {
                    Icon(
                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.label
                    )
                },
                label = { Text(item.label, style = MaterialTheme.typography.labelSmall) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor   = SarrowsRed,
                    selectedTextColor   = SarrowsRed,
                    unselectedIconColor = SarrowsWhite60,
                    unselectedTextColor = SarrowsWhite60,
                    indicatorColor      = SarrowsDarkCard
                )
            )
        }
    }
}
