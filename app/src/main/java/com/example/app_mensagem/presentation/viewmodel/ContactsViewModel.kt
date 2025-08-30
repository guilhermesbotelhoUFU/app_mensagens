package com.example.app_mensagem.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.app_mensagem.data.ChatRepository
import com.example.app_mensagem.data.model.User
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
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

class ContactsViewModel : ViewModel() {

    private val repository = ChatRepository()
    private val auth = FirebaseAuth.getInstance()

    private val _uiState = MutableStateFlow<ContactsUiState>(ContactsUiState.Loading)
    val uiState: StateFlow<ContactsUiState> = _uiState

    private val _navigationState = MutableStateFlow<ContactNavigationState>(ContactNavigationState.Idle)
    val navigationState: StateFlow<ContactNavigationState> = _navigationState

    fun loadUsers() {
        val currentUserId = auth.currentUser?.uid
        if (currentUserId == null) {
            _uiState.value = ContactsUiState.Error("Usuário não autenticado.")
            return
        }

        viewModelScope.launch {
            val allUsersFlow = repository.getUsers()
            val conversationsFlow = repository.getConversations(currentUserId)

            allUsersFlow.combine(conversationsFlow) { allUsers, conversations ->
                val existingConversationUserIds = conversations.map {
                    it.id.replace(currentUserId, "").replace("-", "")
                }.toSet()

                allUsers.filter { user -> user.uid !in existingConversationUserIds }
            }
                .catch { exception ->
                    _uiState.value = ContactsUiState.Error(exception.message ?: "Erro desconhecido")
                }
                .collect { filteredUsers ->
                    _uiState.value = ContactsUiState.Success(filteredUsers)
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