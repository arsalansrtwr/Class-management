package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.repository.ProfRepository
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.attemptAutoSignIn
import com.example.ui.screens.MainAppScreen
import com.example.ui.screens.TeacherSetupScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.ProfNavy
import com.example.ui.theme.PureWhite
import com.example.ui.viewmodel.ProfViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                AppNavigation()
            }
        }
    }
}

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

@Composable
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    var isAttemptingAutoSignIn by remember { mutableStateOf(currentUser == null) }

    LaunchedEffect(Unit) {
        if (currentUser == null) {
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = { isAttemptingAutoSignIn = false },
                onUnauthenticated = { isAttemptingAutoSignIn = false },
                scope = coroutineScope
            )
        } else {
            isAttemptingAutoSignIn = false
        }
    }

    if (isAttemptingAutoSignIn) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PureWhite),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = ProfNavy)
        }
    } else {
        val user = currentUser
        if (user == null) {
            AuthScreen(
                onAuthSuccess = {
                    // Handled reactively by authStateFlow
                }
            )
        } else {
            AuthenticatedApp(user = user)
        }
    }
}

@Composable
fun AuthenticatedApp(user: FirebaseUser) {
    val viewModel: ProfViewModel = viewModel(
        key = user.uid,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                ProfViewModel(ProfRepository(db), user.uid)
            }
        }
    )

    val teacherProfile by viewModel.teacherProfile.collectAsStateWithLifecycle()
    var forceShowDashboard by remember { mutableStateOf(false) }

    // If teacher profile is not created yet, show onboarding
    if (teacherProfile == null && !forceShowDashboard) {
        TeacherSetupScreen(
            viewModel = viewModel,
            onSetupComplete = { forceShowDashboard = true }
        )
    } else {
        MainAppScreen(
            viewModel = viewModel,
            onSignOut = {
                // Handled reactively
            }
        )
    }
}
