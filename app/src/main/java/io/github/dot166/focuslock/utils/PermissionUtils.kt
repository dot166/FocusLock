package io.github.dot166.focuslock.utils

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.content.Context.POWER_SERVICE
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.text.TextUtils
import androidx.core.app.NotificationManagerCompat
import io.github.dot166.focuslock.core.AppBlockerAccessibilityService
import io.github.dot166.focuslock.utils.UsageUtils.hasUsageAccess
import kotlin.text.isEmpty

object PermissionUtils {
    fun isAccessibilityServiceEnabled(context: Context, serviceClass: Class<out AccessibilityService>): Boolean {
        val accessibilityEnabled = try {
            Settings.Secure.getInt(context.contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED)
        } catch (e: Settings.SettingNotFoundException) {
            e.printStackTrace()
            0
        }

        if (accessibilityEnabled == 0) {
            return false
        }

        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )

        if (enabledServices == null || enabledServices.isEmpty()) {
            return false
        }

        val selfComponent = ComponentName(context, serviceClass).flattenToString()
        val splitter = TextUtils.SimpleStringSplitter(':')
        splitter.setString(enabledServices)

        while (splitter.hasNext()) {
            if (splitter.next().equals(selfComponent, ignoreCase = true)) {
                return true
            }
        }

        return false
    }
    fun hasAllPermissions(ctx: Context): Boolean {
        val packageName = ctx.packageName
        val pm = ctx.getSystemService(POWER_SERVICE) as PowerManager
        val notifPermGranted: Boolean = if (Build.VERSION.SDK_INT_FULL >= Build.VERSION_CODES_FULL.TIRAMISU) {
            NotificationManagerCompat.from(ctx).areNotificationsEnabled()
        } else {
            // treat as granted
            true
        }
        return hasUsageAccess(ctx) && Settings.canDrawOverlays(ctx) && pm.isIgnoringBatteryOptimizations(packageName) && isAccessibilityServiceEnabled(ctx, AppBlockerAccessibilityService::class.java) && notifPermGranted
    }
}