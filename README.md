# SocialBreak

> **Reclaim your attention.** 100% on-device digital wellness and short-form blocker for Android.

SocialBreak surgically eliminates addictive algorithmic loops—Instagram Reels, YouTube Shorts, infinite feeds, and toxic comment sections—while preserving authentic, intentional human connection (DMs, intentional search, and friend-sent clips).

---

## Key Highlights

- **100% On-Device & Zero Server Telemetry**: Operates completely host-isolated. All daily statistics, app schedules, and DNS filters reside strictly in local Jetpack DataStore preferences on your phone.
- **Ultra-Low Memory & Battery Drain**: Uses a lightweight debounced event loop (< 30MB RAM PSS) with 0% CPU idle impact.
- **Local DNS Sinkhole**: Local loopback TUN VPN interface (`WebBlockerVpnService`) drops Adult, Casino, Gaming, and Distracting Social domains at `0.0.0.0` with zero network latency.
- **The "Friend Content" Loop**: Watch specific clips your friends send via direct message, but the infinite swipe-up dopamine feed is completely trapped and disabled.
- **Anti-Tampering Device Admin & Hard Mode**: Enforces an unskippable 4-tier friction gauntlet (commitment typing, 5 math questions, mandatory cooldown timer) to prevent impulsive uninstallation.
- **YC Consumer Dark Design**: Built with Jetpack Compose featuring 24-hour circular arc dials, interactive node graphs, and rich obsidian styling.

---

## Repository Structure

```
SocialBreak/
├── app/                  # Native Android application (Jetpack Compose, Kotlin 2.0, SDK 35)
│   ├── src/main/assets/  # Distraction-cleaning JavaScript injection engines
│   ├── src/main/java/    # Accessibility, VPN, Notification, and Compose UI architecture
│   └── src/main/res/     # Accessibility and Device Admin descriptors
├── web/                  # Interactive YC-tier web platform & smartphone simulator
└── release/              # Ready-to-install Android APK packages
```

---

## Installation

Download the verified release APK directly from [`release/SocialBreak-v1.0.0.apk`](./release/SocialBreak-v1.0.0.apk) or run:

```bash
adb install release/SocialBreak-v1.0.0.apk
```

---

## License

MIT License. Designed and developed with privacy and digital wellbeing at the core.
