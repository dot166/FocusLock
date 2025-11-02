package io.github.dot166.focuslock

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Intent
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import io.github.dot166.focuslock.ui.ScreenTimeGraphicPreference
import io.github.dot166.jlib.LIBAboutActivity
import io.github.dot166.jlib.app.jConfigActivity
import io.github.dot166.jlib.utils.VersionUtils.libVersion
import kotlinx.coroutines.DelicateCoroutinesApi
import java.util.Calendar
import java.util.TimeZone


class MainActivity: jConfigActivity() {
    override fun preferenceFragment(): PreferenceFragmentCompat {
        return PrefFragment()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!hasUsageAccess()) {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            Toast.makeText(this, "Please grant Usage Access permission", Toast.LENGTH_LONG).show()
        }
    }

    class PrefFragment : PreferenceFragmentCompat() {
        @OptIn(DelicateCoroutinesApi::class)
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            val screen = preferenceManager.createPreferenceScreen(requireContext())
            val graph = ScreenTimeGraphicPreference(requireContext())
            val usm = requireContext().getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager
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

            val map = mutableMapOf<String, Pair<Int, String>>()
            val usageEvents = usm.queryEvents(startTime, endTime)
            val appUsageTimeMap: MutableMap<String?, Long?> = HashMap<String?, Long?>()
            val foregroundEventTimeMap: MutableMap<String?, Long?> = HashMap<String?, Long?>()
            val currentEvent = UsageEvents.Event()

            while (usageEvents.getNextEvent(currentEvent)) {
                val packageName = currentEvent.packageName
                val eventTime = currentEvent.timeStamp
                if (currentEvent.eventType == UsageEvents.Event.ACTIVITY_RESUMED ||
                    currentEvent.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND
                ) {
                    foregroundEventTimeMap.put(packageName, eventTime)
                } else if (currentEvent.eventType == UsageEvents.Event.ACTIVITY_PAUSED ||
                    currentEvent.eventType == UsageEvents.Event.MOVE_TO_BACKGROUND
                ) {
                    if (foregroundEventTimeMap.containsKey(packageName)) {
                        val startTime: Long = foregroundEventTimeMap[packageName]!!
                        val timeInForeground = eventTime - startTime
                        val currentTotal: Long = appUsageTimeMap.getOrDefault(packageName, 0L)!!
                        appUsageTimeMap.put(packageName, currentTotal + timeInForeground)
                        foregroundEventTimeMap.remove(packageName)
                    }
                }
            }
            for (entry in foregroundEventTimeMap.entries) {
                val packageName = entry.key
                val startTime: Long = entry.value!!
                val timeInForeground = endTime - startTime
                val currentTotal: Long = appUsageTimeMap.getOrDefault(packageName, 0L)!!
                appUsageTimeMap.put(packageName, currentTotal + timeInForeground)
            }
            var totalScreenTimeMs: Long = 0
            for (time in appUsageTimeMap.values) {
                totalScreenTimeMs += time!!
            }
            val stats = appUsageTimeMap.toList()
            for (i in 0 until stats.size) {
                Log.i(javaClass.simpleName, stats[i].first.toString() + "=" + stats[i].second)
            }
            if (stats.isNotEmpty()) {
                val filteredStats = stats.filter{ usageStats ->
                    //BlockUtils.isAllowedToMonitor(requireContext(), usageStats.first!!, true)
                //}.filter { usageStats ->
                    usageStats.second!! > 0
                }
                // Sort apps by total time used in descending order (most used apps first)
                val sortedStats = filteredStats.sortedByDescending { it.second }


                for (usageStats in sortedStats) {
                    Log.d("UsageStats", "Package: ${usageStats.first}, Foreground Time (ms): ${usageStats.second}")
                }

                // Get top 3 most used apps
                val top3Apps = sortedStats.take(3)  // Take first 3 items after sorting

                var othersTime = 0
                val packageManager = requireContext().packageManager
                top3Apps.forEachIndexed { index, usageStats ->
                    // Add top 3 apps to the map
                    val totalTime = usageStats.second!!
                    val hours = (totalTime / (1000 * 60 * 60)).toInt()
                    val minutes = ((totalTime % (1000 * 60 * 60)) / (1000 * 60)).toInt()
                    val seconds = ((totalTime % (1000 * 60)) / 1000).toInt()

                    // Format time as HH:mm
                    val timeString = String.format("%02d:%02d:%02d", hours, minutes, seconds)
                    Log.i(javaClass.simpleName, index.toString() + usageStats.first + timeString)
                    map["App ${index + 1}"] = Pair(usageStats.second!!.toInt(), packageManager.getApplicationLabel(packageManager.getApplicationInfo(usageStats.first!!, 0)).toString())
                    Log.i(javaClass.simpleName, index.toString() + usageStats.first + usageStats.second!!.toInt())
                }

                // Combine all other apps' usage time into "Others"
                if (sortedStats.size > 3) {

                    // Calculate total time of remaining apps
                    for (i in 3 until sortedStats.size) {
                        val usageStats = sortedStats[i]
                        othersTime += usageStats.second!!.toInt()
                    }

                    // Calculate hours and minutes for "Others"
                    val hours = (othersTime / (1000 * 60 * 60)).toInt()
                    val minutes = ((othersTime % (1000 * 60 * 60)) / (1000 * 60)).toInt()
                    val seconds = ((othersTime % (1000 * 60)) / 1000).toInt()
                    val timeString = String.format("%02d:%02d:%02d", hours, minutes, seconds)
                    Log.i(javaClass.simpleName, "Others$timeString")
                    // Add "Others" category to map
                    map["Others"] = Pair(othersTime, "Others")
                    Log.i(javaClass.simpleName, "Others$othersTime")
                }
            }
            graph.setUsages(map)
            screen.addPreference(graph)
            val manageBlockListPreference = Preference(requireContext())
            manageBlockListPreference.onPreferenceClickListener = Preference.OnPreferenceClickListener { preference: Preference? ->
                startActivity(Intent(preference!!.context, BlockManageActivity::class.java))
                true
            }
            manageBlockListPreference.setTitle(R.string.manage_blocked_apps_and_app_limits)
            manageBlockListPreference.setIcon(R.drawable.hourglass_empty_24px)
            screen.addPreference(manageBlockListPreference)
            val libPref = Preference(requireContext())
            libPref.setIcon(io.github.dot166.jlib.R.mipmap.ic_launcher_j)
            libPref.setTitle(io.github.dot166.jlib.R.string.jlib_version)
            libPref.setSummary(libVersion)
            libPref.onPreferenceClickListener =
                Preference.OnPreferenceClickListener { preference: Preference? ->
                    startActivity(Intent(preference!!.context, LIBAboutActivity::class.java))
                    true
                }
            screen.addPreference(libPref)
            preferenceScreen = screen
        }
    }

    private fun hasUsageAccess(): Boolean {
        val appOps = getSystemService(APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }
}