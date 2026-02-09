package io.github.dot166.focuslock.ui.fragments

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.preference.Preference
import com.android.settingslib.preference.PreferenceFragment
import com.android.settingslib.widget.BarChartPreference2
import com.android.settingslib.widget.UntitledPreferenceCategory
import io.github.dot166.focuslock.utils.UsageUtils
import java.util.Calendar
import java.util.Locale

class WeekFragment : PreferenceFragment() {
    var graph: BarChartPreference2? = null
    var group1: UntitledPreferenceCategory? = null
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        val screen = preferenceManager.createPreferenceScreen(requireContext())
        val group0 = UntitledPreferenceCategory(requireContext())
        screen.addPreference(group0)
        graph = BarChartPreference2(requireContext())
        group0.addPreference(graph!!)
        group1 = UntitledPreferenceCategory(requireContext())
        screen.addPreference(group1!!)
        preferenceScreen = screen
    }
    @Suppress("UNCHECKED_CAST")
    @SuppressLint("DefaultLocale")
    override fun onResume() {
        super.onResume()
        val stats = UsageUtils.getWeeklyScreenTime(requireContext())
        graph!!.setValues(stats as MutableList<Long?>)
        group1!!.removeAll()

        val packageManager = requireContext().packageManager

        for (i in 0 until stats.size) {
            val totalTime = stats[i]
            val millisPerSecond = 1000L
            val millisPerMinute = 60L * millisPerSecond
            val millisPerHour   = 60L * millisPerMinute
            val millisPerDay    = 24L * millisPerHour
            val days = totalTime / millisPerDay
            val hours = (totalTime % millisPerDay) / millisPerHour
            val minutes = (totalTime % millisPerHour) / millisPerMinute
            val seconds = (totalTime % millisPerMinute) / millisPerSecond

            val timeString = "${days}d %02dh %02dm %02ds".format(hours, minutes, seconds)
            val appUsageItemPref = Preference(requireContext())
            appUsageItemPref.summary = timeString
            appUsageItemPref.title = dayNameFromDaysAgo(i)
            appUsageItemPref.icon = getDefaultCalendarIcon(packageManager)
            group1!!.addPreference(appUsageItemPref)
        }
    }
    fun dayNameFromDaysAgo(daysAgo: Int): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -daysAgo)

        return cal.getDisplayName(
            Calendar.DAY_OF_WEEK,
            Calendar.LONG,
            Locale.getDefault()
        )!!
    }
    fun getDefaultCalendarIcon(pm: PackageManager): Drawable? {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_APP_CALENDAR)
        }

        val resolveInfo = pm.resolveActivity(
            intent,
            PackageManager.MATCH_DEFAULT_ONLY
        ) ?: return null

        val packageName = resolveInfo.activityInfo.packageName
        val appInfo = pm.getApplicationInfo(packageName, 0)
        val icon = pm.getApplicationIcon(appInfo)
        return icon
    }
}