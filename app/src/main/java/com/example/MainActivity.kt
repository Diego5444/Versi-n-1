package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.notification.NotificationHelper
import com.example.notification.NotificationScheduler
import com.example.ui.screens.MainAppNavigation
import com.example.ui.theme.CineSyncTheme
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.FavoritesViewModel
import com.example.ui.viewmodel.HistoryViewModel
import com.example.ui.viewmodel.MovieViewModel
import com.example.ui.viewmodel.RecommendationsViewModel

class MainActivity : ComponentActivity() {

    private val authViewModel: AuthViewModel by viewModels()
    private val movieViewModel: MovieViewModel by viewModels()
    private val favoritesViewModel: FavoritesViewModel by viewModels()
    private val historyViewModel: HistoryViewModel by viewModels()
    private val recommendationsViewModel: RecommendationsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        try {
            NotificationHelper.createNotificationChannel(applicationContext)
            NotificationScheduler.schedulePeriodicCheck(applicationContext)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        setContent {
            CineSyncTheme {
                MainAppNavigation(
                    authViewModel = authViewModel,
                    movieViewModel = movieViewModel,
                    favoritesViewModel = favoritesViewModel,
                    historyViewModel = historyViewModel,
                    recommendationsViewModel = recommendationsViewModel
                )
            }
        }
    }
}
