package io.github.dot166.focuslock.utils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

object BlockUtils {

    val blockList = arrayOf("com.google.android.apps.restore", "com.google.android.dialer", "com.android.dialer", "com.android.settings", "com.android.angle", "com.android.traceur", "com.android.vending", "app.grapheneos.apps", "com.google.android.accessibility.switchaccess", "com.android.deskclock")

    val allowList = arrayOf("com.google.android.gms", "app.grapheneos.gmscompat", "com.android.egg", "com.android.captiveportallogin", "com.android.stk", "app.vanadium.webview")

    fun isAllowedToMonitor(ctx: Context, pName: String, isMonitoring: Boolean = false): Boolean {
        if (pName == ctx.packageName) return false
        if (!isMonitoring) {
            if (blockList.contains(pName)) return false
        } else {
            if (allowList.contains(pName)) return true
        }
        val launcherIntent = Intent(Intent.ACTION_MAIN)
        launcherIntent.addCategory(Intent.CATEGORY_HOME)
        launcherIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        if (ctx.packageManager.resolveActivity(launcherIntent, PackageManager.MATCH_DEFAULT_ONLY)!!.activityInfo.packageName == pName) {
            return false
        }
        return ctx.packageManager.getLaunchIntentForPackage(pName) != null
    }
}