package io.github.dot166.focuslock.ui.fragments

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.android.settingslib.preference.PreferenceFragment
import io.github.dot166.focuslock.core.RestrictedApp
import io.github.dot166.focuslock.core.findDuration
import io.github.dot166.focuslock.core.getDuration
import io.github.dot166.focuslock.core.getPosition
import io.github.dot166.focuslock.utils.BlockUtils
import java.util.Locale

class BlockManageFragment : PreferenceFragment() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceScreen = preferenceManager.createPreferenceScreen(requireContext())
    }

    override fun onResume() {
        super.onResume()
        preferenceScreen.removeAll()

        val allApps = mutableListOf<RestrictedApp>()
        allApps.addAll(
            PreferenceManager.getDefaultSharedPreferences(requireContext()).getStringSet("blockedApps",
                mutableSetOf<String>())!!.toList()
        )

        val packageManager = requireContext().packageManager
        val apps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)

        apps.filter {
            BlockUtils.isAllowedToMonitor(requireContext(), it.packageName)
        }

        apps.sortBy {
            packageManager.getApplicationLabel(it).toString().lowercase(Locale.getDefault())
        }

        allApps.sortBy {
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(it.packageName, 0)).toString().lowercase(Locale.getDefault())
        }

        var isUpToDate = true

        for (i in 0 until apps.size) {
            if (apps.size != allApps.size) {
                isUpToDate = false
                break
            }
            val appInfo = apps[i]
            val app = allApps[i]
            if (appInfo.packageName != app.packageName) {
                isUpToDate = false
            }
        }

        if (!isUpToDate) {
            val savedRestrictedApps = mutableListOf<RestrictedApp>()
            for (i in 0 until allApps.size) {
                val app = allApps[i]
                if (app.allowedTimeInMinutes > -1) {
                    savedRestrictedApps.add(app)
                }
            }
            allApps.clear()
            allApps.addAll(savedRestrictedApps)
            allApps.addAll(apps, requireContext())
            allApps.sortBy {
                packageManager.getApplicationLabel(packageManager.getApplicationInfo(it.packageName, 0)).toString().lowercase(Locale.getDefault())
            }
        }

        for (app in allApps) {
            val appItemPref = app.getItemPreference(requireContext())
            appItemPref.setSelection(getPosition(findDuration(app.allowedTimeInMinutes)))
            appItemPref.setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    allApps.update(appItemPref.summary.toString(), getDuration(position).minutes)

                    PreferenceManager.getDefaultSharedPreferences(requireContext()).edit {
                        putStringSet(
                            "blockedApps",
                            allApps.toStringSet()
                        )
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    // Nothing to implement
                }
            })
            preferenceScreen.addPreference(appItemPref)
        }
    }
}

private fun MutableList<RestrictedApp>.toStringSet(): MutableSet<String> {
    val stringSet = mutableSetOf<String>()
    for (i in 0 until size) {
        stringSet.add(get(i).toString())
    }
    return stringSet
}

private fun MutableList<RestrictedApp>.addAll(elements: List<String>) {
    for (i in 0 until elements.size) {
        add(RestrictedApp.fromString(elements[i]))
    }
}

private fun MutableList<RestrictedApp>.findByPackageName(pName: String): RestrictedApp {
    for (i in 0 until size) {
        if (get(i).packageName == pName)
            return get(i)
    }
    throw IllegalArgumentException()
}

private fun MutableList<RestrictedApp>.update(pName: String, allowedTimeInMinutes: Long) {
    findByPackageName(pName).allowedTimeInMinutes = allowedTimeInMinutes
}

private fun MutableList<RestrictedApp>.addAll(elements: List<ApplicationInfo>, ctx: Context) {
    for (i in 0 until elements.size) {
        if (BlockUtils.isAllowedToMonitor(ctx, elements[i].packageName)) {
            var hasInList = false
            for (j in 0 until size) {
                if (elements[i].packageName == get(j).packageName)
                    hasInList = true
            }
            if (!hasInList)
                add(RestrictedApp(elements[i].packageName, -1))
        }
    }
}