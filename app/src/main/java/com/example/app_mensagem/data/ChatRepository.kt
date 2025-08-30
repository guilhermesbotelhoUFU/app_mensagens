package com.example.app_mensagem.data

import com.example.app_mensagem.data.model.Conversation
import com.example.app_mensagem.data.model.Message
import com.example.app_mensagem.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class ChatRepository {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()

    fun getConversations(userId: String): Flow<List<Conversation>> = callbackFlow {
        val conversationsRef = database.getReference("user-conversations").child(userId)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val conversations = snapshot.children.mapNotNull {
                    it.getValue(Conversation::class.java)
                }
                trySend(conversations)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        conversationsRef.addValueEventListener(listener)

        awaitClose {
            conversationsRef.removeEventListener(listener)
        }
    }

    fun getUsers(): Flow<List<User>> = callbackFlow {
        val currentUserId = auth.currentUser?.uid
        val usersRef = database.getReference("users")

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val users = snapshot.children.mapNotNull {
                    it.getValue(User::class.java)
                }.filter { it.uid != currentUserId }
                trySend(users)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        usersRef.addValueEventListener(listener)

        awaitClose { usersRef.removeEventListener(listener) }
    }


    suspend fun createOrGetConversation(targetUser: User): String {
        val currentUserId = auth.currentUser?.uid ?: throw IllegalStateException("Usuário não autenticado")
        val targetUserId = targetUser.uid

        val conversationId = if (currentUserId > targetUserId) {
            "$currentUserId-$targetUserId"
        } else {
            "$targetUserId-$currentUserId"
        }

        val conversationRef = database.getReference("user-conversations/$currentUserId/$conversationId")
        val snapshot = conversationRef.get().await()

        if (snapshot.exists()) {
            return conversationId
        }

        val currentUserConversation = Conversation(
            id = conversationId,
            name = targetUser.name,
            lastMessage = "Inicie a conversa!",
            timestamp = System.currentTimeMillis()
        )

        val targetUserConversation = Conversation(
            id = conversationId,
            name = auth.currentUser?.email?.substringBefore('@') ?: "Usuário",
            lastMessage = "Inicie a conversa!",
            timestamp = System.currentTimeMillis()
        )

        database.getReference("user-conversations/$currentUserId/$conversationId")
            .setValue(currentUserConversation).await()

        database.getReference("user-conversations/$targetUserId/$conversationId")
            .setValue(targetUserConversation).await()

        return conversationId
    }

    fun getMessages(conversationId: String): Flow<List<Message>> = callbackFlow {
        val messagesRef = database.getReference("messages").child(conversationId)

        val listener = messagesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val messages = snapshot.children.mapNotNull {
                    it.getValue(Message::class.java)
                }
                trySend(messages)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        })

        awaitClose { messagesRef.removeEventListener(listener) }
    }

    suspend fun sendMessage(conversationId: String, text: String) {
        val currentUserId = auth.currentUser?.uid ?: throw IllegalStateException("Usuário não autenticado")
        val messagesRef = database.getReference("messages").child(conversationId)
        val messageId = messagesRef.push().key ?: throw IllegalStateException("Não foi possível criar ID da mensagem")

        val message = Message(
            id = messageId,
            senderId = currentUserId,
            text = text,
            timestamp = System.currentTimeMillis()
        )

        messagesRef.child(messageId).setValue(message).await()

        val lastMessageUpdate = mapOf(
            "lastMessage" to text,
            "timestamp" to message.timestamp
        )

        val userIds = conversationId.split("-")
        if (userIds.size == 2) {
            val user1 = userIds[0]
            val user2 = userIds[1]

            database.getReference("user-conversations/$user1/$conversationId").updateChildren(lastMessageUpdate)
            database.getReference("user-conversations/$user2/$conversationId").updateChildren(lastMessageUpdate)
        }
    }

    suspend fun getConversationDetails(conversationId: String): Conversation? = suspendCoroutine { continuation ->
        val currentUserId = auth.currentUser?.uid
        if (currentUserId == null) {
            continuation.resume(null)
            return@suspendCoroutine
        }

        database.getReference("user-conversations").child(currentUserId).child(conversationId)
            .get()
            .addOnSuccessListener { snapshot ->
                val conversation = snapshot.getValue(Conversation::class.java)
                continuation.resume(conversation)
            }
            .addOnFailureListener {
                continuation.resume(null)
            }
    }
}