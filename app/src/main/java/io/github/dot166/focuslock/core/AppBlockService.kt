package io.github.dot166.focuslock.core

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Process
import android.util.Log
import androidx.core.app.NotificationCompat
import io.github.dot166.focuslock.R
import io.github.dot166.focuslock.ui.activity.BlockScreenActivity
import io.github.dot166.focuslock.ui.activity.MainActivity
import io.github.dot166.focuslock.utils.UsageUtils

class AppBlockService : Service() {
    private var backgroundHandlerThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null
    private var currentAppToCheck: String? = null
    private var restrictedApps: List<RestrictedApp> = mutableListOf()

    companion object {
        const val ACTION_APP_SWITCH = "ACTION_APP_SWITCH"
        const val EXTRA_PACKAGE_NAME = "PACKAGE_NAME"
        const val BLOCKER_CHANNEL_ID = "AppBlockServiceChannel"
        const val NOTIFICATION_ID = 1
    }

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID, createNotification())
        backgroundHandlerThread = HandlerThread(
            "AppBlockServiceWorker",
            Process.THREAD_PRIORITY_BACKGROUND
        )
        backgroundHandlerThread!!.start()
        backgroundHandler = Handler(backgroundHandlerThread!!.looper)
    }

    private fun createNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0, notificationIntent, PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, BLOCKER_CHANNEL_ID)
        .setContentTitle("FocusLock is Active")
        .setContentText("Monitoring app usage to help you stay focused.")
        .setSmallIcon(R.drawable.hourglass_empty_24px)
        .setContentIntent(pendingIntent)
        .setOngoing(true)
        .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(javaClass.simpleName, "Service Command Received")
        restrictedApps = RestrictedApp.getRestrictedApps()
        when (intent?.action) {
            ACTION_APP_SWITCH -> {
                val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME)
                if (packageName != null) {
                    handleAppSwitch(packageName)
                }
            }
            else -> {
                Log.i(javaClass.simpleName, "Service Started")
            }
        }
        return START_STICKY
    }

    private fun handleAppSwitch(newPackageName: String) {
        backgroundHandler?.removeCallbacks(checkRunnable)
        val isRestricted = restrictedApps.any {
            it.packageName == newPackageName && it.allowedTimeInMinutes > -1
        }
        if (isRestricted) {
            Log.d(javaClass.simpleName, "Monitoring app: $newPackageName")
            currentAppToCheck = newPackageName
            backgroundHandler?.post(checkRunnable)
        } else {
            Log.d(javaClass.simpleName, "Not monitoring app: $newPackageName")
            currentAppToCheck = null
        }
    }

    private val checkRunnable = object : Runnable {
        override fun run() {
            val topApp = currentAppToCheck
            if (topApp == null) {
                backgroundHandler?.removeCallbacks(this)
                return
            }
            val usages = UsageUtils.getUsages(this@AppBlockService, false)
            val hashmap = hashMapOf<String, Long>()
            for (element in usages) {
                val totalTime = element.second!!
                val minutes = (totalTime / (1000 * 60))
                hashmap[element.first!!] = minutes
            }
            for (restrictedApp in restrictedApps) {
                if (restrictedApp.packageName != topApp) continue
                if (hashmap[restrictedApp.packageName] == null) {
                    hashmap[restrictedApp.packageName] = 0
                }
                if (restrictedApp.allowedTimeInMinutes.toInt() != -1 &&
                    hashmap[restrictedApp.packageName]!! >= restrictedApp.allowedTimeInMinutes) {
                    val intent = Intent(this@AppBlockService, BlockScreenActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    startActivity(intent)
                    backgroundHandler?.removeCallbacks(this)
                    return
                }
            }
            backgroundHandler?.postDelayed(this, 1000)
        }
    }

    override fun onDestroy() {
        backgroundHandler?.removeCallbacks(checkRunnable)
        stopForeground(STOP_FOREGROUND_REMOVE)
        Log.i(javaClass.simpleName, "Service Stopped")
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
