# 🌌 Rare Launcer

> **Aether Cyberpunk & Minimalist Android Launcher**  
> Custom launcher modern berbasis Android yang dibangun dengan **Jetpack Compose**, mengutamakan estetika futuristik minimalis, performa ringan tanpa *overheating*, dan fitur multitasking berbasis gestur.

---

## ✨ Fitur Utama

- **⏱️ Horizon Clock & Battery Capsule**: Jam digital arsitektural minimalis dengan indikator baterai, tanggal cerdas, dan deteksi status pengisian daya yang hemat daya (0% CPU di latar belakang).
- **🫧 Sticky Recent App Bubbles (Balon Aplikasi)**:
  - Balon gelembung organik yang saling bersinggungan di pojok kiri atas saat Anda baru saja kembali ke layar beranda.
  - Menampilkan kluster aplikasi yang terakhir dibuka.
  - **1-Tap Dialog**: Ketuk balon untuk membuka panel multitasking langsung (*Switch App*, *Force Close / App Info*, atau *Hapus dari riwayat*).
  - Dilengkapi *Auto-Fade* otomatis (menghilang sendiri setelah 8 detik).
- **📂 Workspace Directory & Fast Popover**:
  - 1 kartu direktori aplikasi utama di layar beranda (maksimal 4 slot aplikasi pilihan).
  - Dialog popover mengambang untuk mengelola, menambah, atau mengganti shortcut aplikasi favorit secara instan.
- **🔐 Biometric Hidden Vault & App Lock**:
  - Sembunyikan aplikasi privat dari laci aplikasi utama dan amankan dengan sidik jari / PIN.
  - Fitur kunci aplikasi per-aplikasi (*App Lock*) yang tetap aktif saat dibuka lewat notifikasi atau switcher.
- **📞 Communication & Speed Dial Hub**:
  - Geser ke kanan (*swipe right*) untuk membuka laci komunikasi cepat.
  - Dilengkapi fitur pengaman **Slide-to-Call** (geser kanan untuk memanggil) agar terhindar dari salah pencet nomor.
  - Terintegrasi dengan Kontak Perangkat dan Riwayat Panggilan Cepat.
- **⚡ Super Optimized for Low-End Chipsets**:
  - Animasi dirender via RenderThread tanpa beban rekursif recomposition 120Hz.
  - Background Service adaptif: otomatis mematikan polling IPC saat tidak ada aplikasi yang dikunci di Vault.
  - Tidak memotong gestur status bar / notifikasi bawaan HP.

---

## 🛠️ Tech Stack & Requirements

| Komponen | Spesifikasi |
|---|---|
| **Bahasa** | Kotlin 2.2.10 |
| **UI Toolkit** | Jetpack Compose (BOM 2026.02.01) + Material 3 |
| **Min SDK** | Android 8.0 (API 26) |
| **Target / Compile SDK** | Android 16 (API 37) |
| **Build System** | Gradle 9.6.0 + Android Gradle Plugin 9.5.1 |
| **Java Toolchain** | JDK 21 / 25 |

---

## 🚀 Memulai (Local Setup)

### 1. Clone Repositori
```bash
git clone https://github.com/rachmanzz/rarelauncer.git
cd rarelauncer
```

### 2. Build Debug APK
Pastikan Anda memiliki JDK 21+ dan Android SDK:
```bash
./gradlew assembleDebug
```
File APK debug akan dihasilkan di: `app/build/outputs/apk/debug/app-debug.apk`

### 3. Jalankan Pengujian
```bash
./gradlew testDebugUnitTest
```

### 4. Pasang ke Perangkat via ADB
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```
*Tips: Buka **Pengaturan HP > Aplikasi > Aplikasi Default > Aplikasi Beranda (Home App)** dan pilih **Rare Launcer** sebagai peluncur utama.*

---

## ⚙️ CI/CD (GitHub Actions)

Repositori ini dilengkapi dengan pipeline otomatis:
- **CI (`.github/workflows/ci.yml`)**: Berjalan di setiap `push` dan `pull_request` pada branch `main` untuk menjalankan Unit Tests dan mengompilasi APK Debug.
- **CD (`.github/workflows/cd.yml`)**: Berjalan otomatis saat ada tag versi baru (misal `v1.0.0`) atau lewat manual trigger (*workflow_dispatch*):
  - Menggunakan Environment GitHub: `ANDROID_ENV` dengan Secret `ANDROID_APP_SECRET`.
  - Otomatis men-generate Keystore bersertifikat resmi 10 tahun (3650 hari) atas nama **The Five Zombie Org** (*Team Zombie*).
  - Melakukan signing resmi untuk:
    - 📦 **`.aab`** (Android App Bundle resmi untuk Google Play Store).
    - 📱 **`-release.apk`** (Release APK bertanda tangan resmi).
    - 🧪 **`-debug.apk`** (Testing APK).
    - 🔐 **`SHA256SUMS.txt`** (Checksum integritas berkas).
  - Otomatis mempublikasikan seluruh berkas tersebut langsung ke **GitHub Releases**.

---

## 📄 Lisensi & Kontributor

Dikelola dan dikembangkan oleh **Zombie Teams** (`fivezombieorg@gmail.com`).  
Dirilis di bawah lisensi MIT atau sesuai kebijakan repositori.
