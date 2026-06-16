package io.github.dot166.focuslock.core

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.util.Log
import com.android.settingslib.datastore.SharedPreferencesStorage
import io.github.dot166.focuslock.ui.activity.LauncherActivity
import io.github.dot166.jlib.app.DefaultSharedPrefsManager

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val intentAction = intent.action
        if (Intent.ACTION_MY_PACKAGE_REPLACED == intentAction) {
            Log.i(
                javaClass.getSimpleName(),
                "Package has been replaced: " + context.packageName
            )
            toggleAppIcon(context)
            val intent = Intent(context, AppBlockService::class.java)
            context.startForegroundService(intent)
        } else if (Intent.ACTION_BOOT_COMPLETED == intentAction) {
            Log.i(javaClass.getSimpleName(), "Boot has been completed")
            toggleAppIcon(context)
            val intent = Intent(context, AppBlockService::class.java)
            context.startForegroundService(intent)
        } else if (Intent.ACTION_USER_INITIALIZE == intentAction) {
            Log.i(javaClass.getSimpleName(), "User Initialisation")
            toggleAppIcon(context)
            val intent = Intent(context, AppBlockService::class.java)
            context.startForegroundService(intent)
        }
    }

    companion object {
        fun toggleAppIcon(context: Context) {
            val appInfoFlags = context.applicationInfo.flags
            val isSystemApp = (appInfoFlags and ApplicationInfo.FLAG_SYSTEM) > 0
            Log.i(
                BootCompletedReceiver::class.java.getSimpleName(),
                "toggleAppIcon() : FLAG_SYSTEM = $isSystemApp"
            )
            context.packageManager.setComponentEnabledSetting(
                ComponentName(context, LauncherActivity::class.java),
                if (readShowLauncherIcon(
                        DefaultSharedPrefsManager.getSharedPreferencesStorage(
                            context
                        ), context
                    )
                )
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                else
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
        }

        fun readShowLauncherIcon(
            prefs: SharedPreferencesStorage,
            context: Context
        ): Boolean {
            if (!prefs.contains("show_icon_in_l3")) {
                val appInfo = context.applicationInfo
                val isApplicationInSystemImage =
                    (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                // Default value
                return !isApplicationInSystemImage
            }
            return prefs.getBoolean("show_icon_in_l3") ?: false
        }
    }
}
