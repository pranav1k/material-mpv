package com.example.mpvcompose.utils

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager

object WindowBrightness {
    fun findActivity(context: Context): Activity? {
        var ctx = context
        while (ctx is ContextWrapper) {
            if (ctx is Activity) return ctx
            ctx = ctx.baseContext
        }
        return null
    }

    /**
     * Set the current window brightness between 0.01f and 1.0f.
     * Setting negative restores the system default brightness.
     */
    fun setBrightness(activity: Activity?, brightness: Float) {
        val window = activity?.window ?: return
        val lp = window.attributes
        lp.screenBrightness = brightness.coerceIn(0.01f, 1.0f)
        window.attributes = lp
    }

    fun getBrightness(activity: Activity?): Float {
        val window = activity?.window ?: return 0.5f
        val current = window.attributes.screenBrightness
        return if (current < 0f) 0.5f else current
    }
}
