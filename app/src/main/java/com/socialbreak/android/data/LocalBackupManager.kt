package com.socialbreak.android.data

import android.content.Context
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

    suspend fun createEncryptedBackup(context: Context, prefs: PreferencesManager, pin: String): File {
        val rootJson = JSONObject().apply {
            put("version", 1)
            put("timestamp", System.currentTimeMillis())
            put("sleepMode", prefs.sleepModeEnabled.first())
            put("bedtimeHour", prefs.bedtimeHour.first())
            put("bedtimeMinute", prefs.bedtimeMinute.first())
            put("wakeHour", prefs.wakeHour.first())
            put("wakeMinute", prefs.wakeMinute.first())
            put("windDownMinutes", prefs.windDownMinutes.first())
            put("hardMode", prefs.hardModeEnabled.first())

            val blockedArray = JSONArray()
            prefs.blockedPackages.first().forEach { blockedArray.put(it) }
            put("blockedPackages", blockedArray)

            put("webAdult", prefs.webBlockingAdult.first())
            put("webCasinos", prefs.webBlockingCasinos.first())
            put("webGaming", prefs.webBlockingGaming.first())
            put("webBetting", prefs.webBlockingBetting.first())
            put("webSocial", prefs.webBlockingSocial.first())

            val chipsArray = JSONArray()
            prefs.reachableSocialChips.first().forEach { chipsArray.put(it) }
            put("reachableChips", chipsArray)

            put("blocksLanded", prefs.todayBlocksLanded.first())
            put("strippedCount", prefs.todayStrippedCount.first())
        }

        val plainBytes = rootJson.toString().toByteArray(Charsets.UTF_8)

        // Generate salt & IV
        val random = SecureRandom()
        val salt = ByteArray(SALT_LENGTH)
        random.nextBytes(salt)
        val iv = ByteArray(IV_LENGTH)
        random.nextBytes(iv)

        // Derive key from PIN
        val spec = PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val keyBytes = factory.generateSecret(spec).encoded
        val secretKey = SecretKeySpec(keyBytes, "AES")

        // Encrypt with AES-GCM
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH, iv))
        val cipherText = cipher.doFinal(plainBytes)

        // Write to local backup file: salt + iv + cipherText
        val backupFile = File(context.getExternalFilesDir(null), "socialbreak_backup.enc")
        backupFile.outputStream().use { os ->
            os.write(salt)
            os.write(iv)
            os.write(cipherText)
        }

        return backupFile
    }

    suspend fun restoreEncryptedBackup(context: Context, prefs: PreferencesManager, pin: String): Boolean {
        val backupFile = File(context.getExternalFilesDir(null), "socialbreak_backup.enc")
        if (!backupFile.exists()) return false

        return try {
            val allBytes = backupFile.readBytes()
            if (allBytes.size < SALT_LENGTH + IV_LENGTH) return false

            val salt = allBytes.copyOfRange(0, SALT_LENGTH)
            val iv = allBytes.copyOfRange(SALT_LENGTH, SALT_LENGTH + IV_LENGTH)
            val cipherText = allBytes.copyOfRange(SALT_LENGTH + IV_LENGTH, allBytes.size)

            val spec = PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, KEY_LENGTH)
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            val keyBytes = factory.generateSecret(spec).encoded
            val secretKey = SecretKeySpec(keyBytes, "AES")

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH, iv))
            val plainBytes = cipher.doFinal(cipherText)

            val json = JSONObject(String(plainBytes, Charsets.UTF_8))

            prefs.setSleepMode(json.optBoolean("sleepMode", false))
            prefs.setSleepSchedule(
                json.optInt("bedtimeHour", 23),
                json.optInt("bedtimeMinute", 0),
                json.optInt("wakeHour", 7),
                json.optInt("wakeMinute", 0)
            )
            prefs.setHardMode(json.optBoolean("hardMode", false))
            prefs.setWebFilter("adult", json.optBoolean("webAdult", false))
            prefs.setWebFilter("casinos", json.optBoolean("webCasinos", false))
            prefs.setWebFilter("gaming", json.optBoolean("webGaming", false))
            prefs.setWebFilter("betting", json.optBoolean("webBetting", false))
            prefs.setWebFilter("social", json.optBoolean("webSocial", false))

            true
        } catch (e: Exception) {
            false
        }
    }
}
