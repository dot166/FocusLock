package io.github.dot166.focuslock.ui.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.preference.PreferenceManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import io.github.dot166.focuslock.AppBlockService
import io.github.dot166.focuslock.R
import io.github.dot166.focuslock.ui.widget.AppItemView
import io.github.dot166.focuslock.utils.BlockUtils
import io.github.dot166.focuslock.utils.UsageUtils.hasUsageAccess
import java.util.Locale

class BlockManageActivity : CoreActivity() {
    private lateinit var appsList: LinearLayout
    private var selectedApps = mutableListOf<String>()

    @SuppressLint("BatteryLife")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_block)
        configureToolBar(findViewById(R.id.toolbar))

        selectedApps.addAll(
            PreferenceManager.getDefaultSharedPreferences(this).getStringSet("blockedApps",
            mutableSetOf<String>())!!.toList())

        appsList = findViewById(R.id.appsList)
        val startButton = findViewById<MaterialButton>(R.id.startButton)

        displayInstalledApps()

        startButton.setOnClickListener {
            if (!hasUsageAccess(this)) {
                startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                Toast.makeText(this, "Please grant Usage Access permission", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                Toast.makeText(this, "Please grant overlay permission", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val packageName = applicationContext.packageName
            val pm = applicationContext.getSystemService(POWER_SERVICE) as PowerManager
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                val intent = Intent()
                intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                intent.setData(("package:$packageName").toUri())
                startActivity(intent)
                return@setOnClickListener
            }
            selectedApps.clear()
            for (i in 0 until appsList.childCount) {
                val appItemView = appsList.getChildAt(i) as AppItemView
                if (appItemView.checkBox.isChecked) {
                    selectedApps.add(appItemView.appPackageNameView.text.toString())
                }
            }

            if (selectedApps.isEmpty()) {
                Snackbar.make(startButton, "Please select at least one app to block.", Snackbar.LENGTH_SHORT).show()
            } else {
                PreferenceManager.getDefaultSharedPreferences(this).edit {
                    putStringSet(
                        "blockedApps",
                        selectedApps.toSet()
                    )
                }
                val intent = Intent(this, AppBlockService::class.java)
                intent.putStringArrayListExtra("blockedApps", ArrayList(selectedApps))
                startService(intent)
                Snackbar.make(startButton, "App Blocker started!", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun displayInstalledApps() {
        val packageManager = packageManager
        val apps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)

        apps.sortBy {
            packageManager.getApplicationLabel(it).toString().lowercase(Locale.getDefault())
        }

        for (appInfo in apps) {
            if (BlockUtils.isAllowedToMonitor(this, appInfo.packageName)) {
                val appItemView = AppItemView(this)
                appItemView.appPackageNameView.text = appInfo.packageName
                appItemView.appNameView.text = packageManager.getApplicationLabel(appInfo)
                appItemView.appIconView.setImageDrawable(packageManager.getApplicationIcon(appInfo))
                if (selectedApps.contains(appInfo.packageName)) {
                    appItemView.checkBox.isChecked = true
                }
                appItemView.checkBox.setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        selectedApps.add(appInfo.packageName)
                    } else {
                        selectedApps.remove(appInfo.packageName)
                    }
                }
                appsList.addView(appItemView)
            }
        }
    }
}