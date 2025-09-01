package com.example.app_mensagem.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.app_mensagem.MyApplication
import com.example.app_mensagem.data.ChatRepository
import com.example.app_mensagem.data.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

sealed class ContactsUiState {
    object Loading : ContactsUiState()
    data class Success(val users: List<User>) : ContactsUiState()
    data class Error(val message: String) : ContactsUiState()
}

sealed class ContactNavigationState {
    object Idle : ContactNavigationState()
    data class NavigateToChat(val conversationId: String) : ContactNavigationState()
}

class ContactsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ChatRepository

    private val _uiState = MutableStateFlow<ContactsUiState>(ContactsUiState.Loading)
    val uiState: StateFlow<ContactsUiState> = _uiState

    private val _navigationState = MutableStateFlow<ContactNavigationState>(ContactNavigationState.Idle)
    val navigationState: StateFlow<ContactNavigationState> = _navigationState

    init {
        val db = (application as MyApplication).database
        // Correção: Passando "application" como o terceiro parâmetro para o context
        repository = ChatRepository(db.conversationDao(), db.messageDao(), application)
        loadUsers()
    }

    fun loadUsers() {
        viewModelScope.launch {
            repository.getUsers()
                .catch { exception ->
                    _uiState.value = ContactsUiState.Error(exception.message ?: "Erro desconhecido")
                }
                .collect { users ->
                    _uiState.value = ContactsUiState.Success(users)
                }
        }
    }

    fun onUserClicked(user: User) {
        viewModelScope.launch {
            try {
                val conversationId = repository.createOrGetConversation(user)
                _navigationState.value = ContactNavigationState.NavigateToChat(conversationId)
            } catch (e: Exception) {
                _uiState.value = ContactsUiState.Error(e.message ?: "Falha ao criar conversa")
            }
        }
    }

    fun onNavigated() {
        _navigationState.value = ContactNavigationState.Idle
    }
}