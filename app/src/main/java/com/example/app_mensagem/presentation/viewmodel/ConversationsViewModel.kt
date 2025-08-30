package com.example.app_mensagem.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.app_mensagem.data.ChatRepository
import com.example.app_mensagem.data.model.Conversation
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

sealed class ConversationUiState {
    object Loading : ConversationUiState()
    data class Success(val conversations: List<Conversation>) : ConversationUiState()
    data class Error(val message: String) : ConversationUiState()
}

class ConversationsViewModel : ViewModel() {

    private val repository = ChatRepository()
    private val auth = FirebaseAuth.getInstance()

    private val _uiState = MutableStateFlow<ConversationUiState>(ConversationUiState.Loading)
    val uiState: StateFlow<ConversationUiState> = _uiState


    fun loadConversations() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            _uiState.value = ConversationUiState.Error("Usuário não autenticado.")
            return
        }

        viewModelScope.launch {
            repository.getConversations(userId)
                .catch { exception ->
                    _uiState.value = ConversationUiState.Error(exception.message ?: "Erro desconhecido")
                }
                .collect { conversations ->
                    _uiState.value = ConversationUiState.Success(conversations)
                }
        }
    }
}