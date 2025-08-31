package com.example.app_mensagem.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.app_mensagem.MyApplication
import com.example.app_mensagem.data.ChatRepository
import com.example.app_mensagem.data.model.Message
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

data class ChatUiState(
    val messages: List<Message> = emptyList(),
    val conversationTitle: String = "",
    val isLoading: Boolean = true,
    val error: String? = null
)

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ChatRepository
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState

    init {
        val db = (application as MyApplication).database
        repository = ChatRepository(db.conversationDao(), db.messageDao())
    }

    fun loadMessages(conversationId: String) {
        viewModelScope.launch {
            repository.getConversationDetails(conversationId)?.let { conversation ->
                _uiState.value = _uiState.value.copy(conversationTitle = conversation.name)
            }

            repository.getMessagesForConversation(conversationId)
                .catch { exception ->
                    _uiState.value = _uiState.value.copy(
                        error = exception.message ?: "Erro ao carregar mensagens",
                        isLoading = false
                    )
                }
                .collect { messages ->
                    _uiState.value = _uiState.value.copy(
                        messages = messages,
                        isLoading = false
                    )
                    // Lógica para marcar mensagens como lidas
                    repository.markMessagesAsRead(conversationId, messages)
                }
        }
    }

    fun onReactionClick(conversationId: String, messageId: String, emoji: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.toggleReaction(conversationId, messageId, emoji)
        }
    }

    fun sendMessage(conversationId: String, text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            try {
                repository.sendMessage(conversationId, text)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Erro ao enviar mensagem"
                )
            }
        }
    }
}