package com.example.app_mensagem

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.app_mensagem.presentation.auth.ForgotPasswordScreen
import com.example.app_mensagem.presentation.auth.LoginScreen
import com.example.app_mensagem.presentation.auth.SignUpScreen
import com.example.app_mensagem.presentation.chat.ChatScreen
import com.example.app_mensagem.presentation.contacts.ContactsScreen
import com.example.app_mensagem.presentation.home.HomeScreen
import com.example.app_mensagem.presentation.import.ImportContactsScreen
import com.example.app_mensagem.presentation.viewmodel.*
import com.example.app_mensagem.ui.theme.App_mensagemTheme
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // ... (onNewIntent e handleIntent que adicionamos para notificações)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        askNotificationPermission()
        setContent {
            App_mensagemTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val authViewModel: AuthViewModel by viewModels()
                    val conversationsViewModel: ConversationsViewModel by viewModels()
                    val contactsViewModel: ContactsViewModel by viewModels()
                    val importContactsViewModel: ImportContactsViewModel by viewModels() // NOVO VIEWMODEL
                    val authState by authViewModel.uiState.collectAsState()

                    // ... (LaunchedEffect do authState)

                    val startDestination = if (FirebaseAuth.getInstance().currentUser != null) {
                        "home"
                    } else {
                        "login"
                    }

                    NavHost(navController = navController, startDestination = startDestination) {
                        composable("login") { LoginScreen(navController, authViewModel) }
                        composable("signup") { SignUpScreen(navController, authViewModel) }
                        composable("home") { HomeScreen(navController, authViewModel, conversationsViewModel) }
                        composable("forgot_password") { ForgotPasswordScreen(navController, authViewModel) }
                        composable("contacts") { ContactsScreen(navController, contactsViewModel) }
                        // NOVA ROTA
                        composable("import_contacts") { ImportContactsScreen(navController, importContactsViewModel) }
                        composable(
                            route = "chat/{conversationId}",
                            arguments = listOf(navArgument("conversationId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val conversationId = backStackEntry.arguments?.getString("conversationId")
                            ChatScreen(navController, conversationId)
                        }
                    }
                }
            }
        }
    }
}