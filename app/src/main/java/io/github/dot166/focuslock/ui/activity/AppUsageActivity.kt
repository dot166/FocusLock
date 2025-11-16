package io.github.dot166.focuslock.ui.activity

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.preference.Preference
import com.android.settingslib.preference.PreferenceFragment
import io.github.dot166.focuslock.utils.UsageUtils
import io.github.dot166.jlib.app.jConfigActivity
import java.util.Locale

class AppUsageActivity : jConfigActivity() {
    override fun preferenceFragment(): PreferenceFragment {
        return AppUsageFragment()
    }

    class AppUsageFragment : PreferenceFragment() {
        @SuppressLint("BatteryLife")
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            preferenceScreen = preferenceManager.createPreferenceScreen(requireContext())
        }
        override fun onResume() {
            super.onResume()
            preferenceScreen.removeAll()

            val packageManager = requireContext().packageManager
            val usages = UsageUtils.getUsages(requireContext(), true)

            usages.sortedBy {
                packageManager.getApplicationLabel(packageManager.getApplicationInfo(it.first!!, 0)).toString().lowercase(Locale.getDefault())
            }

            for (i in 0 until usages.size) {
                val appInfo = packageManager.getApplicationInfo(usages[i].first!!, 0)
                val totalTime = usages[i].second!!
                val hours = (totalTime / (1000 * 60 * 60)).toInt()
                val minutes = ((totalTime % (1000 * 60 * 60)) / (1000 * 60)).toInt()
                val seconds = ((totalTime % (1000 * 60)) / 1000).toInt()

                val timeString = String.format("%02d:%02d:%02d", hours, minutes, seconds)
                val appUsageItemPref = Preference(requireContext())
                appUsageItemPref.summary = timeString
                appUsageItemPref.title = packageManager.getApplicationLabel(appInfo) as String
                appUsageItemPref.icon = packageManager.getApplicationIcon(appInfo)
                preferenceScreen.addPreference(appUsageItemPref)
            }
        }
    }
}