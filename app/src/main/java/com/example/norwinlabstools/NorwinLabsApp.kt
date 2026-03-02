package com.example.norwinlabstools

import android.app.Application
import com.google.android.material.color.DynamicColors
import com.google.firebase.FirebaseApp

class NorwinLabsApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize Firebase
        FirebaseApp.initializeApp(this)

        // Apply dynamic color to all activities in the app
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}