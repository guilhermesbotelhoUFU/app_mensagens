package com.example.app_mensagem.presentation.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.app_mensagem.MyApplication
import com.example.app_mensagem.data.ChatRepository
import com.example.app_mensagem.data.model.Conversation
import com.example.app_mensagem.data.model.Message
import com.example.app_mensagem.data.model.User
import com.example.app_mensagem.presentation.chat.ChatItem
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
    val conversation: Conversation? = null,
    val pinnedMessage: Message? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val mediaToSendUri: Uri? = null,
    val mediaType: String? = null,
    val groupMembers: Map<String, User> = emptyMap()
)

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ChatRepository

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState

    init {
        val db = (application as MyApplication).database
        repository = ChatRepository(db.conversationDao(), db.messageDao(), application)
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

    fun onMediaSelected(uri: Uri?, type: String) {
        _uiState.value = _uiState.value.copy(mediaToSendUri = uri, mediaType = type)
    }

    fun onSearchQueryChanged(query: String) {
        val filteredList = if (query.isBlank()) {
            _uiState.value.messages
        } else {
            _uiState.value.messages.filter {
                it.content.contains(query, ignoreCase = true) && it.type == "TEXT"
            }
        }
        _uiState.value = _uiState.value.copy(
            searchQuery = query,
            filteredMessages = filteredList,
            chatItems = groupMessagesByDate(filteredList)
        )
    }

    fun loadMessages(conversationId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val conversation = repository.getConversationDetails(conversationId)
            if (conversation == null) {
                _uiState.value = _uiState.value.copy(error = "Conversa não encontrada.", isLoading = false)
                return@launch
            }

            var membersMap = emptyMap<String, User>()
            if (conversation.isGroup) {
                val members = repository.getGroupMembers(conversationId)
                membersMap = members.associateBy { it.uid }
            }

            val pinnedMessage = if (conversation.pinnedMessageId != null) {
                repository.getMessageById(conversationId, conversation.pinnedMessageId, conversation.isGroup)
            } else {
                null
            }

            _uiState.value = _uiState.value.copy(
                conversationTitle = conversation.name,
                conversation = conversation,
                groupMembers = membersMap,
                pinnedMessage = pinnedMessage
            )

            repository.getMessagesForConversation(conversationId, conversation.isGroup)
                .catch { exception ->
                    _uiState.value = _uiState.value.copy(
                        error = exception.message ?: "Erro ao carregar mensagens",
                        isLoading = false
                    )
                }
                .collect { messages ->
                    val filteredList = if (_uiState.value.searchQuery.isBlank()) {
                        messages
                    } else {
                        messages.filter {
                            it.content.contains(_uiState.value.searchQuery, ignoreCase = true) && it.type == "TEXT"
                        }
                    }
                    _uiState.value = _uiState.value.copy(
                        messages = messages,
                        filteredMessages = filteredList,
                        chatItems = groupMessagesByDate(filteredList),
                        isLoading = false
                    )

                    if (!conversation.isGroup) {
                        repository.markMessagesAsRead(conversationId, messages, conversation.isGroup)
                    }
                }
        }
    }

    fun sendMessage(conversationId: String, text: String) {
        viewModelScope.launch {
            val isGroup = _uiState.value.conversation?.isGroup ?: false
            val mediaUri = _uiState.value.mediaToSendUri

            try {
                if (mediaUri != null) {
                    val mediaType = _uiState.value.mediaType ?: "IMAGE"
                    repository.sendMediaMessage(conversationId, mediaUri, mediaType, isGroup)
                    _uiState.value = _uiState.value.copy(mediaToSendUri = null, mediaType = null)
                } else if (text.isNotBlank()) {
                    repository.sendMessage(conversationId, text, isGroup)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Erro ao enviar mensagem"
                )
            }
        }
    }

    fun sendSticker(conversationId: String, stickerId: String) {
        viewModelScope.launch {
            val isGroup = _uiState.value.conversation?.isGroup ?: false
            try {
                repository.sendStickerMessage(conversationId, stickerId, isGroup)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Erro ao enviar sticker"
                )
            }
        }
    }

    fun onReactionClick(conversationId: String, messageId: String, emoji: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val isGroup = _uiState.value.conversation?.isGroup ?: false
            repository.toggleReaction(conversationId, messageId, emoji, isGroup)
        }
    }

    fun onPinMessageClick(conversationId: String, message: Message) {
        viewModelScope.launch {
            try {
                val isGroup = _uiState.value.conversation?.isGroup ?: false
                repository.togglePinMessage(conversationId, message, isGroup)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message ?: "Falha ao fixar mensagem")
            }
        }
    }
}