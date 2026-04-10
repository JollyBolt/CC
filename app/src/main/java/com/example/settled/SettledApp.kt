package com.example.settled

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SettledApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialization for Firebase or other services can go here
    }
}
