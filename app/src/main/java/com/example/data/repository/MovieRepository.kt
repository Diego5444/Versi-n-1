package com.example.data.repository

import com.example.data.model.CategoryItem
import com.example.data.model.MovieItem
import com.example.data.model.SeriesItem
import com.example.data.model.UserProfile
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class MovieRepository {
    private val database: FirebaseDatabase?
        get() = try {
            FirebaseDatabase.getInstance("https://abby-cdb30-default-rtdb.firebaseio.com")
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }

    // Movies Flow: Strictly reads from Realtime DB 'peliculas'
    fun getMoviesFlow(): Flow<List<MovieItem>> = callbackFlow {
        val dbInstance = database
        if (dbInstance == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val moviesRef = dbInstance.getReference("peliculas")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val list = mutableListOf<MovieItem>()
                    for (child in snapshot.children) {
                        child.getValue(MovieItem::class.java)?.let { list.add(it) }
                    }
                    trySend(list)
                } else {
                    // Strictly emit empty list if database has no items
                    trySend(emptyList())
                }
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(emptyList())
            }
        }

        moviesRef.addValueEventListener(listener)
        awaitClose { moviesRef.removeEventListener(listener) }
    }

    // Series Flow: Reads from Realtime DB 'series'
    fun getSeriesFlow(): Flow<List<SeriesItem>> = callbackFlow {
        val dbInstance = database
        if (dbInstance == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val seriesRef = dbInstance.getReference("series")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val list = mutableListOf<SeriesItem>()
                    for (child in snapshot.children) {
                        child.getValue(SeriesItem::class.java)?.let { list.add(it) }
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

        seriesRef.addValueEventListener(listener)
        awaitClose { seriesRef.removeEventListener(listener) }
    }

    // Categories Flow: Reads from Realtime DB 'categorias'
    fun getCategoriesFlow(): Flow<List<CategoryItem>> = callbackFlow {
        val dbInstance = database
        if (dbInstance == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val catRef = dbInstance.getReference("categorias")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val list = mutableListOf<CategoryItem>()
                    for (child in snapshot.children) {
                        child.getValue(CategoryItem::class.java)?.let { list.add(it) }
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

        catRef.addValueEventListener(listener)
        awaitClose { catRef.removeEventListener(listener) }
    }

    // All Users Flow for Admin Panel
    fun getUsersFlow(): Flow<List<UserProfile>> = callbackFlow {
        val dbInstance = database
        if (dbInstance == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val usersRef = dbInstance.getReference("usuarios")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val list = mutableListOf<UserProfile>()
                    for (child in snapshot.children) {
                        child.getValue(UserProfile::class.java)?.let { list.add(it) }
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

        usersRef.addValueEventListener(listener)
        awaitClose { usersRef.removeEventListener(listener) }
    }

    // CRUD for Movies
    suspend fun saveMovie(movie: MovieItem): Result<Unit> {
        return try {
            val dbInstance = database ?: throw Exception("Base de datos no disponible")
            val ref = dbInstance.getReference("peliculas")
            val movieId = movie.id.ifBlank { ref.push().key ?: System.currentTimeMillis().toString() }
            val itemToSave = movie.copy(id = movieId)
            ref.child(movieId).setValue(itemToSave).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteMovie(movieId: String): Result<Unit> {
        return try {
            val dbInstance = database ?: throw Exception("Base de datos no disponible")
            dbInstance.getReference("peliculas").child(movieId).removeValue().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // CRUD for Series
    suspend fun saveSeries(series: SeriesItem): Result<Unit> {
        return try {
            val dbInstance = database ?: throw Exception("Base de datos no disponible")
            val ref = dbInstance.getReference("series")
            val seriesId = series.id.ifBlank { ref.push().key ?: System.currentTimeMillis().toString() }
            val itemToSave = series.copy(id = seriesId)
            ref.child(seriesId).setValue(itemToSave).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteSeries(seriesId: String): Result<Unit> {
        return try {
            val dbInstance = database ?: throw Exception("Base de datos no disponible")
            dbInstance.getReference("series").child(seriesId).removeValue().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // CRUD for Categories
    suspend fun saveCategory(category: CategoryItem): Result<Unit> {
        return try {
            val dbInstance = database ?: throw Exception("Base de datos no disponible")
            val ref = dbInstance.getReference("categorias")
            val catId = category.id.ifBlank { ref.push().key ?: System.currentTimeMillis().toString() }
            val itemToSave = category.copy(id = catId)
            ref.child(catId).setValue(itemToSave).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteCategory(categoryId: String): Result<Unit> {
        return try {
            val dbInstance = database ?: throw Exception("Base de datos no disponible")
            dbInstance.getReference("categorias").child(categoryId).removeValue().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // User Role Management
    suspend fun updateUserRole(uid: String, newRole: String): Result<Unit> {
        return try {
            val dbInstance = database ?: throw Exception("Base de datos no disponible")
            dbInstance.getReference("usuarios").child(uid).child("role").setValue(newRole).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Increment View Count
    suspend fun incrementMovieViews(movieId: String) {
        try {
            val dbInstance = database ?: return
            val ref = dbInstance.getReference("peliculas").child(movieId).child("viewsCount")
            val snap = ref.get().await()
            val current = snap.getValue(Int::class.java) ?: 0
            ref.setValue(current + 1).await()
        } catch (_: Exception) {}
    }

    suspend fun incrementSeriesViews(seriesId: String) {
        try {
            val dbInstance = database ?: return
            val ref = dbInstance.getReference("series").child(seriesId).child("viewsCount")
            val snap = ref.get().await()
            val current = snap.getValue(Int::class.java) ?: 0
            ref.setValue(current + 1).await()
        } catch (_: Exception) {}
    }
}
