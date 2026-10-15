# Light & Fast Material 3 (Material You) mpv Player for Android

A lightweight, modern, hardware-accelerated video player architecture for Android pairing **Jetpack Compose (Material 3)** with the native **`libmpv` JNI bridge** (`is.xyz.mpv.MPVLib`) rendered onto a bare **`SurfaceView`**.

---

## 1. Core Architecture & Highlights

| Layer | Component | Technical Benefit |
| :--- | :--- | :--- |
| **Native Engine** | `libmpv` + `JNI` (`MPVLib`) | Hardware-accelerated decoding (`hwdec=mediacodec,mediacodec-copy`) with full `libass` subtitle rendering. |
| **Video Surface** | `SurfaceView` via `AndroidView` | Renders directly to a hardware overlay buffer (`gpu-context=android`), bypassing Compose recomposition and avoiding `TextureView`'s extra GPU copy. |
| **UI & Theming** | **Jetpack Compose + Material 3** | Dynamic color theming (`dynamicDarkColorScheme` / Material You), zero XML bloat, and reactive `StateFlow` state management. |
| **Optimized Recomposition** | **Lambda State Providers** | The Seekbar slider reads `positionProvider: () -> Float`, restricting 1Hz/10Hz timestamp ticks strictly to the seekbar without recomposing the player UI. |
| **Size & Speed** | **arm64-v8a + R8 Full Mode** | Filtered native ABIs to `arm64-v8a` cut package size by ~60%, paired with R8 full mode tree shaking. |

---

## 2. Project Layout

```
mpvandroid/
├── app/
│   ├── build.gradle.kts                # Configured with arm64-v8a ABI filters & R8
│   ├── proguard-rules.pro              # Preserves MPVLib JNI methods and callbacks
│   └── src/main/
│       ├── AndroidManifest.xml         # Declares hardware acceleration & configChanges
│       ├── assets/                     # Default subfont (subfont.ttf) & SSL certs (cacert.pem)
│       ├── jniLibs/arm64-v8a/          # Prebuilt libmpv, libavcodec, libavformat, etc.
│       ├── res/                        # Themes, styles, icons
│       └── java/com/example/mpvcompose/
│           ├── MainActivity.kt         # Edge-to-edge entry point & intent handler
│           ├── mpv/
│           │   └── MpvEngine.kt        # libmpv lifecycle, options & hardware decoder presets
│           ├── presentation/
│           │   ├── theme/              # Material 3 colors, dynamic colors & typography
│           │   ├── home/
│           │   │   └── HomeScreen.kt   # SAF file picker & stream URL loader
│           │   └── player/
│           │       ├── MpvViewport.kt  # Bare SurfaceView in AndroidView
│           │       ├── PlayerViewModel.kt # MPVLib.EventObserver with StateFlows
│           │       ├── PlayerScreen.kt # Immersive mode, screen-on lock, and view orchestration
│           │       └── components/
│           │           ├── GestureSurface.kt       # Double tap seek & vertical brightness/volume drag
│           │           ├── GestureIndicator.kt     # HUD overlay for volume/brightness/seeking
│           │           ├── IsolatedSeekBar.kt      # Isolated recomposition slider
│           │           ├── PlayerControlsOverlay.kt # Material 3 styled controls
│           │           └── QuickSettingsSheet.kt   # Audio & subtitle track switcher + speed controls
│           └── utils/
│               ├── TimeFormatter.kt
│               └── WindowBrightness.kt
├── gradle/
│   ├── libs.versions.toml              # Version catalog
│   └── wrapper/
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties                   # R8 full mode & build caches enabled
```

---

## 3. Key Implementation Details

### A. High Performance Native Flags (`MpvEngine.kt`)
Before initializing the engine, performance-focused flags are passed to `MPVLib`:
```kotlin
MPVLib.create(appContext)
MPVLib.setOptionString("config", "yes")
MPVLib.setOptionString("config-dir", configDir)
MPVLib.setOptionString("profile", "fast")                  // Drops heavy post-processing shaders
MPVLib.setOptionString("vo", "gpu")
MPVLib.setOptionString("gpu-context", "android")
MPVLib.setOptionString("opengl-es", "yes")
MPVLib.setOptionString("hwdec", "mediacodec,mediacodec-copy") // Hardware acceleration
MPVLib.setOptionString("ao", "audiotrack,opensles")
MPVLib.init()
```

### B. Bare `SurfaceView` Video Viewport (`MpvViewport.kt`)
```kotlin
AndroidView(
    modifier = viewModifier,
    factory = { context ->
        SurfaceView(context).apply {
            holder.addCallback(object : SurfaceHolder.Callback {
                override fun surfaceCreated(holder: SurfaceHolder) {
                    MpvEngine.attachSurface(holder.surface)
                }
                override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                    MpvEngine.updateSurfaceSize(width, height)
                }
                override fun surfaceDestroyed(holder: SurfaceHolder) {
                    MpvEngine.detachSurface()
                }
            })
        }
    }
)
```

### C. Isolate Recomposition Pattern (`IsolatedSeekBar.kt`)
```kotlin
@Composable
fun IsolatedSeekBar(
    positionProvider: () -> Float,
    durationProvider: () -> Float,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    // Only this composable reads positionProvider(), so 1-second ticks
    // do not trigger recomposition of the parent controls overlay.
    val duration = durationProvider()
    val position = positionProvider()
    ...
}
```

### D. Touch Gestures (`GestureSurface.kt`)
- **Single Tap:** Toggles visibility of controls with an auto-hide timer (3.5s).
- **Double Tap Left/Right:** Relative seeks (`-10s` / `+10s`).
- **Vertical Drag Left Half:** Adjusts window brightness via `window.attributes.screenBrightness`.
- **Vertical Drag Right Half:** Adjusts media audio stream via `AudioManager.STREAM_MUSIC`.

---

## 4. Building the Project

Ensure you have Android SDK 35 and JDK 17 installed, then execute:

```bash
# Build arm64-v8a debug APK
./gradlew assembleDebug

# Build optimized release APK with R8 Full Mode
./gradlew assembleRelease
```
