package com.example.app_mensagem.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.app_mensagem.MyApplication
import com.example.app_mensagem.data.ChatRepository
import com.example.app_mensagem.data.model.Message
import com.example.app_mensagem.presentation.chat.ChatItem
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class ChatUiState(
    val chatItems: List<ChatItem> = emptyList(),
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

    private fun groupMessagesByDate(messages: List<Message>): List<ChatItem> {
        val items = mutableListOf<ChatItem>()
        if (messages.isEmpty()) return items

        var lastHeaderDate = ""
        messages.forEach { message ->
            val messageDateString = formatDateHeader(message.timestamp)
            if (messageDateString != lastHeaderDate) {
                items.add(ChatItem.DateHeader(messageDateString))
                lastHeaderDate = messageDateString
            }
            items.add(ChatItem.MessageItem(message))
        }
        return items
    }

    private fun formatDateHeader(timestamp: Long): String {
        val messageCalendar = Calendar.getInstance().apply { timeInMillis = timestamp }
        val todayCalendar = Calendar.getInstance()
        val yesterdayCalendar = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }

        return when {
            isSameDay(messageCalendar, todayCalendar) -> "Hoje"
            isSameDay(messageCalendar, yesterdayCalendar) -> "Ontem"
            else -> SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(messageCalendar.time)
        }
    }

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
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
        val chatItems = groupMessagesByDate(filteredList)
        _uiState.value = _uiState.value.copy(
            filteredMessages = filteredList,
            chatItems = chatItems
        )
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
                    val chatItems = groupMessagesByDate(messages)
                    _uiState.value = _uiState.value.copy(
                        messages = messages,
                        filteredMessages = messages,
                        chatItems = chatItems,
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

    // NOVA FUNÇÃO PARA ENVIAR STICKER
    fun sendSticker(conversationId: String, stickerId: String) {
        viewModelScope.launch {
            try {
                repository.sendSticker(conversationId, stickerId)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Erro ao enviar figurinha"
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