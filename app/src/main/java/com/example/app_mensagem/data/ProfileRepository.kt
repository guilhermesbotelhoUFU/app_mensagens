package com.example.app_mensagem.data

import android.net.Uri
import com.example.app_mensagem.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

class ProfileRepository {

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()
    private val storage = FirebaseStorage.getInstance()

    suspend fun getUserProfile(): User? {
        val userId = auth.currentUser?.uid ?: return null
        val snapshot = database.getReference("users").child(userId).get().await()
        return snapshot.getValue(User::class.java)
    }

    suspend fun updateProfile(name: String, imageUri: Uri?): String? {
        val userId = auth.currentUser?.uid ?: return null
        var imageUrl: String? = null

        if (imageUri != null) {
            val storageRef = storage.getReference("profile_pictures/$userId")
            storageRef.putFile(imageUri).await()
            imageUrl = storageRef.downloadUrl.await().toString()
        }

        val updates = mutableMapOf<String, Any>()
        updates["name"] = name
        if (imageUrl != null) {
            updates["profilePictureUrl"] = imageUrl
        }

        database.getReference("users").child(userId).updateChildren(updates).await()

        return imageUrl
    }
}