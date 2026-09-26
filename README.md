# NetMuzzle 🛡️

<p align="center">
  <strong>English</strong> • <a href="README.pl.md">Polski</a>
</p>

<p align="center">
  <img src="https://raw.githubusercontent.com/mastai-dev/NetMuzzle/main/app/src/main/res/mipmap-xxxhdpi/ic_launcher.png" width="96" height="96" alt="NetMuzzle Logo" style="border-radius: 20px;" />
</p>

<p align="center">
  <strong>Ultra-lightweight, Zero-CPU Android Firewall & Mobile Game AdBlocker</strong><br>
  <em>No root required. Zero battery drain. No packet inspection loops. Pure kernel efficiency.</em>
</p>

<p align="center">
  <a href="https://github.com/mastai-dev/NetMuzzle/releases/latest"><img src="https://img.shields.io/badge/Release-v1.1.0-00E5FF?style=for-the-badge&logo=android&logoColor=white" alt="Latest Release"></a>
  <a href="https://github.com/mastai-dev/NetMuzzle/releases"><img src="https://img.shields.io/badge/APK%20Size-~1.2%20MB-success?style=for-the-badge" alt="APK Size"></a>
  <a href="https://android.com"><img src="https://img.shields.io/badge/Android-8.0%2B%20(Oreo%20to%2014%2B)-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Android Support"></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-MIT-blue.svg?style=for-the-badge" alt="License"></a>
  <a href="https://github.com/mastai-dev/NetMuzzle/actions"><img src="https://img.shields.io/badge/Build-Passing-brightgreen?style=for-the-badge&logo=githubactions&logoColor=white" alt="Build Status"></a>
</p>

<p align="center">
  <a href="https://github.com/mastai-dev/NetMuzzle/releases/download/v1.1.0/NetMuzzle-v1.1.0.apk">
    <img src="https://img.shields.io/badge/⬇️_Download_NetMuzzle_APK-v1.1.0_(Direct)-00E5FF?style=for-the-badge&labelColor=0f172a" alt="Download APK">
  </a>
  <a href="https://mastai-dev.github.io/NetMuzzle/">
    <img src="https://img.shields.io/badge/🌐_Official_Website-Live_Demo-7C3AED?style=for-the-badge&labelColor=0f172a" alt="Official Website">
  </a>
</p>

---

## ⚡ Why NetMuzzle?

Most Android firewall apps (such as NetGuard or generic VPN blockers) **route 100% of your device's traffic into user-space**, reading and writing every single packet in memory. This continuous processing burns CPU cycles, heats up your battery, and introduces network lag into games.

**NetMuzzle takes the opposite, pure architectural route:**

1. 🚀 **0% CPU Blackhole Routing:**  
   Allowed applications **completely bypass the VPN** directly at the Linux kernel level (`addAllowedApplication`). 99% of your traffic never touches our app.
2. 🎮 **Game AdBlock Shield (New in v1.1.0):**  
   Play mobile games without intrusive full-screen video ads. Our ultra-lightweight DNS shield blocks ad networks (Unity Ads, Google AdMob, AppLovin, IronSource, Mintegral, etc.) while keeping game servers, leaderboards, and multiplayer running at native hardware speeds.
3. 🎁 **Rewarded Ads Control:**  
   Want bonus coins or gems in your favorite game? Easily toggle specific ad networks (e.g. Unity Ads) on or off, or add your own custom ad domains with a single tap.
4. 🔒 **Watertight Zero-DNS-Leak Sink:**  
   Blocked applications receive dead local DNS servers (`10.0.0.1` and `fd00::2`). Queries fail immediately on-device without leaking domain lookups to your mobile carrier or ISP.

---

## 📊 How NetMuzzle Compares

| Feature / Metric | Traditional VPN Firewalls (e.g. NetGuard) | Root AdBlockers (AdAway / iptables) | **NetMuzzle 🛡️ (v1.1.0)** |
| :--- | :---: | :---: | :---: |
| **Requires Root Access?** | ❌ No | ⚠️ Yes (Voids warranty) | 🟢 **No Root Needed** |
| **Battery & CPU Overhead** | 🔴 5% – 15% (packet inspection) | 🟢 Negligible | 🟢 **Exactly 0% (Kernel Sink)** |
| **Online Mobile Gaming** | 🔴 Adds latency & breaks games | 🟡 Requires complex whitelist | 🟢 **Zero Latency (Game Shield)** |
| **Rewarded Ads Support** | ❌ All-or-nothing | ❌ Manual hosts file editing | 🟢 **1-Click Network Toggles** |
| **App Size** | 🟡 15 MB – 45 MB | 🟡 10 MB – 25 MB | 🟢 **Ultra-lightweight (~1.2 MB)** |
| **3-State Control per App** | ❌ Only 2 states (On/Off) | ❌ Global only | 🟢 **Bypass / Game Shield / Muzzle** |
| **Background Data Leaks** | 🟡 Risk of TLS proxy bypass | 🟢 Blocked | 🟢 **Watertight Loopback Sink** |
| **Source Code & Privacy** | 🟡 Proprietary / Analytics | 🟢 Open Source | 🟢 **100% Open Source (MIT)** |

