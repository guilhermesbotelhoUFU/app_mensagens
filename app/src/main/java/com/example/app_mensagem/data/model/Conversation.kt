package com.example.app_mensagem.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversations")
data class Conversation(
    @PrimaryKey val id: String = "",
    val name: String = "",
    val lastMessage: String = "",
    val timestamp: Long = 0L
)