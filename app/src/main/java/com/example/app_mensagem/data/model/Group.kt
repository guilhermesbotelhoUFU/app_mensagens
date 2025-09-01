package com.example.app_mensagem.data.model

// Este modelo não precisa ser salvo no Room, apenas no Firebase.
data class Group(
    val id: String = "",
    val name: String = "",
    val creatorId: String = "",
    val members: Map<String, Boolean> = emptyMap(), // Mapa de UIDs dos membros para facilitar a busca
    val timestamp: Long = System.currentTimeMillis()
)