package com.example

import android.app.Application
import com.example.error.CrashHandler
import com.example.notification.NotificationHelper
import com.example.notification.NotificationScheduler
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

class CineSyncApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        CrashHandler.init(this)

        initFirebase()

        try {
            NotificationHelper.createNotificationChannel(this)
            NotificationScheduler.schedulePeriodicCheck(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun initFirebase() {
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                val app = FirebaseApp.initializeApp(this)
                if (app == null || FirebaseApp.getApps(this).isEmpty()) {
                    initFirebaseWithExplicitOptions()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            initFirebaseWithExplicitOptions()
        }
    }

    private fun initFirebaseWithExplicitOptions() {
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                val options = FirebaseOptions.Builder()
                    .setApplicationId("1:738241914654:android:80351b3fed2f4dc3688b0f")
                    .setApiKey("AIzaSyAfqvmcg2Y6GOAQOVjFvXi46hp3NTCT6ZE")
                    .setDatabaseUrl("https://abby-cdb30-default-rtdb.firebaseio.com")
                    .setProjectId("abby-cdb30")
                    .setGcmSenderId("738241914654")
                    .setStorageBucket("abby-cdb30.appspot.com")
                    .build()
                FirebaseApp.initializeApp(this, options)
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }
}

