package io.github.dot166.focuslock.utils

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Context.APP_OPS_SERVICE
import android.content.Context.USAGE_STATS_SERVICE
import android.os.Process
import android.util.Log
import java.util.Calendar
import java.util.TimeZone

object UsageUtils {

    fun getUsages(ctx: Context, isMonitoring: Boolean = false): List<Pair<String?, Long?>> {
        val usm = ctx.getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager
        val calendar = Calendar.getInstance()
        calendar.timeZone = TimeZone.getDefault()

        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startTime = calendar.timeInMillis

        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endTime = calendar.timeInMillis

        val usageEvents = usm.queryEvents(startTime, endTime)
        val appUsageTimeMap: MutableMap<String?, Long?> = HashMap()
        val foregroundEventTimeMap: MutableMap<String?, Long?> = HashMap()
        val currentEvent = UsageEvents.Event()

        while (usageEvents.getNextEvent(currentEvent)) {
            val packageName = currentEvent.packageName
            val eventTime = currentEvent.timeStamp
            if (currentEvent.eventType == UsageEvents.Event.ACTIVITY_RESUMED ||
                currentEvent.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND
            ) {
                foregroundEventTimeMap[packageName] = eventTime
            } else if (currentEvent.eventType == UsageEvents.Event.ACTIVITY_PAUSED ||
                currentEvent.eventType == UsageEvents.Event.MOVE_TO_BACKGROUND
            ) {
                if (foregroundEventTimeMap.containsKey(packageName)) {
                    val startTime: Long = foregroundEventTimeMap[packageName]!!
                    val timeInForeground = eventTime - startTime
                    val currentTotal: Long = appUsageTimeMap.getOrDefault(packageName, 0L)!!
                    appUsageTimeMap[packageName] = currentTotal + timeInForeground
                    foregroundEventTimeMap.remove(packageName)
                }
            }
        }
        for (entry in foregroundEventTimeMap.entries) {
            val packageName = entry.key
            val startTime: Long = entry.value!!
            val timeInForeground = System.currentTimeMillis() - startTime
            val currentTotal: Long = appUsageTimeMap.getOrDefault(packageName, 0L)!!
            appUsageTimeMap[packageName] = currentTotal + timeInForeground
        }
        var totalScreenTimeMs: Long = 0
        for (time in appUsageTimeMap.values) {
            totalScreenTimeMs += time!!
        }
        val stats = appUsageTimeMap.toList()
        for (element in stats) {
            Log.i(javaClass.simpleName, element.first.toString() + "=" + element.second)
        }
        if (stats.isNotEmpty()) {
            val filteredStats = stats.filter { usageStats ->
                BlockUtils.isAllowedToMonitor(ctx, usageStats.first!!, isMonitoring)
            }.filter { usageStats ->
                usageStats.second!! > 0
            }
            // Sort apps by total time used in descending order (most used apps first)
            val sortedStats = filteredStats.sortedByDescending { it.second }
            return sortedStats
        }
        return emptyList()
    }


    fun hasUsageAccess(ctx: Context): Boolean {
        val appOps = ctx.getSystemService(APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            ctx.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }
}