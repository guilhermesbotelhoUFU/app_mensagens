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
    data class Success(
        val allConversations: List<Conversation> = emptyList(),
        val filteredConversations: List<Conversation> = emptyList(),
        val searchQuery: String = ""
    ) : ConversationUiState()
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

    fun onSearchQueryChanged(query: String) {
        val currentState = _uiState.value
        if (currentState is ConversationUiState.Success) {
            val filteredList = if (query.isBlank()) {
                currentState.allConversations
            } else {
                currentState.allConversations.filter {
                    // Filtra pelo nome da conversa, ignorando maiúsculas/minúsculas
                    it.name.contains(query, ignoreCase = true)
                }
            }
            _uiState.value = currentState.copy(
                searchQuery = query,
                filteredConversations = filteredList
            )
        }
    }

    fun loadConversations() {
        viewModelScope.launch {
            repository.getConversations()
                .catch { exception ->
                    _uiState.value = ConversationUiState.Error(exception.message ?: "Erro desconhecido")
                }
                .collect { conversations ->
                    _uiState.value = ConversationUiState.Success(
                        allConversations = conversations,
                        filteredConversations = conversations // Inicialmente, a lista filtrada é a lista completa
                    )
                }
        }
    }
}