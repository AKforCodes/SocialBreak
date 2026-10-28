# SocialBreak

> **Reclaim your attention.** 100% on-device digital wellness and short-form blocker for Android.

SocialBreak surgically eliminates addictive algorithmic loops—Instagram Reels, YouTube Shorts, infinite feeds, and toxic comment sections—while preserving authentic, intentional human connection (DMs, intentional search, and friend-sent clips).

---

## Key Highlights

- **100% On-Device & Zero Server Telemetry**: Operates completely host-isolated. Zero network calls, zero analytics SDKs, zero cloud dependencies. All daily statistics, schedules, and filters reside strictly in local Jetpack DataStore and Room SQLite on your phone.
- **The 8 Zero-Server Subsystems**:
  1. **Quick Settings Tiles (`TileService`)**: Toggle Focus Sprint, Sleep Mode, and Hard Mode straight from the Android notification shade with zero latency.
  2. **Hardware Daltonizer Display Grayscale (`Settings.Secure`)**: Strips bright dopamine colors across the entire Android OS with automatic CSS grayscale fallback.
  3. **Dynamic Launcher Shortcuts (`ShortcutManager`)**: Long-press launcher shortcuts for Instant 25m Focus Sprint, Sleep Mode, Clean Instagram, and Clean YouTube.
  4. **On-Device Real App Usage Tracking (`UsageStatsManager`)**: Privacy-first hourly query engine powering 3 home dials and the 7-petal floral radial achievement chart without network access.
  5. **Advanced DOM Content Strippers & Terminal Feed Breakers (`assets/*.js`)**: Strips Reels, Shorts, algorithmic feeds, toxic comments, and limits infinite scroll to 3 posts.
  6. **4-Stage Friction Gauntlet Hardening**: Procedural arithmetic `(A * B) - C = ?`, Levenshtein anti-paste sentence verification, and monotonic hardware clock (`SystemClock.elapsedRealtime()`) cooldown.
  7. **Encrypted Local Backup & Data Sovereignty (`LocalBackupManager.kt`)**: AES-256-GCM local encrypted backup export and import using a user-chosen PIN.
  8. **Interactive Smartphone Web Simulator Parity (`web/index.html`, `style.css`, `app.js`)**: 1:1 visual and functional parity with the 5 native tabs, interactive sleep dial, feed cleaner demo, and playable 4-stage friction gauntlet.
- **Ultra-Low Memory & Battery Drain**: Debounced event loop (< 30MB RAM PSS) with 0% CPU idle impact.
- **Local DNS Sinkhole (TUN Interface)**: Local `VpnService` drops 108 blacklisted domains across 5 categories (16 Social Apps, 21 Casual Gaming Portals, 24 Online Casinos, 22 Sports Betting Sites, 25 Adult Domains) locally at `0.0.0.0` with zero network hops.
- **The "Friend Content" Exception**: Multi-lingual conversation breadcrumb detection permits watching friend-sent clips while trapping infinite swipe-up reels.
- **Anti-Tampering Device Admin & Hard Mode**: Native Android `DeviceAdminReceiver` prevents impulsive uninstallation during active Focus or Sleep schedules.

---

## 5-Tab Architecture (1:1 Native Parity)

1. **Sleep Tab (`tab_sleep`)**: 24-hour circular arc dial, scheduled bedtime/wakeup, wind-down transitions, and scheduled grayscale.
2. **Blocking Tab (`tab_blocking`)**: Full app blocking categories, 25m/1h/2h quick presets, and 5-category 108-domain web filter drawer.
3. **Home Dashboard (`tab_home`)**: Real-time foreground blocker nodes, today's shield count, and Hard Mode trigger.
4. **WebApps Tab (`tab_webapps`)**: Clean, distraction-free web clients (Instagram Clean with DM deep links, YouTube Clean with background audio playback).
5. **Profile Tab (`tab_profile`)**: 7-petal floral radial achievement streak chart, system Daltonizer toggle, Device Admin anti-tamper lock, and local AES-256 backup export/import.

---

## Repository Structure

```
SocialBreak/
├── app/                  # Native Android application (Jetpack Compose, Kotlin 2.0, SDK 35)
│   ├── src/main/assets/  # Distraction-cleaning JavaScript injection engines
│   ├── src/main/java/    # Accessibility, VPN, Notification, Quick Settings & Compose architecture
│   └── src/main/res/     # Layouts, Widget providers, and Device Admin descriptors
├── web/                  # Interactive YC-tier web platform & smartphone simulator
└── release/              # Ready-to-install Android APK packages (SocialBreak-v1.0.0.apk)
```

---

## Installation & Verification

Download the verified release APK directly from `release/SocialBreak-v1.0.0.apk` (12.1 MB Release) or install via ADB:

```bash
adb install release/SocialBreak-v1.0.0.apk
```

To compile from source:
```bash
./gradlew assembleRelease
```

---

## License

MIT License. Designed and developed with privacy, digital wellness, and data sovereignty at the core.
