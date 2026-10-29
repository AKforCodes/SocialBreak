package com.socialbreak.android.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import com.socialbreak.android.data.DomainFilterRepository
import com.socialbreak.android.data.PreferencesManager
import com.socialbreak.android.data.SocialBreakPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.FileInputStream
import java.io.FileOutputStream

class WebBlockerVpnService : VpnService() {

    companion object {
        const val ACTION_STOP_VPN = "com.socialbreak.android.action.STOP_VPN"
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var vpnInterface: ParcelFileDescriptor? = null

    @Volatile
    private var currentPrefs = SocialBreakPreferences()

    @Volatile
    private var isRunning = false

    override fun onCreate() {
        super.onCreate()
        val prefsManager = PreferencesManager(applicationContext)
        serviceScope.launch {
            prefsManager.preferencesFlow.collectLatest { prefs ->
                currentPrefs = prefs
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_VPN) {
            stopVpnLoop()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(1002, createVpnNotification())
        if (!isRunning) {
            isRunning = true
            serviceScope.launch {
                runLocalDnsFilterLoop()
            }
        }
        return START_STICKY
    }

    private fun runLocalDnsFilterLoop() {
        try {
            val builder = Builder()
                .setSession("SocialLite Web Blocking")
                .addAddress("10.111.222.1", 24)
                .addDnsServer("10.111.222.2")
                .addRoute("10.111.222.2", 32)
                .setBlocking(true)

            vpnInterface = builder.establish() ?: return
            val fd = vpnInterface!!.fileDescriptor
            val input = FileInputStream(fd)
            val output = FileOutputStream(fd)
            val packet = ByteArray(32767)

            while (isRunning && vpnInterface != null) {
                val length = input.read(packet)
                if (length <= 28) continue

                // Check IPv4 (version 4) and UDP (protocol 17)
                val version = (packet[0].toInt() ushr 4) and 0x0F
                if (version != 4) continue
                val protocol = packet[9].toInt() and 0xFF
                if (protocol != 17) continue

                val ihl = (packet[0].toInt() and 0x0F) * 4
                if (length < ihl + 8 + 12) continue

                val dstPort = ((packet[ihl + 2].toInt() and 0xFF) shl 8) or (packet[ihl + 3].toInt() and 0xFF)
                if (dstPort != 53) continue

                val dnsOffset = ihl + 8
                val queriedDomain = extractDnsQueryDomain(packet, dnsOffset, length) ?: continue

                val prefs = currentPrefs
                val shouldBlock = DomainFilterRepository.isDomainBlocked(
                    host = queriedDomain,
                    blockSocial = prefs.webBlockSocialApps,
                    blockAdult = prefs.webBlockAdultContent,
                    blockCasinos = prefs.webBlockOnlineCasinos,
                    blockGaming = prefs.webBlockCasualGaming,
                    blockSports = prefs.webBlockSportsBetting,
                    customDomains = prefs.customBlockedDomains,
                    whitelistedDomains = prefs.webWhitelistedDomains
                )

                if (shouldBlock) {
                    val nxResponse = buildDnsNxDomainResponse(packet, ihl, dnsOffset, length)
                    if (nxResponse != null) {
                        output.write(nxResponse)
                        output.flush()
                    }
                }
            }
        } catch (_: Exception) {
            // Closed cleanly when VPN stops
        } finally {
            isRunning = false
        }
    }

    private fun extractDnsQueryDomain(packet: ByteArray, dnsOffset: Int, totalLength: Int): String? {
        var pos = dnsOffset + 12
        if (pos >= totalLength) return null
        val sb = StringBuilder()
        while (pos < totalLength) {
            val labelLen = packet[pos].toInt() and 0xFF
            if (labelLen == 0) break
            if ((labelLen and 0xC0) == 0xC0) return null
            pos++
            if (pos + labelLen > totalLength) return null
            if (sb.isNotEmpty()) sb.append('.')
            for (i in 0 until labelLen) {
                sb.append((packet[pos + i].toInt() and 0xFF).toChar())
            }
            pos += labelLen
        }
        return sb.toString().lowercase()
    }

    private fun buildDnsNxDomainResponse(
        request: ByteArray,
        ihl: Int,
        dnsOffset: Int,
        totalLength: Int
    ): ByteArray? {
        if (totalLength <= dnsOffset + 12) return null
        val resp = request.copyOf(totalLength)

        // Swap IPv4 source and destination IP addresses (bytes 12..15 <-> 16..19)
        for (i in 0..3) {
            val tmp = resp[12 + i]
            resp[12 + i] = resp[16 + i]
            resp[16 + i] = tmp
        }

        // Swap UDP source and destination ports
        val srcP0 = resp[ihl]
        val srcP1 = resp[ihl + 1]
        resp[ihl] = resp[ihl + 2]
        resp[ihl + 1] = resp[ihl + 3]
        resp[ihl + 2] = srcP0
        resp[ihl + 3] = srcP1

        // Clear UDP checksum (optional in IPv4)
        resp[ihl + 6] = 0
        resp[ihl + 7] = 0

        // Set DNS Flags: QR=1 (response), RCODE=3 (NXDOMAIN) -> 0x8183
        resp[dnsOffset + 2] = 0x81.toByte()
        resp[dnsOffset + 3] = 0x83.toByte()

        // Recompute IPv4 header checksum
        resp[10] = 0
        resp[11] = 0
        var sum = 0
        for (i in 0 until ihl step 2) {
            val word = ((resp[i].toInt() and 0xFF) shl 8) or (resp[i + 1].toInt() and 0xFF)
            sum += word
        }
        while ((sum ushr 16) != 0) {
            sum = (sum and 0xFFFF) + (sum ushr 16)
        }
        val checksum = sum.inv() and 0xFFFF
        resp[10] = ((checksum ushr 8) and 0xFF).toByte()
        resp[11] = (checksum and 0xFF).toByte()

        return resp
    }

    private fun stopVpnLoop() {
        isRunning = false
        try {
            vpnInterface?.close()
        } catch (_: Exception) {}
        vpnInterface = null
    }

    private fun createVpnNotification(): Notification {
        val channelId = "socialbreak_vpn_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Web Blocking",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("SocialLite Web Blocking")
            .setContentText("Filtering blocked sites on this device.")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopVpnLoop()
        serviceScope.cancel()
    }
}
