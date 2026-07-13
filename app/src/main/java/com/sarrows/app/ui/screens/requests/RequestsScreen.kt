package com.sarrows.app.ui.screens.requests

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.sarrows.app.data.models.ContentRequest
import com.sarrows.app.ui.theme.*
import com.sarrows.app.ui.viewmodels.RequestsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestsScreen(
    navController: NavController,
    viewModel: RequestsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showForm by remember { mutableStateOf(false) }
    var reqTitle by remember { mutableStateOf("") }
    var reqType  by remember { mutableStateOf("anime") }
    var reqNote  by remember { mutableStateOf("") }

    LaunchedEffect(uiState.submitSuccess) {
        if (uiState.submitSuccess) {
            showForm = false
            reqTitle = ""; reqNote = ""
            viewModel.resetSubmitState()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Content Requests", color = SarrowsWhite) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null, tint = SarrowsWhite)
                    }
                },
                actions = {
                    IconButton(onClick = { showForm = true }) {
                        Icon(Icons.Default.Add, null, tint = SarrowsRed)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SarrowsDark)
            )
        },
        containerColor = SarrowsBlack
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = SarrowsRed)
            }
            return@Scaffold
        }

        if (uiState.requests.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Movie, null, tint = SarrowsWhite38, modifier = Modifier.size(80.dp))
                    Spacer(Modifier.height(16.dp))
                    Text("No requests yet", color = SarrowsWhite60, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text("Tap + to request a movie, anime, or series", color = SarrowsWhite38, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = { showForm = true }, colors = ButtonDefaults.buttonColors(containerColor = SarrowsRed)) {
                        Icon(Icons.Default.Add, null)
                        Spacer(Modifier.width(8.dp))
                        Text("New Request")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.requests) { request ->
                    RequestCard(request = request, onCancel = { viewModel.cancelRequest(request.id) })
                }
            }
        }

        // New request sheet
        if (showForm) {
            ModalBottomSheet(
                onDismissRequest = { showForm = false },
                containerColor = SarrowsDarkCard
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("New Request", style = MaterialTheme.typography.titleLarge, color = SarrowsWhite)
                    Spacer(Modifier.height(16.dp))

                    // Type selector
                    Text("Type", style = MaterialTheme.typography.labelMedium, color = SarrowsWhite60)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("movie", "anime", "series").forEach { type ->
                            FilterChip(
                                selected = reqType == type,
                                onClick  = { reqType = type },
                                label    = { Text(type.replaceFirstChar { it.uppercase() }) },
                                colors   = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SarrowsRed,
                                    selectedLabelColor = SarrowsWhite,
                                    containerColor = SarrowsDarkSurface,
                                    labelColor = SarrowsWhite60
                                )
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = reqTitle, onValueChange = { reqTitle = it },
                        label = { Text("Title *") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SarrowsRed, focusedLabelColor = SarrowsRed,
                            focusedTextColor = SarrowsWhite, unfocusedTextColor = SarrowsWhite,
                            unfocusedBorderColor = SarrowsDarkBorder, unfocusedLabelColor = SarrowsWhite60
                        )
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = reqNote, onValueChange = { reqNote = it },
                        label = { Text("Note (optional)") },
                        modifier = Modifier.fillMaxWidth(), maxLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SarrowsRed, focusedLabelColor = SarrowsRed,
                            focusedTextColor = SarrowsWhite, unfocusedTextColor = SarrowsWhite,
                            unfocusedBorderColor = SarrowsDarkBorder, unfocusedLabelColor = SarrowsWhite60
                        )
                    )

                    uiState.submitError?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }

                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = { viewModel.submitRequest(reqTitle, reqType, reqNote) },
                        enabled = !uiState.isSubmitting,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SarrowsRed)
                    ) {
                        if (uiState.isSubmitting) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = SarrowsWhite, strokeWidth = 2.dp)
                        } else {
                            Text("Submit Request")
                        }
                    }
                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
fun RequestCard(request: ContentRequest, onCancel: () -> Unit) {
    val (statusColor, statusLabel) = when (request.status) {
        "pending"     -> SarrowsGold  to "Pending"
        "in_progress" -> SarrowsBlue  to "In Progress"
        "fulfilled"   -> SarrowsGreen to "Fulfilled"
        "rejected"    -> SarrowsRedLight to "Rejected"
        else          -> SarrowsWhite60  to request.status
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SarrowsDarkCard),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(request.title, style = MaterialTheme.typography.titleSmall, color = SarrowsWhite)
                    Text(request.type.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.bodySmall, color = SarrowsWhite60)
                }
                Surface(shape = MaterialTheme.shapes.small, color = statusColor.copy(alpha = 0.15f)) {
                    Text(statusLabel, color = statusColor, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall)
                }
            }
            request.note?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, style = MaterialTheme.typography.bodySmall, color = SarrowsWhite60)
            }
            request.adminNote?.let {
                Spacer(Modifier.height(4.dp))
                Row {
                    Icon(Icons.Default.AdminPanelSettings, null, tint = SarrowsBlue, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(it, style = MaterialTheme.typography.bodySmall, color = SarrowsBlue)
                }
            }
            if (request.status == "pending") {
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onCancel, contentPadding = PaddingValues(0.dp)) {
                    Icon(Icons.Default.Cancel, null, tint = SarrowsWhite38, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Cancel", color = SarrowsWhite38, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
