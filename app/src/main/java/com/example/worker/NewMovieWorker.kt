package com.example.worker

import android.content.Context
import android.content.Context.MODE_PRIVATE
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.model.FavoriteItem
import com.example.data.model.MovieItem
import com.example.data.model.SeriesItem
import com.example.notification.NotificationHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

class NewMovieWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val dbUrl = "https://abby-cdb30-default-rtdb.firebaseio.com"
    private val prefs = context.getSharedPreferences("movie_notifications_prefs", MODE_PRIVATE)

    override suspend fun doWork(): Result {
        return try {
            val database = try { FirebaseDatabase.getInstance(dbUrl) } catch (e: Exception) { null } ?: return Result.retry()
            val auth = try { FirebaseAuth.getInstance() } catch (e: Exception) { null }
            val userId = auth?.currentUser?.uid

            // Track notified items in SharedPreferences
            val notifiedMovieIds = prefs.getStringSet("notified_movie_ids", emptySet())?.toMutableSet() ?: mutableSetOf()
            val notifiedSeriesIds = prefs.getStringSet("notified_series_ids", emptySet())?.toMutableSet() ?: mutableSetOf()
            val notifiedEpisodeKeys = prefs.getStringSet("notified_episode_keys", emptySet())?.toMutableSet() ?: mutableSetOf()

            var notificationSent = false

            // 1. Check for NEW MOVIES published
            val moviesSnap = database.getReference("peliculas").get().await()
            if (moviesSnap.exists()) {
                for (child in moviesSnap.children) {
                    val movie = child.getValue(MovieItem::class.java) ?: continue
                    if (movie.isPublished && !notifiedMovieIds.contains(movie.id) && movie.id.isNotBlank()) {
                        val title = "🎬 ¡Nuevo Estreno Disponible!"
                        val message = "Se ha añadido '${movie.title}' (${movie.releaseYear}) al catálogo. ¡Mírala ahora!"
                        val poster = movie.posterUrl.ifBlank { movie.bannerUrl }

                        NotificationHelper.showNotificationWithImage(
                            context = context,
                            notificationId = movie.id.hashCode(),
                            title = title,
                            message = message,
                            imageUrl = poster
                        )

                        notifiedMovieIds.add(movie.id)
                        notificationSent = true
                        break // Send one notification per worker run
                    }
                }
            }

            // 2. Check for NEW SERIES/ANIME published
            if (!notificationSent) {
                val seriesSnap = database.getReference("series").get().await()
                val seriesList = mutableListOf<SeriesItem>()
                if (seriesSnap.exists()) {
                    for (child in seriesSnap.children) {
                        child.getValue(SeriesItem::class.java)?.let { seriesList.add(it) }
                    }
                }

                for (series in seriesList) {
                    if (series.isPublished && !notifiedSeriesIds.contains(series.id) && series.id.isNotBlank()) {
                        val isAnime = series.contentType.equals("anime", ignoreCase = true)
                        val title = if (isAnime) "🍿 ¡Nuevo Anime Disponible!" else "📺 ¡Nueva Serie Publicada!"
                        val message = "Se ha publicado '${series.title}' en el catálogo. ¡Empieza a verla ya!"
                        val poster = series.posterUrl.ifBlank { series.bannerUrl }

                        NotificationHelper.showNotificationWithImage(
                            context = context,
                            notificationId = series.id.hashCode(),
                            title = title,
                            message = message,
                            imageUrl = poster
                        )

                        notifiedSeriesIds.add(series.id)
                        notificationSent = true
                        break
                    }
                }

                // 3. Check for NEW EPISODES in User's Favorites
                if (!notificationSent && !userId.isNullOrBlank()) {
                    val favSnap = database.getReference("favoritos").child(userId).get().await()
                    if (favSnap.exists()) {
                        val userFavorites = mutableListOf<FavoriteItem>()
                        for (child in favSnap.children) {
                            child.getValue(FavoriteItem::class.java)?.let { userFavorites.add(it) }
                        }

                        // Filter series/anime in favorites
                        val favoriteSeries = seriesList.filter { series ->
                            userFavorites.any { fav -> fav.contentId == series.id }
                        }

                        for (favSeries in favoriteSeries) {
                            for (season in favSeries.seasons) {
                                for (episode in season.episodes) {
                                    val episodeKey = "${favSeries.id}_s${season.seasonNumber}_e${episode.episodeNumber}"
                                    if (!notifiedEpisodeKeys.contains(episodeKey)) {
                                        val isAnime = favSeries.contentType.equals("anime", ignoreCase = true)
                                        val title = if (isAnime) "🔥 ¡Nuevo Capítulo de Anime!" else "🎉 ¡Nuevo Capítulo Disponible!"
                                        val epTitle = episode.title.ifBlank { "Capítulo ${episode.episodeNumber}" }
                                        val message = "¡Salió el Capítulo ${episode.episodeNumber} ($epTitle) de '${favSeries.title}'! Entra a verlo ya."
                                        val image = episode.thumbnailUrl.ifBlank { favSeries.posterUrl }

                                        NotificationHelper.showNotificationWithImage(
                                            context = context,
                                            notificationId = episodeKey.hashCode(),
                                            title = title,
                                            message = message,
                                            imageUrl = image
                                        )

                                        notifiedEpisodeKeys.add(episodeKey)
                                        notificationSent = true
                                        break
                                    }
                                }
                                if (notificationSent) break
                            }
                            if (notificationSent) break
                        }
                    }
                }
            }

            // Save updated state to SharedPreferences
            prefs.edit()
                .putStringSet("notified_movie_ids", notifiedMovieIds)
                .putStringSet("notified_series_ids", notifiedSeriesIds)
                .putStringSet("notified_episode_keys", notifiedEpisodeKeys)
                .putLong("last_check_timestamp", System.currentTimeMillis())
                .apply()

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}

