package com.example.app_mensagem.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.app_mensagem.MyApplication
import com.example.app_mensagem.data.ChatRepository
import com.example.app_mensagem.data.model.Conversation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

sealed class ConversationUiState {
    object Loading : ConversationUiState()
    data class Success(val conversations: List<Conversation>) : ConversationUiState()
    data class Error(val message: String) : ConversationUiState()
}

class ConversationsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ChatRepository

    private val _uiState = MutableStateFlow<ConversationUiState>(ConversationUiState.Loading)
    val uiState: StateFlow<ConversationUiState> = _uiState

    init {
        val db = (application as MyApplication).database
        repository = ChatRepository(db.conversationDao(), db.messageDao())
        loadConversations()
    }

    fun loadConversations() {
        viewModelScope.launch {
            repository.getConversations()
                .catch { exception ->
                    _uiState.value = ConversationUiState.Error(exception.message ?: "Erro desconhecido")
                }
                .collect { conversations ->
                    _uiState.value = ConversationUiState.Success(conversations)
                }
        }
    }
}