package io.github.wuttttta.uwb

import android.util.Log

/** Every log line uses one tag so `adb logcat -s POE` shows the whole story. */
const val TAG = "POE"

object PoeLog {
    fun i(msg: String) { Log.i(TAG, msg) }
    fun w(msg: String, t: Throwable? = null) { Log.w(TAG, msg, t) }
    fun e(msg: String, t: Throwable? = null) { Log.e(TAG, msg, t) }

    /** Secrets are logged only when the DEBUG_LOG_SECRETS build flag is on (never in shipped builds). */
    fun secret(label: String, value: () -> String) {
        if (BuildConfig.DEBUG_LOG_SECRETS) Log.w(TAG, "SECRET $label=${value()}")
    }
}
