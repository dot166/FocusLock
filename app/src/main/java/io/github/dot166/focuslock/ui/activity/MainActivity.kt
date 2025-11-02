package io.github.dot166.focuslock.ui.activity

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import io.github.dot166.focuslock.R
import io.github.dot166.focuslock.ui.preference.ScreenTimeGraphicPreference
import io.github.dot166.focuslock.utils.UsageUtils
import io.github.dot166.focuslock.utils.UsageUtils.hasUsageAccess
import io.github.dot166.jlib.LIBAboutActivity
import io.github.dot166.jlib.app.jConfigActivity
import io.github.dot166.jlib.utils.VersionUtils
import kotlinx.coroutines.DelicateCoroutinesApi

class MainActivity: jConfigActivity() {
    override fun preferenceFragment(): PreferenceFragmentCompat {
        return PrefFragment()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!hasUsageAccess(this)) {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            Toast.makeText(this, "Please grant Usage Access permission", Toast.LENGTH_LONG).show()
        }
    }

    class PrefFragment : PreferenceFragmentCompat() {
        @OptIn(DelicateCoroutinesApi::class)
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            val screen = preferenceManager.createPreferenceScreen(requireContext())
            val graph = ScreenTimeGraphicPreference(requireContext())
            val map = mutableMapOf<String, Pair<Int, String>>()
            val stats = UsageUtils.getUsages(requireContext(), true)
            if (stats.isNotEmpty()) {
                for (usageStats in stats) {
                    Log.d("UsageStats", "Package: ${usageStats.first}, Foreground Time (ms): ${usageStats.second}")
                }

                // Get top 3 most used apps
                val top3Apps = stats.take(3)  // Take first 3 items after sorting

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
                if (stats.size > 3) {

                    // Calculate total time of remaining apps
                    for (i in 3 until stats.size) {
                        val usageStats = stats[i]
                        othersTime += usageStats.second!!.toInt()
                    }

                    // Calculate hours and minutes for "Others"
                    val hours = (othersTime / (1000 * 60 * 60))
                    val minutes = ((othersTime % (1000 * 60 * 60)) / (1000 * 60))
                    val seconds = ((othersTime % (1000 * 60)) / 1000)
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
            libPref.setSummary(VersionUtils.libVersion)
            libPref.onPreferenceClickListener =
                Preference.OnPreferenceClickListener { preference: Preference? ->
                    startActivity(Intent(preference!!.context, LIBAboutActivity::class.java))
                    true
                }
            screen.addPreference(libPref)
            preferenceScreen = screen
        }
    }
}