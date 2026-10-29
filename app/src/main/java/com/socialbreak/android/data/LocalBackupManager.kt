package com.socialbreak.android.data

import android.content.Context
import android.util.Base64
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object LocalBackupManager {

    private const val ITERATIONS = 10000
    private const val KEY_LENGTH = 256
    private const val GCM_TAG_LENGTH = 128
    private const val IV_LENGTH = 12
    private const val SALT_LENGTH = 16

    fun exportEncryptedBackup(prefs: SocialBreakPreferences, passphrase: String): String {
        val rootJson = JSONObject().apply {
            put("version", 2)
            put("timestamp", System.currentTimeMillis())
            put("sleepMode", prefs.sleepModeEnabled)
            put("bedtimeHour", prefs.bedtimeHour)
            put("bedtimeMinute", prefs.bedtimeMinute)
            put("wakeHour", prefs.wakeHour)
            put("wakeMinute", prefs.wakeMinute)
            put("beforeBedWindDownEnabled", prefs.beforeBedWindDownEnabled)
            put("beforeBedWindDownMinutes", prefs.beforeBedWindDownMinutes)
            put("afterWakeWindDownEnabled", prefs.afterWakeWindDownEnabled)
            put("afterWakeWindDownMinutes", prefs.afterWakeWindDownMinutes)
            put("hardMode", prefs.hardModeEnabled)
            put("themeMode", prefs.themeMode)
            put("iconTheme", prefs.iconTheme)
            put("navigationStyle", prefs.navigationStyle)
            put("globalGrayscale", prefs.globalGrayscale)
            put("dailyGoalMinutes", prefs.dailyGoalMinutes)

            val blockedArray = JSONArray()
            prefs.blockedSocialApps.forEach { blockedArray.put(it) }
            put("blockedSocialApps", blockedArray)

            put("webAdult", prefs.webBlockAdultContent)
            put("webCasinos", prefs.webBlockOnlineCasinos)
            put("webGaming", prefs.webBlockCasualGaming)
            put("webBetting", prefs.webBlockSportsBetting)
            put("webSocial", prefs.webBlockSocialApps)
        }

        val plainBytes = rootJson.toString().toByteArray(Charsets.UTF_8)
        val random = SecureRandom()
        val salt = ByteArray(SALT_LENGTH).also { random.nextBytes(it) }
        val iv = ByteArray(IV_LENGTH).also { random.nextBytes(it) }

        val spec = PBEKeySpec(passphrase.toCharArray(), salt, ITERATIONS, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val secretKey = SecretKeySpec(factory.generateSecret(spec).encoded, "AES")

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH, iv))
        val cipherText = cipher.doFinal(plainBytes)

        val combined = ByteArray(salt.size + iv.size + cipherText.size)
        System.arraycopy(salt, 0, combined, 0, salt.size)
        System.arraycopy(iv, 0, combined, salt.size, iv.size)
        System.arraycopy(cipherText, 0, combined, salt.size + iv.size, cipherText.size)

        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    suspend fun importEncryptedBackup(
        base64Payload: String,
        passphrase: String,
        prefsManager: PreferencesManager
    ): Boolean {
        return try {
            val allBytes = Base64.decode(base64Payload, Base64.NO_WRAP)
            if (allBytes.size < SALT_LENGTH + IV_LENGTH) return false

            val salt = allBytes.copyOfRange(0, SALT_LENGTH)
            val iv = allBytes.copyOfRange(SALT_LENGTH, SALT_LENGTH + IV_LENGTH)
            val cipherText = allBytes.copyOfRange(SALT_LENGTH + IV_LENGTH, allBytes.size)

            val spec = PBEKeySpec(passphrase.toCharArray(), salt, ITERATIONS, KEY_LENGTH)
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            val secretKey = SecretKeySpec(factory.generateSecret(spec).encoded, "AES")

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH, iv))
            val plainBytes = cipher.doFinal(cipherText)

            val json = JSONObject(String(plainBytes, Charsets.UTF_8))
            prefsManager.setSleepMode(json.optBoolean("sleepMode", false))
            prefsManager.updateSleepSchedule(
                json.optInt("bedtimeHour", 23),
                json.optInt("bedtimeMinute", 0),
                json.optInt("wakeHour", 7),
                json.optInt("wakeMinute", 0)
            )
            prefsManager.setBeforeBedWindDown(
                json.optBoolean("beforeBedWindDownEnabled", false),
                json.optInt("beforeBedWindDownMinutes", 30)
            )
            prefsManager.setAfterWakeWindDown(
                json.optBoolean("afterWakeWindDownEnabled", false),
                json.optInt("afterWakeWindDownMinutes", 30)
            )
            prefsManager.setHardMode(json.optBoolean("hardMode", false))
            prefsManager.setThemeMode(json.optString("themeMode", "dark"))
            prefsManager.setIconTheme(json.optString("iconTheme", "painted"))
            prefsManager.setNavigationStyle(json.optString("navigationStyle", "bar"))
            prefsManager.setGlobalGrayscale(json.optBoolean("globalGrayscale", false))
            prefsManager.setDailyGoalMinutes(json.optInt("dailyGoalMinutes", 30))

            prefsManager.setBooleanPreference(PreferencesManager.WEB_BLOCK_ADULT, json.optBoolean("webAdult", false))
            prefsManager.setBooleanPreference(PreferencesManager.WEB_BLOCK_CASINOS, json.optBoolean("webCasinos", false))
            prefsManager.setBooleanPreference(PreferencesManager.WEB_BLOCK_GAMING, json.optBoolean("webGaming", false))
            prefsManager.setBooleanPreference(PreferencesManager.WEB_BLOCK_SPORTS, json.optBoolean("webBetting", false))
            prefsManager.setBooleanPreference(PreferencesManager.WEB_BLOCK_SOCIAL, json.optBoolean("webSocial", false))
            true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun createEncryptedBackup(context: Context, prefsManager: PreferencesManager, pin: String): File {
        val current = prefsManager.preferencesFlow.first()
        val encoded = exportEncryptedBackup(current, pin)
        val backupFile = File(context.getExternalFilesDir(null), "socialbreak_backup.enc")
        backupFile.writeText(encoded, Charsets.UTF_8)
        return backupFile
    }

    suspend fun restoreEncryptedBackup(context: Context, prefsManager: PreferencesManager, pin: String): Boolean {
        val backupFile = File(context.getExternalFilesDir(null), "socialbreak_backup.enc")
        if (!backupFile.exists()) return false
        val encoded = backupFile.readText(Charsets.UTF_8).trim()
        return importEncryptedBackup(encoded, pin, prefsManager)
    }
}
