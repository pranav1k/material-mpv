# Preserve native libmpv JNI bridge and callbacks
-keep class is.xyz.mpv.** { *; }
-keepclassmembers class is.xyz.mpv.** {
    native <methods>;
}

# Keep EventObserver and Node types used by MPVLib
-keep interface is.xyz.mpv.MPVLib$EventObserver { *; }
-keep interface is.xyz.mpv.MPVLib$LogObserver { *; }
-keep class is.xyz.mpv.MPVNode { *; }

# Preserve Compose runtime reflections and animations
-keepattributes *Annotation*
-dontwarn is.xyz.mpv.**
