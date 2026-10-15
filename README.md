# material-mpv

A lightweight, modern, hardware-accelerated video player architecture for Android pairing **Jetpack Compose (Material 3)** with the native **`libmpv` JNI bridge** (`is.xyz.mpv.MPVLib`) rendered onto a bare **`SurfaceView`**.

## Building the Project

Ensure you have Android SDK 35 and JDK 17 installed, then execute:

```bash
# Build arm64-v8a debug APK
./gradlew assembleDebug

# Build optimized release APK with R8 Full Mode
./gradlew assembleRelease
```
