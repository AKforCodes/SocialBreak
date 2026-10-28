package com.socialbreak.android.data

import android.content.Context
import android.provider.Settings

object GrayscaleController {

    private const val DALTONIZER_ENABLED = "accessibility_display_daltonizer_enabled"
    private const val DALTONIZER_MODE = "accessibility_display_daltonizer"
    private const val MONOCHROME_MODE = 0

    fun isHardwareGrayscaleActive(context: Context): Boolean {
        return try {
            val enabled = Settings.Secure.getInt(context.contentResolver, DALTONIZER_ENABLED, 0)
            val mode = Settings.Secure.getInt(context.contentResolver, DALTONIZER_MODE, -1)
            enabled == 1 && mode == MONOCHROME_MODE
        } catch (e: Exception) {
            false
        }
    }

    fun setHardwareGrayscale(context: Context, enabled: Boolean): Boolean {
        return try {
            if (enabled) {
                Settings.Secure.putInt(context.contentResolver, DALTONIZER_ENABLED, 1)
                Settings.Secure.putInt(context.contentResolver, DALTONIZER_MODE, MONOCHROME_MODE)
            } else {
                Settings.Secure.putInt(context.contentResolver, DALTONIZER_ENABLED, 0)
            }
            true
        } catch (e: SecurityException) {
            // WRITE_SECURE_SETTINGS permission requires ADB grant on non-rooted devices
            false
        }
    }

    fun getGrayscaleCssInjection(): String {
        return """
            (function() {
                var style = document.getElementById('socialbreak-grayscale');
                if (!style) {
                    style = document.createElement('style');
                    style.id = 'socialbreak-grayscale';
                    style.textContent = 'html, body, img, video { filter: grayscale(100%) !important; }';
                    (document.head || document.documentElement).appendChild(style);
                }
            })();
        """.trimIndent()
    }
}
