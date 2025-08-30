package com.example.app_mensagem.data

import com.example.app_mensagem.data.model.User
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

class AuthRepository {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()

    suspend fun loginUser(email: String, pass: String): AuthResult {
        return auth.signInWithEmailAndPassword(email, pass).await()
    }

    suspend fun createUser(email: String, pass: String): AuthResult {
        val authResult = auth.createUserWithEmailAndPassword(email, pass).await()
        val firebaseUser = authResult.user
        if (firebaseUser != null) {
            val user = User(
                uid = firebaseUser.uid,
                name = email.substringBefore('@'), // Usando o início do e-mail como nome temporário
                email = email
            )
            database.getReference("users").child(firebaseUser.uid).setValue(user).await()
        }
        return authResult
    }

    suspend fun sendPasswordResetEmail(email: String) {
        auth.sendPasswordResetEmail(email).await()
    }
}