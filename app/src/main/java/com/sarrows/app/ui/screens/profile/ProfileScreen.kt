package com.sarrows.app.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.sarrows.app.ui.components.SarrowsBottomNavBar
import com.sarrows.app.ui.navigation.Routes
import com.sarrows.app.ui.theme.*
import com.sarrows.app.ui.viewmodels.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    onLogout: () -> Unit,
    profileViewModel: ProfileViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val profileState by profileViewModel.uiState.collectAsState()
    val authState    by authViewModel.uiState.collectAsState()

    var showLogoutDialog by remember { mutableStateOf(false) }

    LaunchedEffect(authState) {
        if (authState is AuthUiState.Idle && authState !is AuthUiState.Loading) {
            // Check if we just logged out
        }
    }

    Scaffold(
        bottomBar = { SarrowsBottomNavBar(navController) },
        topBar = {
            TopAppBar(
                title = { Text("Profile", color = SarrowsWhite) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SarrowsDark)
            )
        },
        containerColor = SarrowsBlack
    ) { padding ->
        if (profileState.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = SarrowsRed)
            }
            return@Scaffold
        }

        val user = profileState.user

        if (user == null) {
            // Not logged in
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.AccountCircle, null, tint = SarrowsWhite38, modifier = Modifier.size(80.dp))
                    Spacer(Modifier.height(16.dp))
                    Text("You're not signed in", style = MaterialTheme.typography.titleMedium, color = SarrowsWhite60)
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = { navController.navigate(Routes.LOGIN) },
                        colors = ButtonDefaults.buttonColors(containerColor = SarrowsRed)
                    ) { Text("Sign In") }
                }
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(padding).background(SarrowsBlack),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(32.dp))

            // Avatar
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .background(SarrowsRed.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = user.name.first().uppercaseChar().toString(),
                    style = MaterialTheme.typography.displaySmall,
                    color = SarrowsRed
                )
            }

            Spacer(Modifier.height(16.dp))
            Text(user.name, style = MaterialTheme.typography.headlineSmall, color = SarrowsWhite)
            Text(user.email, style = MaterialTheme.typography.bodyMedium, color = SarrowsWhite60)

            if (user.role == "admin") {
                Spacer(Modifier.height(8.dp))
                Surface(shape = MaterialTheme.shapes.small, color = SarrowsRed.copy(alpha = 0.2f)) {
                    Text("Admin", color = SarrowsRed, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium)
                }
            }

            Spacer(Modifier.height(40.dp))

            // Menu items
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                ProfileMenuItem(
                    icon = Icons.Default.VideoLibrary,
                    title = "Content Requests",
                    subtitle = "Request movies, anime & series",
                    onClick = { navController.navigate(Routes.REQUESTS) }
                )
                HorizontalDivider(color = SarrowsDarkBorder)
                ProfileMenuItem(
                    icon = Icons.Default.Bookmark,
                    title = "My Watchlist",
                    subtitle = "Content you saved to watch later",
                    onClick = { /* future: watchlist screen */ }
                )
                HorizontalDivider(color = SarrowsDarkBorder)
                ProfileMenuItem(
                    icon = Icons.Default.History,
                    title = "Watch History",
                    subtitle = "Continue where you left off",
                    onClick = { /* future: history screen */ }
                )
                HorizontalDivider(color = SarrowsDarkBorder)
                ProfileMenuItem(
                    icon = Icons.Default.Settings,
                    title = "Settings",
                    subtitle = "App preferences",
                    onClick = { /* future: settings screen */ }
                )
                HorizontalDivider(color = SarrowsDarkBorder)
                ProfileMenuItem(
                    icon = Icons.Default.Logout,
                    title = "Sign Out",
                    subtitle = "Sign out of your account",
                    onClick = { showLogoutDialog = true },
                    iconTint = SarrowsRed,
                    titleColor = SarrowsRed
                )
            }
        }

        if (showLogoutDialog) {
            AlertDialog(
                onDismissRequest = { showLogoutDialog = false },
                title = { Text("Sign Out", color = SarrowsWhite) },
                text  = { Text("Are you sure you want to sign out?", color = SarrowsWhite60) },
                confirmButton = {
                    TextButton(onClick = {
                        showLogoutDialog = false
                        authViewModel.logout()
                        onLogout()
                    }) { Text("Sign Out", color = SarrowsRed) }
                },
                dismissButton = {
                    TextButton(onClick = { showLogoutDialog = false }) { Text("Cancel", color = SarrowsWhite60) }
                },
                containerColor = SarrowsDarkCard,
                tonalElevation = 0.dp
            )
        }
    }
}

@Composable
fun ProfileMenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    iconTint: androidx.compose.ui.graphics.Color = SarrowsWhite60,
    titleColor: androidx.compose.ui.graphics.Color = SarrowsWhite
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                Modifier.height(IntrinsicSize.Min)
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, null, tint = iconTint, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleSmall, color = titleColor)
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = SarrowsWhite60)
                }
                Icon(Icons.Default.ChevronRight, null, tint = SarrowsWhite38)
            }
        }
    }
}
