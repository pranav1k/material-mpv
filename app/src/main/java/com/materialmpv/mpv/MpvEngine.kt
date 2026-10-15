package com.materialmpv.mpv

import android.content.Context
import android.util.Log
import android.view.Surface
import `is`.xyz.mpv.MPVLib
import java.io.File
import java.io.FileOutputStream

object MpvEngine {
    private const val TAG = "MpvEngine"
    private var isInitialized = false

    /**
     * Initializes the native libmpv engine with performance-focused defaults:
     * - Fast profile (drops slow shaders)
     * - Direct Android hardware decoding (mediacodec / mediacodec-copy)
     * - Android GPU context directly mapped to SurfaceView
     * - OpenSL / AudioTrack audio output
     */
    @Synchronized
    fun initialize(context: Context) {
        if (isInitialized) {
            Log.d(TAG, "MpvEngine is already initialized")
            return
        }

        val appContext = context.applicationContext
        val configDir = File(appContext.filesDir, "mpv_config").apply { mkdirs() }.absolutePath
        val cacheDir = appContext.cacheDir.absolutePath

        copyAssetIfNeeded(appContext, "cacert.pem", File(configDir, "cacert.pem"))
        copyAssetIfNeeded(appContext, "subfont.ttf", File(configDir, "subfont.ttf"))

        Log.i(TAG, "Creating native MPVLib instance...")
        MPVLib.create(appContext)

        // Core configuration
        MPVLib.setOptionString("config", "yes")
        MPVLib.setOptionString("config-dir", configDir)
        MPVLib.setOptionString("gpu-shader-cache-dir", cacheDir)
        MPVLib.setOptionString("icc-cache-dir", cacheDir)

        // Performance & Hardware acceleration options
        MPVLib.setOptionString("profile", "fast")
        MPVLib.setOptionString("vo", "gpu")
        MPVLib.setOptionString("gpu-context", "android")
        MPVLib.setOptionString("opengl-es", "yes")
        MPVLib.setOptionString("hwdec", "mediacodec,mediacodec-copy")
        MPVLib.setOptionString("ao", "audiotrack,opensles")

        // Subtitle & TLS settings
        val subfont = File(configDir, "subfont.ttf")
        if (subfont.exists()) {
            MPVLib.setOptionString("sub-font-provider", "none")
            MPVLib.setOptionString("sub-font", subfont.absolutePath)
        }
        val cacert = File(configDir, "cacert.pem")
        if (cacert.exists()) {
            MPVLib.setOptionString("tls-ca-file", cacert.absolutePath)
        }

        // Initialize libmpv core
        MPVLib.init()

        // Post-init defaults
        MPVLib.setOptionString("force-window", "no")
        MPVLib.setOptionString("idle", "once")

        isInitialized = true
        Log.i(TAG, "Native MPVLib initialized successfully")
    }

    fun attachSurface(surface: Surface) {
        if (!isInitialized) return
        Log.d(TAG, "Attaching Surface to libmpv")
        MPVLib.attachSurface(surface)
        MPVLib.setOptionString("force-window", "yes")
    }

    fun updateSurfaceSize(width: Int, height: Int) {
        if (!isInitialized) return
        MPVLib.setPropertyString("android-surface-size", "${width}x${height}")
    }

    fun detachSurface() {
        if (!isInitialized) return
        Log.d(TAG, "Detaching Surface from libmpv")
        MPVLib.setOptionString("force-window", "no")
        MPVLib.detachSurface()
    }

    fun loadFile(path: String) {
        if (!isInitialized) return
        Log.d(TAG, "Loading file: $path")
        MPVLib.command("loadfile", path)
    }

    fun seekRelative(seconds: Double) {
        if (!isInitialized) return
        MPVLib.command("seek", seconds.toString(), "relative")
    }

    fun seekAbsolute(seconds: Double) {
        if (!isInitialized) return
        MPVLib.command("seek", seconds.toString(), "absolute")
    }

    fun togglePause() {
        if (!isInitialized) return
        MPVLib.command("cycle", "pause")
    }

    fun setPause(pause: Boolean) {
        if (!isInitialized) return
        MPVLib.setPropertyBoolean("pause", pause)
    }

    fun setSpeed(speed: Double) {
        if (!isInitialized) return
        MPVLib.setPropertyDouble("speed", speed)
    }

    fun setAudioTrack(id: Int) {
        if (!isInitialized) return
        if (id == -1) {
            MPVLib.setPropertyString("aid", "no")
        } else {
            MPVLib.setPropertyInt("aid", id)
        }
    }

    fun setSubtitleTrack(id: Int) {
        if (!isInitialized) return
        if (id == -1) {
            MPVLib.setPropertyString("sid", "no")
        } else {
            MPVLib.setPropertyInt("sid", id)
        }
    }

    fun addObserver(observer: MPVLib.EventObserver) {
        MPVLib.addObserver(observer)
    }

    fun removeObserver(observer: MPVLib.EventObserver) {
        MPVLib.removeObserver(observer)
    }

    fun observeProperty(property: String, format: Int) {
        MPVLib.observeProperty(property, format)
    }

    fun destroy() {
        if (!isInitialized) return
        try {
            MPVLib.destroy()
        } catch (e: Exception) {
            Log.e(TAG, "Error destroying MPVLib", e)
        } finally {
            isInitialized = false
        }
    }

    private fun copyAssetIfNeeded(context: Context, assetName: String, destFile: File) {
        if (destFile.exists() && destFile.length() > 0) return
        try {
            context.assets.open(assetName).use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Optional asset $assetName not copied: ${e.message}")
        }
    }
}
