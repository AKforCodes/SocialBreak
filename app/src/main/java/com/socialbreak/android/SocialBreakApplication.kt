package com.socialbreak.android

import android.app.Application

class SocialBreakApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize dynamic launcher shortcuts on home screen
        com.socialbreak.android.data.AppShortcutManager.initDynamicShortcuts(this)
    }
}
