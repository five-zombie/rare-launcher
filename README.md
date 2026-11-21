# 🌌 Rare Launcer

> **Aether Cyberpunk & Minimalist Android Launcher**  
> A modern custom Android launcher built with **Jetpack Compose**, prioritizing minimalist futuristic aesthetics, lightweight performance without overheating, and gesture-driven multitasking.

---

## ✨ Key Features

- **⏱️ Horizon Clock & Battery Capsule**: Minimalist architectural digital clock with battery indicator, smart date display, and power-efficient charging status detection (0% background CPU usage).
- **🫧 Sticky Recent App Bubbles**:
  - Organic overlapping bubble cluster in the top-left corner when you return to the home screen.
  - Displays the most recently opened app cluster.
  - **1-Tap Dialog**: Tap the bubble to open a direct multitasking panel (*Switch App*, *Force Close / App Info*, or *Remove from recents*).
  - Includes automatic *Auto-Fade* (disappears automatically after 8 seconds).
- **📂 Workspace Directory & Fast Popover**:
  - 1 primary app directory card on the home screen (up to 4 chosen app slots).
  - Floating popover dialog to manage, add, or swap favorite app shortcuts instantly.
- **🔐 Biometric Hidden Vault & App Lock**:
  - Hide private apps from the main app drawer and secure them with fingerprint / PIN.
  - Per-app lock (*App Lock*) that stays enforced even when launched from notifications or Recent Apps.
- **🎵 Cyber Stream Online Music & Radio Player**:
  - **Swipe Left-to-Right**: Slide from left to open the cyber music streaming drawer.
  - Nikmati siaran radio online bebas royalti (synthwave, cyberpunk, lo-fi, ambient chillout) 24/7 langsung di launcher.
  - Fitur pencarian radio global gratis dan preview lagu tanpa API key berbayar.
  - Powered by **Media3 ExoPlayer**: Mendukung background audio playback saat membuka app lain atau layar mati.
  - Mini player interaktif dengan play/pause, next/prev, acak (shuffle), urut, dan playlist lokal.
- **📞 Communication & Speed Dial Hub**:
  - **Swipe Right-to-Left**: Slide from right to open the quick communication drawer.
  - Equipped with a **Slide-to-Call** safety confirmation (slide right to call) to prevent accidental dialing.
  - Integrated with Device Contacts and Recent Call Log.
- **⚡ Super Optimized for Low-End Chipsets**:
  - Animations rendered via RenderThread without 120Hz recursive recomposition overhead.
  - Adaptive Background Service: automatically disables IPC polling when no apps are locked in the Vault.
  - Preserves native system status bar / notification shade gestures.

---

## 🛠️ Tech Stack & Requirements

| Component | Specification |
|---|---|
| **Language** | Kotlin 2.2.10 |
| **UI Toolkit** | Jetpack Compose (BOM 2026.02.01) + Material 3 |
| **Min SDK** | Android 8.0 (API 26) |
| **Target / Compile SDK** | Android 16 (API 37) |
| **Build System** | Gradle 9.6.0 + Android Gradle Plugin 9.5.1 |
| **Java Toolchain** | JDK 21 / 25 |

---

## 🚀 Getting Started (Local Setup)

### 1. Clone the Repository
```bash
git clone https://github.com/rachmanzz/rarelauncer.git
cd rarelauncer
```

### 2. Build Debug APK
Make sure you have JDK 21+ and the Android SDK installed:
```bash
./gradlew assembleDebug
```
The debug APK will be generated at: `app/build/outputs/apk/debug/app-debug.apk`

### 3. Run Unit Tests
```bash
./gradlew testDebugUnitTest
```

### 4. Install on Device via ADB
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```
*Tip: Open **Device Settings > Apps > Default Apps > Home App** and select **Rare Launcer** as your default launcher.*

---

## ⚙️ CI/CD (GitHub Actions)

This repository includes automated pipelines:
- **CI (`.github/workflows/ci.yml`)**: Runs on every `push` and `pull_request` to the `main` branch to execute Unit Tests and compile the Debug APK.
- **CD (`.github/workflows/cd.yml`)**: Runs automatically when a new version tag is pushed (e.g., `v1.0.0`) or via manual trigger (*workflow_dispatch*):
  - Uses GitHub Environment `ANDROID_ENV` with Secret `ANDROID_APP_SECRET`.
  - Automatically generates an official 10-year (3650-day) certificate keystore for **The Five Zombie Org** (*Team Zombie*).
  - Performs official signing for:
    - 📦 **`.aab`** (Official Android App Bundle for Google Play Store).
    - 📱 **`-release.apk`** (Officially signed Release APK).
    - 🧪 **`-debug.apk`** (Testing APK).
    - 🔐 **`SHA256SUMS.txt`** (File integrity verification checksums).
  - Automatically publishes all artifacts directly to **GitHub Releases**.

---

## 📄 License & Contributors

Maintained and developed by **Zombie Teams** (`fivezombieorg@gmail.com`).  
Released under the MIT license or according to repository policy.
