package io.github.dot166.focuslock.ui.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.util.Pair
import androidx.appcompat.content.res.AppCompatResources
import androidx.fragment.app.Fragment
import androidx.preference.Preference
import androidx.preference.SwitchPreferenceCompat
import com.android.settingslib.preference.PreferenceFragment
import com.android.settingslib.widget.BannerMessagePreference
import com.android.settingslib.widget.ButtonPreference2
import com.android.settingslib.widget.CircularGraphicPreference
import com.android.settingslib.widget.UntitledPreferenceCategory
import io.github.dot166.focuslock.R
import io.github.dot166.focuslock.core.AppBlockService
import io.github.dot166.focuslock.core.BootCompletedReceiver
import io.github.dot166.focuslock.ui.fragments.AppUsageFragment
import io.github.dot166.focuslock.ui.fragments.BlockManageFragment
import io.github.dot166.focuslock.ui.fragments.PermissionFragment
import io.github.dot166.focuslock.utils.PermissionUtils
import io.github.dot166.focuslock.utils.UsageUtils
import io.github.dot166.jlib.app.jConfigActivity
import kotlinx.coroutines.DelicateCoroutinesApi

class MainActivity: jConfigActivity() {
    override fun preferenceFragment(): PreferenceFragment {
        return PrefFragment()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intent = Intent(this, AppBlockService::class.java)
        startForegroundService(intent)
    }

    class PrefFragment : PreferenceFragment(), SharedPreferences.OnSharedPreferenceChangeListener {
        var graph: CircularGraphicPreference? = null
        var group: UntitledPreferenceCategory? = null
        @SuppressLint("Recycle")
        @OptIn(DelicateCoroutinesApi::class)
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            val screen = preferenceManager.createPreferenceScreen(requireContext())
            val group0 = UntitledPreferenceCategory(requireContext())
            group = UntitledPreferenceCategory(requireContext())
            screen.addPreference(group!!)
            screen.addPreference(group0)
            graph = CircularGraphicPreference(requireContext())
            group0.addPreference(graph!!)
            val buttonPref = ButtonPreference2(requireContext())
            buttonPref.setButtonStyle(ButtonPreference2.TYPE_OUTLINE, ButtonPreference2.SIZE_EXTRA_LARGE)
            buttonPref.setOnClickListener {
                val args: Bundle = buttonPref.getExtras()
                val fragment: Fragment = requireActivity().supportFragmentManager.getFragmentFactory().instantiate(
                    requireActivity().classLoader, AppUsageFragment().javaClass.name
                )
                fragment.setArguments(args)
                requireActivity().supportFragmentManager.beginTransaction()
                    .replace(com.android.settingslib.collapsingtoolbar.R.id.content_frame, fragment).addToBackStack(null).commit()
            }
            buttonPref.setTitle(getString(R.string.view_app_usage))
            group0.addPreference(buttonPref)
            val group1 = UntitledPreferenceCategory(requireContext())
            screen.addPreference(group1)
            val manageBlockListPreference = Preference(requireContext())
            manageBlockListPreference.fragment = BlockManageFragment().javaClass.name
            manageBlockListPreference.setTitle(R.string.manage_blocked_apps_and_app_limits)
            val drawable = AppCompatResources.getDrawable(requireContext(), R.drawable.hourglass_empty_24px)
            drawable!!.setTint(requireContext().obtainStyledAttributes(intArrayOf(androidx.appcompat.R.attr.colorControlNormal)).getColor(0, 0))
            manageBlockListPreference.icon = drawable
            group1.addPreference(manageBlockListPreference)
            val showInL3Pref = SwitchPreferenceCompat(requireContext())
            showInL3Pref.key = "show_icon_in_l3"
            showInL3Pref.icon = requireContext().packageManager.getApplicationIcon(requireContext().packageName)
            showInL3Pref.title = getString(R.string.show_app_icon_in_launcher)
            showInL3Pref.setDefaultValue(BootCompletedReceiver.readShowLauncherIcon(preferenceManager.sharedPreferences!!, requireContext()))
            val launcherIntent = Intent(Intent.ACTION_MAIN)
            launcherIntent.addCategory(Intent.CATEGORY_HOME)
            launcherIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            showInL3Pref.setSummary(
                getString(
                    R.string.show_icon_summary,
                    getString(R.string.app_name),
                    requireContext().packageManager.getApplicationLabel(
                        requireContext().packageManager.getApplicationInfo(
                            requireContext().packageManager.resolveActivity(
                                launcherIntent,
                                PackageManager.MATCH_DEFAULT_ONLY
                            )!!.activityInfo.packageName, 0
                        )
                    )
                )
            )
            val appInfo = requireContext().applicationInfo
            val isApplicationInSystemImage =
                (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            showInL3Pref.isEnabled = isApplicationInSystemImage
            group1.addPreference(showInL3Pref)
            preferenceScreen = screen
            preferenceManager.getSharedPreferences()!!.registerOnSharedPreferenceChangeListener(this)
        }
        override fun onSharedPreferenceChanged(
            sharedPreferences: SharedPreferences,
            key: String?
        ) {
            if (key == "show_icon_in_l3") {
                BootCompletedReceiver.toggleAppIcon(requireContext())
            }
        }
        @SuppressLint("DefaultLocale")
        override fun onResume() {
            super.onResume()
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
                    map["Value ${index + 1}"] = Pair(usageStats.second!!.toInt(), packageManager.getApplicationLabel(packageManager.getApplicationInfo(usageStats.first!!, 0)).toString())
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
                    map["Value 4"] = Pair(othersTime, "Others")
                    Log.i(javaClass.simpleName, "Others$othersTime")
                }
            }
            var total = 0
            for (i in map.entries) {
                total += i.value.first
            }
            val hours = (total / (1000 * 60 * 60))
            val minutes = ((total % (1000 * 60 * 60)) / (1000 * 60))
            val seconds = ((total % (1000 * 60)) / 1000)
            val timeString = String.format("\n%02dh%02dm%02ds", hours, minutes, seconds)
            graph!!.setCentreLabel(requireContext().getString(R.string.scrntime_label_centre, timeString))
            graph!!.setUsages(map)
            group!!.removeAll()
            if (!PermissionUtils.hasAllPermissions(requireContext())) {
                val bannerMessagePreference = BannerMessagePreference(requireContext())
                bannerMessagePreference.title = getString(R.string.missing_perms_title, getString(R.string.app_name))
                bannerMessagePreference.setAttentionLevel(BannerMessagePreference.AttentionLevel.HIGH)
                bannerMessagePreference.setPositiveButtonText(getString(R.string.grant_required_permissions))
                bannerMessagePreference.setPositiveButtonEnabled(true)
                bannerMessagePreference.setPositiveButtonVisible(true)
                bannerMessagePreference.setPositiveButtonOnClickListener {
                    val args: Bundle = bannerMessagePreference.getExtras()
                    val fragment: Fragment = requireActivity().supportFragmentManager.getFragmentFactory().instantiate(
                        requireActivity().classLoader, PermissionFragment().javaClass.name
                    )
                    fragment.setArguments(args)
                    requireActivity().supportFragmentManager.beginTransaction()
                        .replace(com.android.settingslib.collapsingtoolbar.R.id.content_frame, fragment).addToBackStack(null).commit()
                }
                group!!.addPreference(bannerMessagePreference)
            }
        }
    }
}