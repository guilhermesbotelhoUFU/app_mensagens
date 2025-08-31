package com.example.app_mensagem.data

import com.example.app_mensagem.data.local.ConversationDao
import com.example.app_mensagem.data.local.MessageDao
import com.example.app_mensagem.data.model.Conversation
import com.example.app_mensagem.data.model.Message
import com.example.app_mensagem.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.getValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class ChatRepository(
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao
) {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()

    fun getConversations(): Flow<List<Conversation>> {
        val userId = auth.currentUser?.uid ?: return flowOf(emptyList())

        val conversationsRef = database.getReference("user-conversations").child(userId)
        conversationsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val conversations = snapshot.children.mapNotNull { it.getValue(Conversation::class.java) }
                CoroutineScope(Dispatchers.IO).launch {
                    conversations.forEach { conversationDao.insertOrUpdate(it) }
                }
            }
            override fun onCancelled(error: DatabaseError) { }
        })

        return conversationDao.getAllConversations()
    }

    fun getMessagesForConversation(conversationId: String): Flow<List<Message>> {
        val currentUserId = auth.currentUser?.uid
        val messagesRef = database.getReference("messages").child(conversationId)

        messagesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val messages = snapshot.children.mapNotNull {
                    val msg = it.getValue(Message::class.java)
                    msg?.copy(conversationId = conversationId)
                }

                CoroutineScope(Dispatchers.IO).launch {
                    messages.forEach { message ->
                        messageDao.insertOrUpdate(message)
                        if (message.senderId != currentUserId && message.deliveredTimestamp == 0L) {
                            confirmDelivery(conversationId, message.id)
                        }
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) { }
        })

        return messageDao.getMessagesForConversation(conversationId)
    }

    private fun confirmDelivery(conversationId: String, messageId: String) {
        database.getReference("messages/$conversationId/$messageId")
            .child("deliveredTimestamp").setValue(System.currentTimeMillis())
    }

    fun markMessagesAsRead(conversationId: String, messages: List<Message>) {
        val currentUserId = auth.currentUser?.uid
        messages.forEach { message ->
            if (message.senderId != currentUserId && message.readTimestamp == 0L) {
                database.getReference("messages/$conversationId/${message.id}")
                    .child("readTimestamp").setValue(System.currentTimeMillis())
            }
        }
    }

    suspend fun sendMessage(conversationId: String, text: String) {
        val currentUserId = auth.currentUser?.uid ?: return
        val messagesRef = database.getReference("messages").child(conversationId)
        val messageId = messagesRef.push().key ?: return

        val message = Message(
            id = messageId,
            conversationId = conversationId,
            senderId = currentUserId,
            text = text,
            timestamp = System.currentTimeMillis(),
            status = "SENDING",
            type = "TEXT" // Garante que o tipo é texto
        )
        messageDao.insertOrUpdate(message)

        val messageForFirebase = message.copy(status = "SENT")

        try {
            messagesRef.child(messageId).setValue(messageForFirebase).await()
            messageDao.insertOrUpdate(message.copy(status = "SENT"))

            val lastMessageUpdate = mapOf("lastMessage" to text, "timestamp" to message.timestamp)
            val userIds = conversationId.split("-")
            if (userIds.size == 2) {
                database.getReference("user-conversations/${userIds[0]}/$conversationId").updateChildren(lastMessageUpdate)
                database.getReference("user-conversations/${userIds[1]}/$conversationId").updateChildren(lastMessageUpdate)
            }
        } catch (e: Exception) {
            messageDao.insertOrUpdate(message.copy(status = "FAILED"))
        }
    }

    // NOVA FUNÇÃO PARA ENVIAR STICKERS
    suspend fun sendSticker(conversationId: String, stickerId: String) {
        val currentUserId = auth.currentUser?.uid ?: return
        val messagesRef = database.getReference("messages").child(conversationId)
        val messageId = messagesRef.push().key ?: return

        val message = Message(
            id = messageId,
            conversationId = conversationId,
            senderId = currentUserId,
            text = "Figurinha", // Texto alternativo para notificações e preview
            timestamp = System.currentTimeMillis(),
            status = "SENDING",
            type = "STICKER",
            stickerId = stickerId
        )

        messageDao.insertOrUpdate(message)

        val messageForFirebase = message.copy(status = "SENT")

        try {
            messagesRef.child(messageId).setValue(messageForFirebase).await()
            messageDao.insertOrUpdate(message.copy(status = "SENT"))

            val lastMessageUpdate = mapOf("lastMessage" to "Figurinha", "timestamp" to message.timestamp)
            val userIds = conversationId.split("-")
            if (userIds.size == 2) {
                database.getReference("user-conversations/${userIds[0]}/$conversationId").updateChildren(lastMessageUpdate)
                database.getReference("user-conversations/${userIds[1]}/$conversationId").updateChildren(lastMessageUpdate)
            }
        } catch (e: Exception) {
            messageDao.insertOrUpdate(message.copy(status = "FAILED"))
        }
    }

    suspend fun createOrGetConversation(targetUser: User): String {
        val currentUserId = auth.currentUser?.uid ?: throw IllegalStateException("Usuário não autenticado")
        val conversationId = getConversationId(currentUserId, targetUser.uid)

        val existingConversation = conversationDao.getConversationById(conversationId)
        if (existingConversation != null) {
            return conversationId
        }

        val currentUserSnapshot = database.getReference("users").child(currentUserId).get().await()
        val currentUserName = currentUserSnapshot.child("name").getValue(String::class.java) ?: "Usuário"

        val conversationForCurrentUser = Conversation(id = conversationId, name = targetUser.name, lastMessage = "Inicie a conversa!", timestamp = System.currentTimeMillis())
        val conversationForTargetUser = Conversation(id = conversationId, name = currentUserName, lastMessage = "Inicie a conversa!", timestamp = System.currentTimeMillis())

        database.getReference("user-conversations/$currentUserId/$conversationId").setValue(conversationForCurrentUser).await()
        database.getReference("user-conversations/${targetUser.uid}/$conversationId").setValue(conversationForTargetUser).await()

        conversationDao.insertOrUpdate(conversationForCurrentUser)

        return conversationId
    }

    private fun getConversationId(userId1: String, userId2: String): String {
        return if (userId1 > userId2) "$userId1-$userId2" else "$userId2-$userId1"
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
            override fun onCancelled(error: DatabaseError) { close(error.toException()) }
        }
        usersRef.addValueEventListener(listener)
        awaitClose { usersRef.removeEventListener(listener) }
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

    suspend fun getMessageById(conversationId: String, messageId: String): Message? {
        val snapshot = database.getReference("messages/$conversationId/$messageId").get().await()
        return snapshot.getValue(Message::class.java)
    }

    suspend fun togglePinMessage(conversationId: String, message: Message) {
        val currentUserId = auth.currentUser?.uid ?: return
        val otherUserId = conversationId.replace(currentUserId, "").replace("-", "")

        val conversationRefCurrentUser = database.getReference("user-conversations/$currentUserId/$conversationId")

        val currentConversation = conversationRefCurrentUser.get().await().getValue(Conversation::class.java)

        val newPinnedId = if (currentConversation?.pinnedMessageId == message.id) {
            null
        } else {
            message.id
        }

        conversationRefCurrentUser.child("pinnedMessageId").setValue(newPinnedId).await()
        database.getReference("user-conversations/$otherUserId/$conversationId").child("pinnedMessageId").setValue(newPinnedId).await()
    }

    suspend fun toggleReaction(conversationId: String, messageId: String, emoji: String) {
        val currentUserId = auth.currentUser?.uid ?: return
        val messageRef = database.getReference("messages/$conversationId/$messageId")

        val snapshot = messageRef.child("reactions").get().await()
        val reactions = snapshot.getValue<MutableMap<String, String>>() ?: mutableMapOf()

        if (reactions[currentUserId] == emoji) {
            reactions.remove(currentUserId)
        } else {
            reactions[currentUserId] = emoji
        }

        messageRef.child("reactions").setValue(reactions).await()
    }
}