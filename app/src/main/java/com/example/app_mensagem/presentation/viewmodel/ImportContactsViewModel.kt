package com.example.app_mensagem.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.app_mensagem.data.DeviceContactsRepository
import com.example.app_mensagem.data.model.DeviceContact
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class ImportContactsUiState {
    object Idle : ImportContactsUiState()
    object Loading : ImportContactsUiState()
    data class Success(val contacts: List<DeviceContact>) : ImportContactsUiState()
}

class ImportContactsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DeviceContactsRepository()

    private val _uiState = MutableStateFlow<ImportContactsUiState>(ImportContactsUiState.Idle)
    val uiState: StateFlow<ImportContactsUiState> = _uiState

    fun loadDeviceContacts() {
        viewModelScope.launch {
            _uiState.value = ImportContactsUiState.Loading
            val contacts = repository.fetchDeviceContacts(getApplication())
            _uiState.value = ImportContactsUiState.Success(contacts)
        }
    }
}