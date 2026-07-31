package com.example.data.repository

import com.example.data.model.ContinueWatchingEntry
import com.example.data.model.FavoriteItem
import com.example.data.model.WatchHistoryEntry
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class UserDataRepository {
    private val database: FirebaseDatabase?
        get() = try {
            FirebaseDatabase.getInstance("https://abby-cdb30-default-rtdb.firebaseio.com")
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }

    // Favorites Flow
    fun getFavoritesFlow(uid: String): Flow<List<FavoriteItem>> = callbackFlow {
        val dbInstance = database
        if (uid.isBlank() || dbInstance == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val favRef = dbInstance.getReference("favoritos").child(uid)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val list = mutableListOf<FavoriteItem>()
                    for (child in snapshot.children) {
                        child.getValue(FavoriteItem::class.java)?.let { list.add(it) }
                    }
                    trySend(list)
                } else {
                    trySend(emptyList())
                }
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(emptyList())
            }
        }

        favRef.addValueEventListener(listener)
        awaitClose { favRef.removeEventListener(listener) }
    }

    suspend fun toggleFavorite(
        uid: String,
        contentId: String,
        contentType: String,
        title: String,
        posterUrl: String,
        rating: Double,
        releaseYear: Int,
        isCurrentlyFav: Boolean
    ): Result<Boolean> {
        if (uid.isBlank()) return Result.failure(Exception("Debes iniciar sesión"))
        return try {
            val dbInstance = database ?: throw Exception("Base de datos no disponible")
            val favRef = dbInstance.getReference("favoritos").child(uid).child(contentId)
            if (isCurrentlyFav) {
                favRef.removeValue().await()
                Result.success(false)
            } else {
                val item = FavoriteItem(
                    contentId = contentId,
                    contentType = contentType,
                    title = title,
                    posterUrl = posterUrl,
                    rating = rating,
                    releaseYear = releaseYear,
                    addedAt = System.currentTimeMillis()
                )
                favRef.setValue(item).await()
                Result.success(true)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Watch History Flow
    fun getWatchHistoryFlow(uid: String): Flow<List<WatchHistoryEntry>> = callbackFlow {
        val dbInstance = database
        if (uid.isBlank() || dbInstance == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val historyRef = dbInstance.getReference("historial").child(uid)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val list = mutableListOf<WatchHistoryEntry>()
                    for (child in snapshot.children) {
                        child.getValue(WatchHistoryEntry::class.java)?.let { list.add(it) }
                    }
                    list.sortByDescending { it.watchedAtTimestamp }
                    trySend(list)
                } else {
                    trySend(emptyList())
                }
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(emptyList())
            }
        }

        historyRef.addValueEventListener(listener)
        awaitClose { historyRef.removeEventListener(listener) }
    }

    suspend fun addOrUpdateHistory(uid: String, entry: WatchHistoryEntry): Result<Unit> {
        if (uid.isBlank()) return Result.failure(Exception("Debes iniciar sesión"))
        return try {
            val dbInstance = database ?: throw Exception("Base de datos no disponible")
            val historyId = entry.historyId.ifBlank { entry.contentId }
            dbInstance.getReference("historial")
                .child(uid)
                .child(historyId)
                .setValue(entry.copy(historyId = historyId, watchedAtTimestamp = System.currentTimeMillis()))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun clearWatchHistory(uid: String): Result<Unit> {
        if (uid.isBlank()) return Result.failure(Exception("Sin usuario"))
        return try {
            val dbInstance = database ?: throw Exception("Base de datos no disponible")
            dbInstance.getReference("historial").child(uid).removeValue().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Continue Watching Flow
    fun getContinueWatchingFlow(uid: String): Flow<List<ContinueWatchingEntry>> = callbackFlow {
        val dbInstance = database
        if (uid.isBlank() || dbInstance == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val cwRef = dbInstance.getReference("continuar_viendo").child(uid)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val list = mutableListOf<ContinueWatchingEntry>()
                    for (child in snapshot.children) {
                        child.getValue(ContinueWatchingEntry::class.java)?.let { list.add(it) }
                    }
                    list.sortByDescending { it.lastPlayedTimestamp }
                    trySend(list)
                } else {
                    trySend(emptyList())
                }
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(emptyList())
            }
        }

        cwRef.addValueEventListener(listener)
        awaitClose { cwRef.removeEventListener(listener) }
    }

    suspend fun saveContinueWatching(uid: String, entry: ContinueWatchingEntry): Result<Unit> {
        if (uid.isBlank()) return Result.failure(Exception("Debes iniciar sesión"))
        return try {
            val dbInstance = database ?: throw Exception("Base de datos no disponible")
            dbInstance.getReference("continuar_viendo")
                .child(uid)
                .child(entry.contentId)
                .setValue(entry.copy(lastPlayedTimestamp = System.currentTimeMillis()))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeContinueWatching(uid: String, contentId: String): Result<Unit> {
        if (uid.isBlank()) return Result.failure(Exception("Debes iniciar sesión"))
        return try {
            val dbInstance = database ?: throw Exception("Base de datos no disponible")
            dbInstance.getReference("continuar_viendo")
                .child(uid)
                .child(contentId)
                .removeValue()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun purgeAllUserData(uid: String): Result<Unit> {
        if (uid.isBlank()) return Result.failure(Exception("Usuario no autenticado"))
        return try {
            val dbInstance = database ?: throw Exception("Base de datos no disponible")
            dbInstance.getReference("favoritos").child(uid).removeValue().await()
            dbInstance.getReference("historial").child(uid).removeValue().await()
            dbInstance.getReference("continuar_viendo").child(uid).removeValue().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
