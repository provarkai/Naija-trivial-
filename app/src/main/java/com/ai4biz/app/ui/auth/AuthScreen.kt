package com.ai4biz.app.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.ai4biz.app.navigation.Routes
import com.ai4biz.app.ui.LocalAppContainer
import com.ai4biz.app.ui.SimpleViewModelFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(navController: NavHostController) {
    val container = LocalAppContainer.current
    val viewModel: AuthViewModel = viewModel(
        factory = SimpleViewModelFactory { AuthViewModel(container.authRepository) }
    )
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var email by remember { mutableStateOf("") }

    // Phase 2 Sprint 2 (docs/PHASE2_ARCHITECTURE.md): first-time sign-ins go
    // through the Business Setup wizard once; repeat sign-ins skip straight
    // to Home like before. Either way this is still the app's one point of
    // no return -- Onboarding+Auth are popped off the back stack here.
    fun goHome() {
        scope.launch {
            val hasSetUpBusiness = container.onboardingRepository.hasCompletedBusinessSetup.first()
            val destination = if (hasSetUpBusiness) Routes.HOME else Routes.BUSINESS_SETUP
            navController.navigate(destination) {
                popUpTo(Routes.ONBOARDING) { inclusive = true }
            }
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = "Sign in", style = MaterialTheme.typography.headlineSmall)
            Text(
                text = "Sync your generated documents and subscription across devices.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
            )

            OutlinedButton(
                onClick = {
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            "Google Sign-In needs Firebase configuration -- coming soon."
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Continue with Google")
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = { viewModel.signInWithEmail(email.ifBlank { "you@example.com" }) { goHome() } },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Continue with Email")
            }

            Spacer(modifier = Modifier.height(24.dp))
            TextButton(
                onClick = { viewModel.signInAsGuest { goHome() } },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Continue as Guest")
            }
        }
    }
}
