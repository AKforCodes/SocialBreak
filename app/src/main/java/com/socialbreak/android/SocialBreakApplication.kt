package com.socialbreak.android

import android.app.Application

class SocialBreakApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize application-level singletons (DataStore preferences, local state)
    }
}
