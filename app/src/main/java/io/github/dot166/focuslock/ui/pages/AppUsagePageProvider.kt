package io.github.dot166.focuslock.ui.pages

import android.content.pm.ApplicationInfo
import android.os.Bundle
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
import androidx.core.os.ConfigurationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.android.settingslib.spa.framework.common.SettingsPageProvider
import com.android.settingslib.spa.framework.common.SpaEnvironmentFactory
import com.android.settingslib.spa.framework.compose.rememberDrawablePainter
import com.android.settingslib.spa.framework.theme.SettingsDimension
import com.android.settingslib.spa.widget.preference.Preference
import com.android.settingslib.spa.widget.preference.PreferenceModel
import com.android.settingslib.spa.widget.scaffold.RegularScaffold
import com.android.settingslib.spa.widget.ui.Category
import com.android.settingslib.spa.widget.ui.CircularLoadingBar
import io.github.dot166.focuslock.R
import io.github.dot166.focuslock.utils.UsageUtils

object AppUsagePageProvider : SettingsPageProvider {
    override val name = "allUsage"
    var usages by mutableStateOf<List<Pair<ApplicationInfo, String>>>(listOf())
    var loading by mutableStateOf(true)

    override fun getTitle(arguments: Bundle?): String {
        return SpaEnvironmentFactory.instance.appContext.getString(R.string.view_app_usage)
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
                        loadUsages()
                    }

                    Lifecycle.State.STARTED -> {}
                    Lifecycle.State.RESUMED -> {
                        loadUsages()
                    }
                }
            }
            if (loading) {
                CircularLoadingBar(loading)
            } else {
                Category {
                    val packageManager = LocalContext.current.packageManager
                    for (it in usages) {
                        Preference(object : PreferenceModel {
                            override val title: String =
                                packageManager.getApplicationLabel(it.first) as String
                            override val icon: @Composable (() -> Unit) =
                                {
                                    Icon(
                                        rememberDrawablePainter(packageManager.getApplicationIcon(it.first)),
                                        null,
                                        modifier = Modifier.size(
                                            SettingsDimension.itemIconSize
                                        ),
                                        tint = Color.Unspecified
                                    )
                                }
                            override val summary: () -> CharSequence = { it.second }
                        })
                    }
                }
            }
        }
    }

    fun loadUsages() {
        loading = true
        val data = mutableListOf<Pair<ApplicationInfo, String>>()
        val stats = UsageUtils.getUsages(SpaEnvironmentFactory.instance.appContext, true)
        val packageManager = SpaEnvironmentFactory.instance.appContext.packageManager
        for (i in stats.indices) {
            val appInfo = packageManager.getApplicationInfo(stats[i].first!!, 0)
            val totalTime = stats[i].second!!
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
            data.add(Pair(appInfo, timeString))
        }
        usages = data
        loading = false
    }
}
