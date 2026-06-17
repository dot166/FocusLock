package io.github.dot166.focuslock.ui.pages

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.os.ConfigurationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.android.settingslib.spa.framework.common.SettingsPageProvider
import com.android.settingslib.spa.framework.common.SpaEnvironmentFactory
import com.android.settingslib.spa.widget.scaffold.RegularScaffold
import com.android.settingslib.spa.widget.ui.Category
import com.android.settingslib.spa.widget.ui.CircularLoadingBar
import io.github.dot166.focuslock.R
import io.github.dot166.focuslock.core.RestrictedApp
import io.github.dot166.focuslock.core.RestrictedApp.Companion.getRestrictedApps
import io.github.dot166.focuslock.core.RestrictedApp.Companion.isPackageInstalled
import io.github.dot166.focuslock.core.getDuration
import io.github.dot166.focuslock.utils.BlockUtils

object BlockManagePageProvider : SettingsPageProvider {
    override val name: String = "blockManage"
    var apps by mutableStateOf<List<RestrictedApp>>(listOf())
    var loading by mutableStateOf(true)

    override fun getTitle(arguments: Bundle?): String {
        return SpaEnvironmentFactory.instance.appContext.getString(R.string.manage_blocked_apps_and_app_limits)
    }

    @Composable
    override fun Page(arguments: Bundle?) {
        val title = remember { getTitle(arguments) }
        RegularScaffold(title) {
            val lifecycleOwner = LocalLifecycleOwner.current
            val lifecycleState by lifecycleOwner.lifecycle.currentStateFlow.collectAsState()
            LaunchedEffect(lifecycleState) {
                when (lifecycleState) {
                    Lifecycle.State.DESTROYED -> {}
                    Lifecycle.State.INITIALIZED -> {}
                    Lifecycle.State.CREATED -> {
                        loadApps()
                    }

                    Lifecycle.State.STARTED -> {}
                    Lifecycle.State.RESUMED -> {
                        loadApps()
                    }
                }
            }
            if (loading) {
                CircularLoadingBar(loading)
            } else {
                Category {
                    for (app in apps) {
                        app.ItemPreference(SpaEnvironmentFactory.instance.appContext) {
                            update(apps, app.packageName, getDuration(it).minutes)
                        }
                    }
                }
            }
        }
    }

    fun loadApps() {
        loading = true
        val ctx = SpaEnvironmentFactory.instance.appContext
        val allApps = mutableListOf<RestrictedApp>()
        allApps.addAll(getRestrictedApps())

        val packageManager = ctx.packageManager
        val appsFromSystem = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)

        appsFromSystem.filter {
            BlockUtils.isAllowedToMonitor(ctx, it.packageName)
        }

        appsFromSystem.sortBy {
            packageManager.getApplicationLabel(it).toString()
                .lowercase(ConfigurationCompat.getLocales(SpaEnvironmentFactory.instance.appContext.resources.configuration)[0]!!)
        }

        allApps.sortBy {
            if (isPackageInstalled(packageManager, it.packageName)) {
                packageManager.getApplicationLabel(
                    packageManager.getApplicationInfo(
                        it.packageName,
                        0
                    )
                )
                    .toString()
                    .lowercase(ConfigurationCompat.getLocales(SpaEnvironmentFactory.instance.appContext.resources.configuration)[0]!!)
            } else {
                it.packageName
            }
        }

        var isUpToDate = true

        for ((i, element) in appsFromSystem.withIndex()) {
            if (appsFromSystem.size != allApps.size) {
                isUpToDate = false
                break
            }
            val appInfo = element
            val app = allApps[i]
            if (appInfo.packageName != app.packageName) {
                isUpToDate = false
            }
        }

        if (!isUpToDate) {
            val savedRestrictedApps = mutableListOf<RestrictedApp>()
            for (i in allApps.indices) {
                val app = allApps[i]
                if (app.allowedTimeInMinutes > -1) {
                    savedRestrictedApps.add(app)
                }
            }
            allApps.clear()
            allApps.addAll(savedRestrictedApps)
            allApps.addAll(appsFromSystem, ctx)
            allApps.sortBy {
                if (isPackageInstalled(packageManager, it.packageName)) {
                    packageManager.getApplicationLabel(
                        packageManager.getApplicationInfo(
                            it.packageName,
                            0
                        )
                    ).toString()
                        .lowercase(ConfigurationCompat.getLocales(SpaEnvironmentFactory.instance.appContext.resources.configuration)[0]!!)
                } else {
                    it.packageName
                }
            }
        }
        apps = allApps
        loading = false
    }

    private fun update(
        initialList: List<RestrictedApp>,
        pName: String,
        allowedTimeInMinutes: Long
    ) {
        val list = initialList.toMutableList()
        list.findByPackageName(pName).allowedTimeInMinutes = allowedTimeInMinutes
        RestrictedApp.saveRestrictedApps(list)
        loadApps()
    }

}

private fun MutableList<RestrictedApp>.findByPackageName(pName: String): RestrictedApp {
    for (i in indices) {
        if (get(i).packageName == pName)
            return get(i)
    }
    throw IllegalArgumentException()
}

private fun MutableList<RestrictedApp>.addAll(elements: List<ApplicationInfo>, ctx: Context) {
    for (i in elements.indices) {
        if (BlockUtils.isAllowedToMonitor(ctx, elements[i].packageName)) {
            var hasInList = false
            for (j in indices) {
                if (elements[i].packageName == get(j).packageName)
                    hasInList = true
            }
            if (!hasInList)
                add(RestrictedApp(elements[i].packageName, -1))
        }
    }
}
