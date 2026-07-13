package com.sarrows.app.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sarrows.app.ui.theme.*
import com.sarrows.app.ui.viewmodels.*

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateSignUp: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current

    var email    by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.Success) onLoginSuccess()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SarrowsBlack)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(Modifier.height(64.dp))

            // Logo / App name
            Text(
                text = "SARROWS",
                style = MaterialTheme.typography.displaySmall,
                color = SarrowsRed,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Stream unlimited movies & anime",
                style = MaterialTheme.typography.bodyMedium,
                color = SarrowsWhite60,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(48.dp))

            Text(
                text = "Sign In",
                style = MaterialTheme.typography.headlineMedium,
                color = SarrowsWhite
            )

            Spacer(Modifier.height(24.dp))

            // Email field
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                leadingIcon = { Icon(Icons.Default.Email, null) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = sarrowsTextFieldColors()
            )

            Spacer(Modifier.height(16.dp))

            // Password field
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                leadingIcon = { Icon(Icons.Default.Lock, null) },
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(
                            if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (showPassword) "Hide password" else "Show password"
                        )
                    }
                },
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = {
                    focusManager.clearFocus()
                    viewModel.login(email, password)
                }),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = sarrowsTextFieldColors()
            )

            // Error message
            if (uiState is AuthUiState.Error) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = (uiState as AuthUiState.Error).message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(Modifier.height(24.dp))

            // Login button
            Button(
                onClick = {
                    focusManager.clearFocus()
                    viewModel.login(email, password)
                },
                enabled = uiState !is AuthUiState.Loading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SarrowsRed)
            ) {
                if (uiState is AuthUiState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = SarrowsWhite,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Sign In", style = MaterialTheme.typography.labelLarge)
                }
            }

            Spacer(Modifier.height(24.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Don't have an account? ", style = MaterialTheme.typography.bodyMedium, color = SarrowsWhite60)
                TextButton(onClick = onNavigateSignUp) {
                    Text("Sign Up", color = SarrowsRed, style = MaterialTheme.typography.labelLarge)
                }
            }

            Spacer(Modifier.height(64.dp))
        }
    }
}

@Composable
fun sarrowsTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor   = SarrowsRed,
    unfocusedBorderColor = SarrowsDarkBorder,
    focusedLabelColor    = SarrowsRed,
    unfocusedLabelColor  = SarrowsWhite60,
    focusedTextColor     = SarrowsWhite,
    unfocusedTextColor   = SarrowsWhite,
    cursorColor          = SarrowsRed,
    focusedLeadingIconColor   = SarrowsRed,
    unfocusedLeadingIconColor = SarrowsWhite60,
    focusedTrailingIconColor  = SarrowsRed,
    unfocusedTrailingIconColor = SarrowsWhite60
)