---

## ✨ Features Overview

### 💊 3-State Segmented Capsule UI
Control internet access per app with our modern, tactile capsule:
* **Allow (Bypass):** The app communicates directly over Wi-Fi / LTE with zero VPN involvement.
* **Block Ads (Game Shield):** Game servers and multiplayer stay connected, but advertising networks (Unity Ads, AdMob, AppLovin, etc.) are silenced on-device.
* **Muzzle (Full Block):** The app is completely cut off from the internet and cast into a zero-CPU blackhole sink.

### ⚙️ AdBlock Filters Manager
* **Built-in Ad Networks:** Pre-configured rules for Unity Ads, Google AdMob, AppLovin, IronSource, Vungle (Liftoff), Mintegral, InMobi, Chartboost, and Pangle.
* **Granular Toggles:** Temporarily unblock Unity Ads to claim in-game rewards, then turn it back on.
* **Custom Domain Lists:** Add your own custom ad or tracking domains directly from the app interface.

### ⚡ Hero Master Protection Switch
* Prominent, interactive master switch on the main screen to activate or pause protection across all configured apps instantly.

### ❓ In-App Tutorial & Help Guide
* Integrated step-by-step dialog explaining each mode, gaming ad shields, and battery efficiency.

### 🌍 Seamless Bilingual Support
* Automatically detects your device language and displays native English or Polish.

---

## 🏗️ Architecture: How the 0% CPU Sink Works

```
                                [ Outgoing App Traffic ]
                                           │
         ┌─────────────────────────────────┴─────────────────────────────────┐
         ▼                                                                   ▼
[ UNBLOCKED APPS (99%) ]                                           [ PROTECTED APPS ]
         │                                                                   │
  Linux Kernel Level                                      ┌──────────────────┴──────────────────┐
  (addAllowedApplication)                                 ▼                                     ▼
         │                                      [ BLOCK ADS MODE ]                      [ MUZZLE MODE ]
         ▼                                                │                                     │
   Wi-Fi / 5G / LTE                               UDP Port 53 Only                              │
  (Native Full Speed)                                     │                                     ▼
   ZERO CPU OVERHEAD                                      ▼                             Virtual TUN Sink
                                                 DnsPacketHandler                          (10.0.0.1/32)
                                                 ┌────────┴────────┐                            │
                                                 ▼                 ▼                            ▼
                                            [ Ad Domain ]    [ Game Server ]              Packets Dropped
                                                 │                 │                      Instantly by OS
                                                 ▼                 ▼                     (0% CPU, 0 LAG)
                                            Blocked (0.0.0.0)  Forwarded via
                                                               protect(socket)
```

> [!NOTE]
> When all apps are either allowed or in full block, the background DNS handler thread stops completely. The TUN interface reverts to a pure blackhole loopback with zero userspace packet reads.

---

## 🚀 Quick Start / Installation

1. **Download:** Grab the latest `NetMuzzle-v1.1.0.apk` (~1.2 MB) from the [Releases](https://github.com/mastai-dev/NetMuzzle/releases/latest) page.
2. **Install:** Open the file on your Android device (Android 8.0 Oreo up to Android 14+). Enable "Install from unknown sources" if prompted.
3. **Grant VPN Permission:** Launch NetMuzzle and grant the standard one-time Android VPN permission. NetMuzzle runs entirely locally on your phone — no traffic ever leaves your device.
4. **Choose Your Rules:** Set your games to **Block Ads** and intrusive apps to **Muzzle**. Turn on the Hero Master Switch and enjoy clean, private browsing and gaming!

---

## 🛡️ Privacy & Transparent Monetization

* **Zero Telemetry:** No Google Firebase, no Crashlytics, no Facebook SDK, no user tracking.
* **100% Offline-Capable:** Core firewall logic requires no external servers.
* **Honest "Zero-Spam" Ad Policy:**
  * Free software requires sustainability. NetMuzzle utilizes Google App Open Ads with a strict **2-hour cooldown period**.
  * Restarting your device resets this period (first app launch after reboot shows an ad).
  * No annoying banners, no popups during configuration, and no video interruptions while toggling firewall rules.

---

## 🛠️ Building from Source

To build NetMuzzle locally from source:

```bash
# Clone the repository
git clone https://github.com/mastai-dev/NetMuzzle.git
cd NetMuzzle

# Build the release APK (requires Java 17 / Microsoft OpenJDK 17)
./gradlew assembleRelease
```

The generated, R8-optimized APK will be available at:  
`app/build/outputs/apk/release/app-release.apk`

---

## 👤 Author & Open Source Community

* **Creator & Lead Developer:** [Marcin Stankiewicz](https://github.com/mastai-dev)
* **License:** [MIT License](LICENSE) — free for personal and educational use.
* **Bug Reports & Feature Requests:** Please open an issue in the [GitHub Issue Tracker](https://github.com/mastai-dev/NetMuzzle/issues).

<p align="center">
  ⭐ <strong>If NetMuzzle saves your battery and blocks annoying game ads, please star this repository!</strong> ⭐
</p>
