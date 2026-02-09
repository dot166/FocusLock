package io.github.dot166.focuslock.ui.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.preference.Preference
import com.android.settingslib.preference.PreferenceFragment
import io.github.dot166.focuslock.utils.UsageUtils
import java.util.Locale

class AppUsageFragment : PreferenceFragment() {
    @SuppressLint("BatteryLife")
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceScreen = preferenceManager.createPreferenceScreen(requireContext())
    }
    @SuppressLint("DefaultLocale")
    override fun onResume() {
        super.onResume()
        preferenceScreen.removeAll()

        val packageManager = requireContext().packageManager
        val usages: MutableList<Pair<String?, Long?>> = UsageUtils.getUsages(requireContext(), true) as MutableList<Pair<String?, Long?>>

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