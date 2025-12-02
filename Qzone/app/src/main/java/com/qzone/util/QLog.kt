package com.qzone.util

import android.util.Log

/**
 * Lightweight logging helper that keeps the verbose statements behind DEBUG
 * checks and gives us a single place to standardize formatting.
 */
object QLog {
    @PublishedApi
    internal const val DEFAULT_TAG = "Qzone"

    @PublishedApi
    @Volatile
    internal var isEnabled: Boolean = true

    fun enableLogging(enabled: Boolean) {
        isEnabled = enabled
    }

    inline fun d(tag: String = DEFAULT_TAG, crossinline message: () -> String) {
        if (!isEnabled) return
        Log.d(tag, message())
    }

    inline fun i(tag: String = DEFAULT_TAG, crossinline message: () -> String) {
        if (!isEnabled) return
        Log.i(tag, message())
    }

    inline fun w(tag: String = DEFAULT_TAG, crossinline message: () -> String) {
        if (!isEnabled) return
        Log.w(tag, message())
    }

    inline fun e(tag: String = DEFAULT_TAG, throwable: Throwable? = null, crossinline message: () -> String) {
        if (!isEnabled) return
        Log.e(tag, message(), throwable)
    }
}

