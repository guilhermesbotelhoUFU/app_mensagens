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
    val filteredMessages: List<Message> = emptyList(),
    val searchQuery: String = "",
    val conversationTitle: String = "",
    val pinnedMessage: Message? = null,
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

    fun onSearchQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        val filteredList = if (query.isBlank()) {
            _uiState.value.messages
        } else {
            _uiState.value.messages.filter {
                it.text.contains(query, ignoreCase = true)
            }
        }
        _uiState.value = _uiState.value.copy(filteredMessages = filteredList)
    }

    fun loadMessages(conversationId: String) {
        viewModelScope.launch {
            repository.getConversationDetails(conversationId)?.let { conversation ->
                _uiState.value = _uiState.value.copy(conversationTitle = conversation.name)
                if (conversation.pinnedMessageId != null) {
                    val pinnedMessage = repository.getMessageById(conversationId, conversation.pinnedMessageId)
                    _uiState.value = _uiState.value.copy(pinnedMessage = pinnedMessage)
                } else {
                    _uiState.value = _uiState.value.copy(pinnedMessage = null)
                }
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
                        filteredMessages = messages, // Inicialmente, a lista filtrada é a lista completa
                        isLoading = false
                    )
                    repository.markMessagesAsRead(conversationId, messages)
                }
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

    fun onReactionClick(conversationId: String, messageId: String, emoji: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.toggleReaction(conversationId, messageId, emoji)
        }
    }

    fun onPinMessageClick(conversationId: String, message: Message) {
        viewModelScope.launch {
            try {
                repository.togglePinMessage(conversationId, message)
                loadMessages(conversationId)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message ?: "Falha ao fixar mensagem")
            }
        }
    }
}