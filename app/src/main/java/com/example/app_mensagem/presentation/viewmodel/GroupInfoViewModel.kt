package com.example.app_mensagem.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.app_mensagem.MyApplication
import com.example.app_mensagem.data.ChatRepository
import com.example.app_mensagem.data.model.Group
import com.example.app_mensagem.data.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class GroupInfoUiState(
    val group: Group? = null,
    val members: List<User> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

class GroupInfoViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ChatRepository

    private val _uiState = MutableStateFlow(GroupInfoUiState())
    val uiState: StateFlow<GroupInfoUiState> = _uiState

    init {
        val db = (application as MyApplication).database
        repository = ChatRepository(db.conversationDao(), db.messageDao(), application)
    }

    fun loadGroupInfo(groupId: String) {
        viewModelScope.launch {
            _uiState.value = GroupInfoUiState(isLoading = true)
            try {
                val groupDetails = repository.getGroupDetails(groupId)
                val groupMembers = repository.getGroupMembers(groupId)
                _uiState.value = GroupInfoUiState(
                    group = groupDetails,
                    members = groupMembers,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = GroupInfoUiState(error = e.message ?: "Falha ao carregar informações do grupo.", isLoading = false)
            }
        }
    }
}