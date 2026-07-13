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
fun SignUpScreen(
    onSignUpSuccess: () -> Unit,
    onNavigateLogin: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current

    var nickname  by remember { mutableStateOf("") }
    var email     by remember { mutableStateOf("") }
    var password  by remember { mutableStateOf("") }
    var confirm   by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    val fieldErrors = (uiState as? AuthUiState.Error)?.fieldErrors

    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.Success) {
            viewModel.resetState()
            onSignUpSuccess()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(SarrowsBlack)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(Modifier.height(64.dp))
            Text("SARROWS", style = MaterialTheme.typography.displaySmall, color = SarrowsRed)
            Spacer(Modifier.height(32.dp))
            Text("Create Account", style = MaterialTheme.typography.headlineMedium, color = SarrowsWhite)
            Spacer(Modifier.height(24.dp))

            OutlinedTextField(
                value = nickname, onValueChange = { nickname = it },
                label = { Text("Nickname") },
                leadingIcon = { Icon(Icons.Default.Person, null) },
                isError = fieldErrors?.containsKey("nickname") == true,
                supportingText = { fieldErrors?.get("nickname")?.let { Text(it, color = MaterialTheme.colorScheme.error) } },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                singleLine = true, modifier = Modifier.fillMaxWidth(),
                colors = sarrowsTextFieldColors()
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = email, onValueChange = { email = it },
                label = { Text("Email") },
                leadingIcon = { Icon(Icons.Default.Email, null) },
                isError = fieldErrors?.containsKey("email") == true,
                supportingText = { fieldErrors?.get("email")?.let { Text(it, color = MaterialTheme.colorScheme.error) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                singleLine = true, modifier = Modifier.fillMaxWidth(),
                colors = sarrowsTextFieldColors()
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = password, onValueChange = { password = it },
                label = { Text("Password") },
                leadingIcon = { Icon(Icons.Default.Lock, null) },
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility, null)
                    }
                },
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                singleLine = true, modifier = Modifier.fillMaxWidth(),
                colors = sarrowsTextFieldColors()
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = confirm, onValueChange = { confirm = it },
                label = { Text("Confirm Password") },
                leadingIcon = { Icon(Icons.Default.Lock, null) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    focusManager.clearFocus()
                    viewModel.signUp(nickname, email, password, confirm)
                }),
                singleLine = true, modifier = Modifier.fillMaxWidth(),
                colors = sarrowsTextFieldColors()
            )

            if (uiState is AuthUiState.Error) {
                Spacer(Modifier.height(8.dp))
                val errMsg = (uiState as AuthUiState.Error).message
                if (errMsg.isNotEmpty()) {
                    Text(errMsg, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    focusManager.clearFocus()
                    viewModel.signUp(nickname, email, password, confirm)
                },
                enabled = uiState !is AuthUiState.Loading,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SarrowsRed)
            ) {
                if (uiState is AuthUiState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = SarrowsWhite, strokeWidth = 2.dp)
                } else {
                    Text("Create Account", style = MaterialTheme.typography.labelLarge)
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Already have an account? ", style = MaterialTheme.typography.bodyMedium, color = SarrowsWhite60)
                TextButton(onClick = onNavigateLogin) {
                    Text("Sign In", color = SarrowsRed, style = MaterialTheme.typography.labelLarge)
                }
            }
            Spacer(Modifier.height(64.dp))
        }
    }
}
