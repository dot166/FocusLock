package io.github.dot166.focuslock.utils

import android.app.usage.UsageStats
import android.content.Context

object BlockUtils {

    val blockList = arrayOf("com.google.android.apps.restore", "com.google.android.dialer", "com.android.settings")

    fun isAllowedToMonitor(ctx: Context, pName: String, isMonitoring: Boolean = false): Boolean {
        if (pName == ctx.packageName) return false
        if (!isMonitoring) {
            if (blockList.contains(pName)) return false
        }
        return ctx.packageManager.getLaunchIntentForPackage(pName) != null
    }
}