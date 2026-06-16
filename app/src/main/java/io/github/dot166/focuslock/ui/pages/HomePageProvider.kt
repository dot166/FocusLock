package io.github.dot166.focuslock.ui.pages

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.os.ConfigurationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.android.settingslib.datastore.DataChangeReason
import com.android.settingslib.datastore.KeyedObserver
import com.android.settingslib.spa.framework.common.SettingsPageProvider
import com.android.settingslib.spa.framework.common.SpaEnvironmentFactory
import com.android.settingslib.spa.framework.compose.navigator
import com.android.settingslib.spa.framework.compose.rememberDrawablePainter
import com.android.settingslib.spa.framework.theme.SettingsDimension
import com.android.settingslib.spa.widget.banner.BannerButton
import com.android.settingslib.spa.widget.banner.BannerModel
import com.android.settingslib.spa.widget.banner.SettingsBanner
import com.android.settingslib.spa.widget.chart.PieChart
import com.android.settingslib.spa.widget.chart.PieChartData
import com.android.settingslib.spa.widget.chart.PieChartModel
import com.android.settingslib.spa.widget.preference.Preference
import com.android.settingslib.spa.widget.preference.PreferenceModel
import com.android.settingslib.spa.widget.preference.SwitchPreference
import com.android.settingslib.spa.widget.preference.SwitchPreferenceModel
import com.android.settingslib.spa.widget.scaffold.RegularScaffold
import com.android.settingslib.spa.widget.ui.Category
import com.google.common.util.concurrent.MoreExecutors
import io.github.dot166.focuslock.R
import io.github.dot166.focuslock.core.BootCompletedReceiver
import io.github.dot166.focuslock.utils.PermissionUtils.hasAllPermissions
import io.github.dot166.focuslock.utils.UsageUtils
import io.github.dot166.jlib.app.DefaultSharedPrefsManager

object HomePageProvider : SettingsPageProvider, KeyedObserver<String?> {
    override val name = "FocusLock"
    var usages by mutableStateOf<List<PieChartData>>(listOf())

    override fun getTitle(arguments: Bundle?): String {
        return SpaEnvironmentFactory.instance.appContext.getString(R.string.app_name)
    }

