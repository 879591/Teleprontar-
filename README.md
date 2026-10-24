# Teleprompter Pro — Premium Camera Studio & Floating Overlay

Teleprompter Pro is a professional, production-grade Android application built using Kotlin, Jetpack Compose, CameraX, and Android Foreground Services. It is engineered specifically for video creators, educators, journalists, and public speakers with full native Hindi Devanagari Unicode support.

---

## 🚀 Features

### 1. Script Management & Home Screen
- **Room Database Local Storage:** 100% offline-first persistent storage using Room DB. Scripts are preserved across restarts and uninstalls.
- **Hindi & English Typography:** Crisp rendering of Devanagari matras and mixed-language text with optimal line heights.
- **Search & Filter:** Instant search by title or script content; filter by All, Favorites, Hindi, and English.
- **Sort Options:** Sort by Last Modified date, Script Title (A-Z), or Estimated Reading Duration.
- **TXT Import/Export:** Import `.txt` files directly via Android Storage Access Framework and export scripts via the Android share sheet.
- **Preloaded Scripts:** Includes sample scripts in Hindi, English, and Mixed language for instant testing.

### 2. Full-Featured Script Editor
- Real-time word count and reading duration calculator (based on 130 WPM average delivery).
- Auto-save with debounce + manual Save button.
- Unsaved changes protection with discard/save confirmation dialog.
- Paste from clipboard with one tap.
- Direct launch buttons into **Prompter**, **Camera Studio**, or **Floating Window**.

### 3. Fullscreen Teleprompter Player
- **Smooth Auto-Scroll:** Framerate-synchronized scrolling with immediate speed adjustment (50 to 320 WPM) without losing reading position.
- **Mirror Mode:** Horizontal flip (`scaleX = -1f`) for professional glass beam-splitter teleprompter rigs.
- **Voice Follow Mode:** Speech recognition listener (in Hindi `hi-IN` and English `en-US`) that fuzzy-matches spoken words against the script and advances the text automatically.
- **Reading Guide Band:** Focused highlight band positioned in the upper-middle reading sweet spot.
- **Countdown Overlay:** Visual 3..2..1 countdown before scrolling starts.
- **Collapsible Control HUD:** Tap anywhere on screen to hide/show speed sliders, font size sliders, alignment toggles, and color pickers (White, Cyan, Amber, Emerald).

### 4. Camera Studio (Video Recording + Teleprompter HUD)
- **CameraX Video Recording:** Records high-definition video with synchronized microphone audio.
- **Lens-Adjacent Overlay:** Teleprompter is positioned right below the front camera lens to maintain natural direct eye contact with viewers.
- **Camera Controls:** Front and rear camera flipping, front camera preview mirroring, exposure compensation, and studio color tint filters (Cool, Warm, Studio, Monochrome).
- **In-Recording Controls:** Play/pause the script while recording; pause/resume video recording; live recording timer with pulsing red REC indicator.
- **Video Preview Dialog:** Review recorded video with built-in player, share to any app via FileProvider, or delete with confirmation.

### 5. Floating Teleprompter (Over Other Apps)
- **Native Android System Overlay:** Uses `SYSTEM_ALERT_WINDOW` and a Foreground Service (`FloatingTeleprompterService`) to display a movable, translucent floating teleprompter over any third-party app (YouTube, TikTok, Zoom, Instagram, or native Camera).
- **Overlay Controls:** Drag header anywhere on screen, Play/Pause, Speed +/- adjustments, Close, and Return to main app button.
- **In-App Floating Mode:** Fallback draggable and resizable teleprompter widget within the application.

### 6. Settings, Backup & Cloud Sync
- **Google Cloud Sync & OAuth Guide:** Complete configuration instructions for setting up Google Drive synchronization.
- **JSON Backup & Restore:** Export all scripts into a single JSON file to share or back up across devices.
- **Voice Follow Language:** Toggle between Hindi (`hi-IN`) and English (`en-US`).
- **Creator Tutorials:** Best practices for eye contact, speaking cadence, and prompter hardware.

---

## 🛠 Google OAuth & Cloud Sync Configuration Guide

1. Go to the [Google Cloud Console](https://console.cloud.google.com/).
2. Create or select your Google Cloud project.
3. In **APIs & Services > Credentials**, create an **OAuth 2.0 Client ID**:
   - Application type: **Android**
   - Package name: `com.aistudio.teleprompter.prokzt` (configured in `app/build.gradle.kts`)
   - SHA-1 certificate fingerprint: Obtain from your debug or release keystore.
4. In **OAuth Consent Screen**, add the scope:
   `https://www.googleapis.com/auth/drive.file`
5. The local Room database operates as the offline source of truth; when cloud credentials are provided, synchronization pushes and pulls script JSONs safely without destructive remote wipes.

---

## 🧪 Testing Checklist

- [x] Create a new script in Hindi and English.
- [x] Verify word count and estimated duration calculations.
- [x] Test auto-save and unsaved-change dialog.
- [x] Import `.txt` file into the editor.
- [x] Export and share script via system share sheet.
- [x] Verify fullscreen teleprompter playback, speed slider, and font size scaling.
- [x] Test Mirror Mode horizontal inversion.
- [x] Verify Voice Follow state transitions and fallback.
- [x] Verify Camera Studio preview, front/rear camera switching, and recording.
- [x] Test Video Preview playback, sharing, and deletion.
- [x] Test Floating Teleprompter overlay permission request and draggable window.
- [x] Verify JSON backup export.
