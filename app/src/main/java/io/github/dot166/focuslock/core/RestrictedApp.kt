package io.github.dot166.focuslock.core

import android.content.Context
import android.content.pm.PackageManager
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.android.settingslib.spa.framework.common.SpaEnvironmentFactory
import com.android.settingslib.spa.framework.theme.SettingsDimension
import com.android.settingslib.spa.widget.preference.SpinnerPreference
import com.android.settingslib.spa.widget.preference.SpinnerPreferenceModel
import com.android.settingslib.spa.widget.ui.SpinnerOption
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import io.github.dot166.jlib.app.DefaultSharedPrefsManager

class RestrictedApp(val packageName: String, var allowedTimeInMinutes: Long) {
    @Composable
    fun ItemPreference(ctx: Context, onSetItemId: ((Int) -> Unit)) {
        val packageManager = ctx.packageManager
        val model = if (isPackageInstalled(packageManager, packageName)) {
            object : SpinnerPreferenceModel {
                override val title: String = packageManager.getApplicationLabel(
                    packageManager.getApplicationInfo(
                        packageName,
                        0
                    )
                ) as String
                override val summary: () -> CharSequence = { packageName }
                override val icon: @Composable () -> Unit = { Icon(rememberDrawablePainter(packageManager.getApplicationIcon(packageName)), null, modifier = Modifier.size(
                    SettingsDimension.itemIconSize), tint = Color.Unspecified) }
                override val onSetItemId: ((Int) -> Unit) = onSetItemId
                override val selectedItem: Int = getPosition(findDuration(allowedTimeInMinutes))
                override val list: List<SpinnerOption> = getDurationOptionsAsSpinnerOptions()
            }
        } else {
            object : SpinnerPreferenceModel {
                override val title: String = packageName
                override val icon: @Composable (() -> Unit) = {
                    Icon(rememberDrawablePainter(
                        AppCompatResources.getDrawable(
                            ctx,
                            android.R.drawable.sym_def_app_icon
                        )
                    ), null, modifier = Modifier.size(SettingsDimension.itemIconSize), tint = Color.Unspecified)
                }
                override val onSetItemId: ((Int) -> Unit) = onSetItemId
                override val selectedItem: Int = getPosition(findDuration(allowedTimeInMinutes))
                override val list: List<SpinnerOption> = getDurationOptionsAsSpinnerOptions()
            }
        }
        SpinnerPreference(model)
    }

    override fun toString(): String {
        val gson = GsonBuilder()
            .serializeNulls()
            .create()
        return gson.toJson(this)
    }

    companion object {
        internal fun fromString(str: String): RestrictedApp {
            val strArr: Array<String?> =
                str.split(",#\\*#,".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            return RestrictedApp(strArr[0]!!, strArr[1]!!.toLong())
        }

        fun getRestrictedApps(): List<RestrictedApp> {
            migrate()
            val gson = GsonBuilder()
                .serializeNulls()
                .create()
            val b = gson.fromJson(
                DefaultSharedPrefsManager.getSharedPreferencesStorage(SpaEnvironmentFactory.instance.appContext)
                    .getString("restrictedApps")
                    ?: "", object : TypeToken<MutableCollection<RestrictedApp>>() {})
            return b?.toMutableList() ?: mutableListOf()
        }

        fun saveRestrictedApps(list: List<RestrictedApp>) {
            val gson = GsonBuilder()
                .serializeNulls()
                .create()
            val json = gson.toJson(list)
            DefaultSharedPrefsManager.getSharedPreferencesStorage(SpaEnvironmentFactory.instance.appContext)
                .setString(
                    "restrictedApps",
                    json
                )
        }

        internal fun migrate() {
            val prefs =
                DefaultSharedPrefsManager.getSharedPreferencesStorage(SpaEnvironmentFactory.instance.appContext)
            if (prefs.contains("blockedApps")) {
                val oldPref =
                    prefs.getValue("blockedApps", Set::class.javaObjectType) as Set<String>?
                        ?: mutableSetOf()
                val newPref = mutableListOf<RestrictedApp>()
                for (app in oldPref) {
                    val newApp = fromString(app)
                    newPref.add(newApp)
                }
                saveRestrictedApps(newPref)
                prefs.setValue("blockedApps", Set::class.javaObjectType, null)
            }
        }

        fun isPackageInstalled(packageManager: PackageManager, packageName: String): Boolean {
            return try {
                packageManager.getApplicationInfo(packageName, 0)
                true
            } catch (_: PackageManager.NameNotFoundException) {
                false
            }
        }
    }

    internal fun getDurationOptionsAsSpinnerOptions(): List<SpinnerOption> {
        val list = mutableListOf<SpinnerOption>()
        for ((i, element) in durationOptions.withIndex()) {
            list.add(SpinnerOption(i, element.label))
        }
        return list
    }
}