    @Composable
    override fun Page(arguments: Bundle?) {
        val title = remember { getTitle(arguments) }
        DefaultSharedPrefsManager.getSharedPreferencesStorage(SpaEnvironmentFactory.instance.appContext)
            .addObserver(this, MoreExecutors.directExecutor())
        RegularScaffold(title) {
            if (!hasAllPermissions(SpaEnvironmentFactory.instance.appContext)) {
                val navigatePerms = navigator("perms")
                val model = BannerModel(
                    title = stringResource(
                        R.string.missing_perms_title,
                        stringResource(R.string.app_name)
                    ),
                    text = "", // TODO: write a proper description
                    buttons = listOf(
                        BannerButton(
                            text = stringResource(R.string.grant_required_permissions),
                            onClick = { navigatePerms() })
                    )
                )
                SettingsBanner(model)
            }
            val lifecycleOwner = LocalLifecycleOwner.current
            val lifecycleState by lifecycleOwner.lifecycle.currentStateFlow.collectAsState()
            LaunchedEffect(lifecycleState) {
                when (lifecycleState) {
                    Lifecycle.State.DESTROYED -> {}
                    Lifecycle.State.INITIALIZED -> {}
                    Lifecycle.State.CREATED -> {
                        loadUsages()
                    }

                    Lifecycle.State.STARTED -> {}
                    Lifecycle.State.RESUMED -> {
                        loadUsages()
                    }
                }
            }
            Category {
                val chartData = usages
                var total = 0L
                for (i in chartData) {
                    total += i.value!!.toLong()
                }
                val hours = (total / (1000 * 60 * 60))
                val minutes = ((total % (1000 * 60 * 60)) / (1000 * 60))
                val seconds = ((total % (1000 * 60)) / 1000)
                val locale =
                    ConfigurationCompat.getLocales(SpaEnvironmentFactory.instance.appContext.resources.configuration)[0]!!
                val timeString = String.format(
                    locale,
                    "%02d:%02d:%02d",
                    hours,
                    minutes,
                    seconds
                )
                PieChart(
                    pieChartModel = object : PieChartModel {
                        override val chartDataList = chartData
                        override val centerText =
                            stringResource(R.string.scrntime_label_centre, timeString)
                    }
                )
                val navigateAllAppsUsage = navigator("allUsage")
                Preference(object : PreferenceModel {
                    override val title: String = stringResource(R.string.view_app_usage)
                    override val onClick: (() -> Unit) = { navigateAllAppsUsage() }
                })
            }
            Category {
                val navigateBlockManage = navigator("blockManage")
                Preference(object : PreferenceModel {
                    override val title: String =
                        stringResource(R.string.manage_blocked_apps_and_app_limits)
                    override val onClick: (() -> Unit) = { navigateBlockManage() }
                })
                val appInfo = LocalContext.current.applicationInfo
                val isApplicationInSystemImage =
                    (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                SwitchPreference(object : SwitchPreferenceModel {
                    override val title: String =
                        stringResource(R.string.show_app_icon_in_launcher)
                    override val icon: @Composable (() -> Unit) = {
                        Icon(
                            rememberDrawablePainter(
                                LocalContext.current.packageManager.getApplicationIcon(LocalContext.current.packageName)
                            ), null, modifier = Modifier.size(SettingsDimension.itemIconSize), tint = Color.Unspecified
                        )
                    }
                    override val summary: () -> CharSequence = {
                        val launcherIntent = Intent(Intent.ACTION_MAIN)
                        launcherIntent.addCategory(Intent.CATEGORY_HOME)
                        launcherIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        SpaEnvironmentFactory.instance.appContext.getString(
                            R.string.show_icon_summary,
                            SpaEnvironmentFactory.instance.appContext.getString(R.string.app_name),
                            SpaEnvironmentFactory.instance.appContext.packageManager.getApplicationLabel(
                                SpaEnvironmentFactory.instance.appContext.packageManager.getApplicationInfo(
                                    SpaEnvironmentFactory.instance.appContext.packageManager.resolveActivity(
                                        launcherIntent,
                                        PackageManager.MATCH_DEFAULT_ONLY
                                    )!!.activityInfo.packageName, 0
                                )
                            )
                        )
                    }
                    override val checked: () -> Boolean? = {
                        BootCompletedReceiver.readShowLauncherIcon(
                            DefaultSharedPrefsManager.getSharedPreferencesStorage(
                                SpaEnvironmentFactory.instance.appContext
                            ),
                            SpaEnvironmentFactory.instance.appContext
                        )
                    }
                    override val onCheckedChange: ((newChecked: Boolean) -> Unit) = {
                        DefaultSharedPrefsManager.getSharedPreferencesStorage(
                            SpaEnvironmentFactory.instance.appContext
                        ).setBoolean("show_icon_in_l3", it)
                    }
                    override val changeable: () -> Boolean
                        get() = { isApplicationInSystemImage }
                })
            }
        }
    }

    override fun onKeyChanged(key: String?, reason: Int) {
        if (reason == DataChangeReason.UPDATE) {
            BootCompletedReceiver.toggleAppIcon(SpaEnvironmentFactory.instance.appContext)
        }
    }

    fun loadUsages() {
        val chartData = mutableListOf<PieChartData>()
        val stats = UsageUtils.getUsages(SpaEnvironmentFactory.instance.appContext, true)
        if (stats.isNotEmpty()) {
            for (usageStats in stats) {
                Log.d(
                    "UsageStats",
                    "Package: ${usageStats.first}, Foreground Time (ms): ${usageStats.second}"
                )
            }
            val top3Apps = stats.take(3)  // Take first 3 items after sorting
            var othersTime = 0L
            val packageManager = SpaEnvironmentFactory.instance.appContext.packageManager
            top3Apps.forEachIndexed { index, usageStats ->
                val totalTime = usageStats.second!!
                val hours = (totalTime / (1000 * 60 * 60)).toInt()
                val minutes = ((totalTime % (1000 * 60 * 60)) / (1000 * 60)).toInt()
                val seconds = ((totalTime % (1000 * 60)) / 1000).toInt()
                val locale =
                    ConfigurationCompat.getLocales(SpaEnvironmentFactory.instance.appContext.resources.configuration)[0]!!
                val timeString = String.format(
                    locale,
                    "%02d:%02d:%02d",
                    hours,
                    minutes,
                    seconds
                )
                Log.i(
                    javaClass.simpleName,
                    index.toString() + usageStats.first + timeString
                )
                chartData.add(
                    PieChartData(
                        usageStats.second!!.toFloat(),
                        packageManager.getApplicationLabel(
                            packageManager.getApplicationInfo(
                                usageStats.first!!,
                                0
                            )
                        ).toString()
                    )
                )
                Log.i(
                    javaClass.simpleName,
                    index.toString() + usageStats.first + usageStats.second!!
                )
            }
            if (stats.size > 3) {
                for (i in 3 until stats.size) {
                    val usageStats = stats[i]
                    othersTime += usageStats.second!!
                }
                val hours = (othersTime / (1000 * 60 * 60))
                val minutes = ((othersTime % (1000 * 60 * 60)) / (1000 * 60))
                val seconds = ((othersTime % (1000 * 60)) / 1000)
                val locale =
                    ConfigurationCompat.getLocales(SpaEnvironmentFactory.instance.appContext.resources.configuration)[0]!!
                val timeString = String.format(
                    locale,
                    "%02d:%02d:%02d",
                    hours,
                    minutes,
                    seconds
                )
                Log.i(javaClass.simpleName, "Others$timeString")
                chartData.add(
                    PieChartData(
                        othersTime.toFloat(),
                        SpaEnvironmentFactory.instance.appContext.getString(R.string.others)
                    )
                )
                Log.i(javaClass.simpleName, "Others$othersTime")
            }
        }
        usages = chartData
    }
}
