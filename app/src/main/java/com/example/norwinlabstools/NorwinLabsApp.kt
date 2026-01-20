package com.example.norwinlabstools

import android.app.Application
import com.google.android.material.color.DynamicColors

class NorwinLabsApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Apply dynamic color to all activities in the app
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}