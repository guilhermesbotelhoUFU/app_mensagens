package com.example.app_mensagem.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.app_mensagem.data.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    data class Success(val uid: String) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
    object SignedOut : AuthUiState()
}

class AuthViewModel : ViewModel() {

    private val repository = AuthRepository()
    private val firebaseAuth = FirebaseAuth.getInstance()

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState

    fun login(email: String, pass: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                val result = repository.loginUser(email, pass)
                _uiState.value = AuthUiState.Success(result.user?.uid ?: "")
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(e.message ?: "Ocorreu um erro desconhecido.")
            }
        }
    }

    fun signUp(email: String, pass: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                val result = repository.createUser(email, pass)
                _uiState.value = AuthUiState.Success(result.user?.uid ?: "")
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(e.message ?: "Ocorreu um erro ao criar a conta.")
            }
        }
    }

    fun logout() {
        firebaseAuth.signOut()
        _uiState.value = AuthUiState.SignedOut
    }

    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }
}