package io.github.dot166.focuslock

import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.preference.PreferenceManager
import io.github.dot166.focuslock.ui.activity.BlockScreenActivity

class AppBlockService : Service() {
    private val handler = Looper.myLooper()?.let { Handler(it) }
    private var blockedApps: List<String> = mutableListOf()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        blockedApps = intent?.getStringArrayListExtra("blockedApps") ?: PreferenceManager.getDefaultSharedPreferences(this).getStringSet("blockedApps",
            mutableSetOf<String>())!!.toList()
        handler?.post(checkRunnable)
        Log.i(javaClass.simpleName, "Service Started")
        return START_STICKY
    }

    private val checkRunnable = object : Runnable {
        override fun run() {
            val topApp = getForegroundApp()
            if (topApp in blockedApps) {
                val intent = Intent(this@AppBlockService, BlockScreenActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }
            handler?.postDelayed(this, 50) // Check every 0.05 seconds
        }
    }

    override fun onDestroy() {
        handler?.removeCallbacks(checkRunnable)
        Log.i(javaClass.simpleName, "Service Stopped")
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun getForegroundApp(): String? {
        val usm = getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager
        val time = System.currentTimeMillis()
        val events = usm.queryEvents(time - 2000, time)
        var lastApp: String? = null
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                lastApp = event.packageName
            }
        }
        return lastApp
    }
}