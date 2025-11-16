package io.github.dot166.focuslock.ui.activity

import android.Manifest
import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.util.Pair
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import androidx.preference.Preference
import androidx.preference.SwitchPreferenceCompat
import com.android.settingslib.preference.PreferenceFragment
import com.android.settingslib.widget.ButtonPreference2
import com.android.settingslib.widget.CircularGraphicPreference
import com.android.settingslib.widget.UntitledPreferenceCategory
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.github.dot166.focuslock.R
import io.github.dot166.focuslock.core.AppBlockService
import io.github.dot166.focuslock.core.AppBlockerAccessibilityService
import io.github.dot166.focuslock.core.BootCompletedReceiver
import io.github.dot166.focuslock.utils.UsageUtils
import io.github.dot166.focuslock.utils.UsageUtils.hasUsageAccess
import io.github.dot166.jlib.app.jConfigActivity
import io.github.dot166.jlib.utils.VersionUtils.isAtLeastT
import kotlinx.coroutines.DelicateCoroutinesApi

class MainActivity: jConfigActivity() {
    private var notificationPermissionLauncher: ActivityResultLauncher<String>? = null
    override fun preferenceFragment(): PreferenceFragment {
        return PrefFragment()
    }
    fun isAccessibilityServiceEnabled(context: Context, serviceClass: Class<out AccessibilityService>): Boolean {
        val accessibilityEnabled = try {
            Settings.Secure.getInt(context.contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED)
        } catch (e: Settings.SettingNotFoundException) {
            e.printStackTrace()
            0
        }

        if (accessibilityEnabled == 0) {
            return false
        }

        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )

        if (enabledServices == null || enabledServices.isEmpty()) {
            return false
        }

        val selfComponent = ComponentName(context, serviceClass).flattenToString()
        val splitter = TextUtils.SimpleStringSplitter(':')
        splitter.setString(enabledServices)

        while (splitter.hasNext()) {
            if (splitter.next().equals(selfComponent, ignoreCase = true)) {
                return true
            }
        }

        return false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!hasUsageAccess(this)) {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            Toast.makeText(this, "Please grant Usage Access permission", Toast.LENGTH_LONG).show()
            return
        }
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            Toast.makeText(this, "Please grant overlay permission", Toast.LENGTH_LONG).show()
            return
        }
        val packageName = applicationContext.packageName
        val pm = applicationContext.getSystemService(POWER_SERVICE) as PowerManager
        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
            val intent = Intent()
            intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            intent.setData(("package:$packageName").toUri())
            startActivity(intent)
            return
        }
        if (!isAccessibilityServiceEnabled(this, AppBlockerAccessibilityService::class.java)) {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
            Toast.makeText(this, "Please find and enable the " + getString(R.string.app_name) + " service.", Toast.LENGTH_LONG).show()
        }
        notificationPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean? ->
            if (!isGranted!!) {
                if (isAtLeastT) {
                    if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                        showNotificationPermissionRationale()
                    } else {
                        showSettingDialog()
                    }
                }
            }
        }
        forceNotificationPermission()
        val intent = Intent(this, AppBlockService::class.java)
        startForegroundService(intent)
    }

    class PrefFragment : PreferenceFragment(), SharedPreferences.OnSharedPreferenceChangeListener {
        var graph: CircularGraphicPreference? = null
        @OptIn(DelicateCoroutinesApi::class)
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            val screen = preferenceManager.createPreferenceScreen(context!!)
            val group0 = UntitledPreferenceCategory(context!!)
            screen.addPreference(group0)
            graph = CircularGraphicPreference(context!!)
            group0.addPreference(graph!!)
            val buttonPref = ButtonPreference2(context!!)
            buttonPref.setButtonStyle(ButtonPreference2.TYPE_OUTLINE, ButtonPreference2.SIZE_EXTRA_LARGE)
            buttonPref.setOnClickListener {
                context!!.startActivity(Intent(context, AppUsageActivity::class.java))
            }
            buttonPref.setTitle(getString(R.string.view_app_usage))
            group0.addPreference(buttonPref)
            val group1 = UntitledPreferenceCategory(context!!)
            screen.addPreference(group1)
            val manageBlockListPreference = Preference(context!!)
            manageBlockListPreference.onPreferenceClickListener = Preference.OnPreferenceClickListener { preference: Preference? ->
                startActivity(Intent(preference!!.context, BlockManageActivity::class.java))
                true
            }
            manageBlockListPreference.setTitle(R.string.manage_blocked_apps_and_app_limits)
            val drawable = context!!.getDrawable(R.drawable.hourglass_empty_24px)
            drawable!!.setTint(context!!.obtainStyledAttributes(intArrayOf(androidx.appcompat.R.attr.colorControlNormal)).getColor(0, 0))
            manageBlockListPreference.icon = drawable
            group1.addPreference(manageBlockListPreference)
            val showInL3Pref = SwitchPreferenceCompat(context!!)
            showInL3Pref.key = "show_icon_in_l3"
            showInL3Pref.icon = context!!.packageManager.getApplicationIcon(context!!.packageName)
            showInL3Pref.title = getString(R.string.show_app_icon_in_launcher)
            showInL3Pref.setDefaultValue(BootCompletedReceiver.readShowLauncherIcon(preferenceManager.sharedPreferences!!, context!!))
            val launcherIntent = Intent(Intent.ACTION_MAIN)
            launcherIntent.addCategory(Intent.CATEGORY_HOME)
            launcherIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            showInL3Pref.setSummary(
                getString(
                    R.string.show_icon_summary,
                    getString(R.string.app_name),
                    context!!.packageManager.getApplicationLabel(
                        context!!.packageManager.getApplicationInfo(
                            context!!.packageManager.resolveActivity(
                                launcherIntent,
                                PackageManager.MATCH_DEFAULT_ONLY
                            )!!.activityInfo.packageName, 0
                        )
                    )
                )
            )
            val appInfo = context!!.applicationInfo
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
                BootCompletedReceiver.toggleAppIcon(context!!)
            }
        }
        override fun onResume() {
            super.onResume()
            val map = mutableMapOf<String, Pair<Int, String>>()
            val stats = UsageUtils.getUsages(context!!, true)
            if (stats.isNotEmpty()) {
                for (usageStats in stats) {
                    Log.d("UsageStats", "Package: ${usageStats.first}, Foreground Time (ms): ${usageStats.second}")
                }

                // Get top 3 most used apps
                val top3Apps = stats.take(3)  // Take first 3 items after sorting

                var othersTime = 0
                val packageManager = context!!.packageManager
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
            graph!!.setCentreLabel(context!!.getString(R.string.scrntime_label_centre, timeString))
            graph!!.setUsages(map)
        }
    }
    private fun showSettingDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.notification_permission)
            .setMessage(getString(R.string.settings_notif_dialog, getString(R.string.app_name)))
            .setPositiveButton(
                io.github.dot166.jlib.R.string.ok
            ) { _: DialogInterface?, _: Int ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.setData(("package:$packageName").toUri())
                startActivity(intent)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
    private fun showNotificationPermissionRationale() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.notification_permission)
            .setMessage(getString(R.string.notif_dialog, getString(R.string.app_name)))
            .setPositiveButton(
                R.string.yes
            ) { _: DialogInterface?, _: Int ->
                if (isAtLeastT) {
                    notificationPermissionLauncher!!.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
            .setNegativeButton(R.string.no, null)
            .show()
    }
    fun forceNotificationPermission() {
        if (isAtLeastT) {
            notificationPermissionLauncher!!.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}