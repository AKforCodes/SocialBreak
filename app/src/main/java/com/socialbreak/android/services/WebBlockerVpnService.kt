package com.socialbreak.android.services

import android.net.VpnService
import android.os.ParcelFileDescriptor
import java.io.FileInputStream
import java.io.FileOutputStream

class WebBlockerVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private var isRunning = false

    companion object {
        val ADULT_DOMAINS = setOf(
            "pornhub.com", "xvideos.com", "xnxx.com", "redtube.com", "youporn.com",
            "chaturbate.com", "onlyfans.com", "stripchat.com", "cam4.com", "livejasmin.com"
        )
        val CASINO_DOMAINS = setOf(
            "bet365.com", "pokerstars.com", "draftkings.com", "fanduel.com", "williamhill.com",
            "888casino.com", "stake.com", "roobet.com", "bovada.lv", "betway.com"
        )
        val GAMING_DOMAINS = setOf(
            "1001games.com", "addictinggames.com", "agame.com", "armorgames.com", "chess.com",
            "coolmathgames.com", "crazygames.com", "friv.com", "gamefaqs.com", "gamehouse.com",
            "kick.com", "kizi.com", "kongregate.com", "lichess.org", "miniclip.com",
            "poki.com", "roblox.com", "silvergames.com", "twitch.tv", "y8.com"
        )
        val SOCIAL_DOMAINS = setOf(
            "instagram.com", "tiktok.com", "facebook.com", "youtube.com", "twitter.com",
            "x.com", "reddit.com", "snapchat.com", "threads.net", "pinterest.com",
            "linkedin.com", "discord.com", "bereal.com", "tumblr.com"
        )
    }

    override fun onStartCommand(intent: android.content.Intent?, flags: Int, startId: Int): Int {
        if (!isRunning) {
            startVpn()
        }
        return START_STICKY
    }

    private fun startVpn() {
        try {
            val builder = Builder()
                .setSession("SocialBreak Web Blocker")
                .addAddress("10.0.0.2", 24)
                .addDnsServer("10.0.0.1")

            vpnInterface = builder.establish()
            isRunning = true

            // Local DNS loopback packet processing runs in background thread
            Thread {
                val input = FileInputStream(vpnInterface?.fileDescriptor)
                val output = FileOutputStream(vpnInterface?.fileDescriptor)
                val buffer = ByteArray(32767)

                while (isRunning) {
                    val length = input.read(buffer)
                    if (length > 0) {
                        // Inspect DNS query payload: if matching blocked domains, drop/NXDOMAIN
                    }
                }
            }.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        isRunning = false
        vpnInterface?.close()
        super.onDestroy()
    }
}
