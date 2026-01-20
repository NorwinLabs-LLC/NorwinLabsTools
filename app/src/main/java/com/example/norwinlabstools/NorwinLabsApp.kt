package com.example.norwinlabstools

import android.app.Application
import com.google.android.material.color.DynamicColors
import com.microsoft.clarity.Clarity
import com.microsoft.clarity.ClarityConfig

class NorwinLabsApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Apply dynamic color to all activities in the app
        DynamicColors.applyToActivitiesIfAvailable(this)

        // Initialize Microsoft Clarity with the required ClarityConfig object
        val config = ClarityConfig("v45dostf70")
        Clarity.initialize(this, config)
    }
}