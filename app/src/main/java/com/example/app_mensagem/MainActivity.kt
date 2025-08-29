package com.example.app_mensagem

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.app_mensagem.presentation.auth.LoginScreen
import com.example.app_mensagem.presentation.auth.SignUpScreen
import com.example.app_mensagem.presentation.home.HomeScreen
import com.example.app_mensagem.presentation.viewmodel.AuthUiState
import com.example.app_mensagem.presentation.viewmodel.AuthViewModel
import com.example.app_mensagem.ui.theme.App_mensagemTheme
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            App_mensagemTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val authViewModel: AuthViewModel by viewModels()
                    val authState by authViewModel.uiState.collectAsState()

                    LaunchedEffect(authState) {
                        if (authState is AuthUiState.SignedOut) {
                            navController.navigate("login") {
                                popUpTo(0) { inclusive = true }
                            }
                            authViewModel.resetState()
                        }
                    }

                    val startDestination = if (FirebaseAuth.getInstance().currentUser != null) {
                        "home"
                    } else {
                        "login"
                    }

                    NavHost(navController = navController, startDestination = startDestination) {
                        composable("login") {
                            LoginScreen(navController, authViewModel)
                        }
                        composable("signup") {
                            SignUpScreen(navController, authViewModel)
                        }
                        composable("home") {
                            HomeScreen(navController, authViewModel)
                        }
                    }
                }
            }
        }
    }
}