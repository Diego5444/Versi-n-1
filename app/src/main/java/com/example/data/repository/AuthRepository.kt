package com.example.data.repository

import com.example.data.model.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class AuthRepository {
    private val auth: FirebaseAuth?
        get() = try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }

    private val database: FirebaseDatabase?
        get() = try {
            FirebaseDatabase.getInstance("https://abby-cdb30-default-rtdb.firebaseio.com")
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }

    val currentUser: FirebaseUser?
        get() = auth?.currentUser

    fun observeAuthState(): Flow<FirebaseUser?> = callbackFlow {
        val authInstance = auth
        if (authInstance == null) {
            trySend(null)
            close()
            return@callbackFlow
        }
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser)
        }
        authInstance.addAuthStateListener(listener)
        awaitClose { authInstance.removeAuthStateListener(listener) }
    }

    fun observeUserProfile(uid: String): Flow<UserProfile?> = callbackFlow {
        val dbInstance = database
        if (uid.isBlank() || dbInstance == null) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val userRef = dbInstance.getReference("usuarios").child(uid)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val profile = snapshot.getValue(UserProfile::class.java)
                    trySend(profile)
                } else {
                    trySend(null)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(null)
            }
        }

        userRef.addValueEventListener(listener)
        awaitClose { userRef.removeEventListener(listener) }
    }

    suspend fun signInWithEmail(email: String, pass: String): Result<FirebaseUser> {
        return try {
            val authInstance = auth ?: throw Exception("Servicio de autenticación no disponible")
            val result = authInstance.signInWithEmailAndPassword(email, pass).await()
            val user = result.user ?: throw Exception("Usuario no encontrado")
            ensureUserProfileExists(user)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signUpWithEmail(email: String, pass: String, name: String): Result<FirebaseUser> {
        return try {
            val authInstance = auth ?: throw Exception("Servicio de autenticación no disponible")
            val dbInstance = database ?: throw Exception("Base de datos no disponible")
            val result = authInstance.createUserWithEmailAndPassword(email, pass).await()
            val user = result.user ?: throw Exception("Error al crear cuenta")
            
            val profile = UserProfile(
                uid = user.uid,
                email = user.email ?: email,
                displayName = name.ifBlank { "Cinéfilo" },
                photoUrl = "",
                role = "user",
                creationDate = System.currentTimeMillis(),
                lastLogin = System.currentTimeMillis()
            )

            dbInstance.getReference("usuarios").child(user.uid).setValue(profile).await()
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> {
        return try {
            val authInstance = auth ?: throw Exception("Servicio de autenticación no disponible")
            authInstance.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signInAnonymously(): Result<FirebaseUser> {
        return try {
            val authInstance = auth ?: throw Exception("Servicio de autenticación no disponible")
            val result = authInstance.signInAnonymously().await()
            val user = result.user ?: throw Exception("Error en acceso invitado")
            ensureUserProfileExists(user, isGuest = true)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun ensureUserProfileExists(user: FirebaseUser, isGuest: Boolean = false) {
        val dbInstance = database ?: return
        val userRef = dbInstance.getReference("usuarios").child(user.uid)
        val snapshot = userRef.get().await()
        if (!snapshot.exists()) {
            val profile = UserProfile(
                uid = user.uid,
                email = user.email ?: if (isGuest) "invitado@cinesync.app" else "",
                displayName = user.displayName ?: if (isGuest) "Usuario Invitado" else "Cinéfilo",
                photoUrl = user.photoUrl?.toString() ?: "",
                role = "user",
                creationDate = System.currentTimeMillis(),
                lastLogin = System.currentTimeMillis()
            )
            userRef.setValue(profile).await()
        } else {
            userRef.child("lastLogin").setValue(System.currentTimeMillis()).await()
        }
    }

    suspend fun updateProfile(uid: String, displayName: String, photoUrl: String = ""): Result<Unit> {
        return try {
            val dbInstance = database ?: throw Exception("Base de datos no disponible")
            val userRef = dbInstance.getReference("usuarios").child(uid)
            val updates = mutableMapOf<String, Any>(
                "displayName" to displayName
            )
            if (photoUrl.isNotBlank()) {
                updates["photoUrl"] = photoUrl
            }
            userRef.updateChildren(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun signOut() {
        try {
            auth?.signOut()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun deleteAccountAndData(uid: String): Result<Unit> {
        if (uid.isBlank()) return Result.failure(Exception("Usuario no autenticado"))
        return try {
            val dbInstance = database ?: throw Exception("Base de datos no disponible")
            
            // Delete user data nodes in Firebase Realtime Database
            dbInstance.getReference("usuarios").child(uid).removeValue().await()
            dbInstance.getReference("favoritos").child(uid).removeValue().await()
            dbInstance.getReference("historial").child(uid).removeValue().await()
            dbInstance.getReference("continuar_viendo").child(uid).removeValue().await()

            // Delete user in Firebase Authentication
            val user = auth?.currentUser
            if (user != null && user.uid == uid) {
                user.delete().await()
            } else {
                auth?.signOut()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
