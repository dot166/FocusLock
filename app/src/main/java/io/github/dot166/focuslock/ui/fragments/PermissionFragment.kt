package io.github.dot166.focuslock.ui.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context.POWER_SERVICE
import android.content.DialogInterface
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import androidx.preference.Preference
import androidx.preference.SwitchPreferenceCompat
import com.android.settingslib.preference.PreferenceFragment
import io.github.dot166.focuslock.R
import io.github.dot166.focuslock.core.AppBlockerAccessibilityService
import io.github.dot166.focuslock.utils.PermissionUtils
import io.github.dot166.focuslock.utils.UsageUtils.hasUsageAccess
import io.github.dot166.jlib.app.SettingsLibAlertDialogBuilder
import kotlinx.coroutines.DelicateCoroutinesApi


class PermissionFragment : PreferenceFragment() {
    private var notificationPermissionLauncher: ActivityResultLauncher<String>? = null
    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        val screen = preferenceManager.createPreferenceScreen(requireContext())
        notificationPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean? ->
            if (!isGranted!!) {
                if (Build.VERSION.SDK_INT_FULL >= Build.VERSION_CODES_FULL.TIRAMISU) {
                    if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                        showNotificationPermissionRationale()
                    } else {
                        showSettingDialog()
                    }
                }
            }
        }
        preferenceScreen = screen
    }
    @SuppressLint("BatteryLife")
    override fun onResume() {
        super.onResume()
        preferenceScreen.removeAll()

        val usagePermPreference = SwitchPreferenceCompat(requireContext())
        val overlayPermPreference = SwitchPreferenceCompat(requireContext())
        val batteryPermPreference = SwitchPreferenceCompat(requireContext())
        val accessibilityPermPreference = SwitchPreferenceCompat(requireContext())
        val notifPermPreference = SwitchPreferenceCompat(requireContext())
        usagePermPreference.title = "Usage Access Permission"
        overlayPermPreference.title = "Overlay Permission"
        batteryPermPreference.title = "Battery Optimization Exemption"
        accessibilityPermPreference.title = "Accessibility Service"
        notifPermPreference.title = "Notification Permission"

        usagePermPreference.onPreferenceClickListener = Preference.OnPreferenceClickListener { _: Preference? ->
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            Toast.makeText(requireContext(), "Please grant Usage Access permission", Toast.LENGTH_LONG).show()
            true
        }

        if (!hasUsageAccess(requireContext())) {
            usagePermPreference.isChecked = false
            usagePermPreference.isEnabled = true
        } else {
            usagePermPreference.isChecked = true
            usagePermPreference.isEnabled = false
        }

        overlayPermPreference.onPreferenceClickListener = Preference.OnPreferenceClickListener { _: Preference? ->
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            Toast.makeText(requireContext(), "Please grant overlay permission", Toast.LENGTH_LONG).show()
            true
        }
        if (!Settings.canDrawOverlays(requireContext())) {
            overlayPermPreference.isChecked = false
            overlayPermPreference.isEnabled = true
        } else {
            overlayPermPreference.isChecked = true
            overlayPermPreference.isEnabled = false
        }
        val packageName = requireContext().packageName
        val pm = requireContext().getSystemService(POWER_SERVICE) as PowerManager
        batteryPermPreference.onPreferenceClickListener = Preference.OnPreferenceClickListener { _: Preference? ->
            val intent = Intent()
            intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            intent.setData(("package:$packageName").toUri())
            startActivity(intent)
            true
        }
        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
            batteryPermPreference.isChecked = false
            batteryPermPreference.isEnabled = true
        } else {
            batteryPermPreference.isChecked = true
            batteryPermPreference.isEnabled = false
        }
        accessibilityPermPreference.onPreferenceClickListener = Preference.OnPreferenceClickListener { _: Preference? ->
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
            Toast.makeText(requireContext(), "Please find and enable the " + getString(R.string.app_name) + " service.", Toast.LENGTH_LONG).show()
            true
        }
        if (!PermissionUtils.isAccessibilityServiceEnabled(requireContext(), AppBlockerAccessibilityService::class.java)) {
            accessibilityPermPreference.isChecked = false
            accessibilityPermPreference.isEnabled = true
        } else {
            accessibilityPermPreference.isChecked = true
            accessibilityPermPreference.isEnabled = false
        }

        if (Build.VERSION.SDK_INT_FULL >= Build.VERSION_CODES_FULL.TIRAMISU) {
            notifPermPreference.onPreferenceClickListener = Preference.OnPreferenceClickListener { _: Preference ->
                notificationPermissionLauncher!!.launch(Manifest.permission.POST_NOTIFICATIONS)
                true
            }
            if (!NotificationManagerCompat.from(requireContext()).areNotificationsEnabled()) {
                notifPermPreference.isChecked = false
                notifPermPreference.isEnabled = true
            } else {
                notifPermPreference.isChecked = true
                notifPermPreference.isEnabled = false
            }
        } else {
            // treat as granted
            notifPermPreference.isChecked = true
            notifPermPreference.isEnabled = false
        }
        preferenceScreen.addPreference(usagePermPreference)
        preferenceScreen.addPreference(overlayPermPreference)
        preferenceScreen.addPreference(batteryPermPreference)
        preferenceScreen.addPreference(accessibilityPermPreference)
        preferenceScreen.addPreference(notifPermPreference)
    }
    private fun showSettingDialog() {
        SettingsLibAlertDialogBuilder(requireContext())
            .setTitle(R.string.notification_permission)
            .setMessage(getString(R.string.settings_notif_dialog, getString(R.string.app_name)))
            .setPositiveButton(
                android.R.string.ok
            ) { _: DialogInterface?, _: Int ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val packageName = requireContext().packageName
                intent.setData(("package:$packageName").toUri())
                startActivity(intent)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
    private fun showNotificationPermissionRationale() {
        SettingsLibAlertDialogBuilder(requireContext())
            .setTitle(R.string.notification_permission)
            .setMessage(getString(R.string.notif_dialog, getString(R.string.app_name)))
            .setPositiveButton(
                R.string.yes
            ) { _: DialogInterface?, _: Int ->
                if (Build.VERSION.SDK_INT_FULL >= Build.VERSION_CODES_FULL.TIRAMISU) {
                    notificationPermissionLauncher!!.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
            .setNegativeButton(R.string.no, null)
            .show()
    }
}