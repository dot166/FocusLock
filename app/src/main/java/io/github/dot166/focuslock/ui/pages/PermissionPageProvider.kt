package io.github.dot166.focuslock.ui.pages

import android.annotation.SuppressLint
import android.content.Context
import android.content.Context.POWER_SERVICE
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.android.settingslib.spa.framework.common.SettingsPageProvider
import com.android.settingslib.spa.framework.common.SpaEnvironmentFactory
import com.android.settingslib.spa.widget.preference.SwitchPreference
import com.android.settingslib.spa.widget.preference.SwitchPreferenceModel
import com.android.settingslib.spa.widget.scaffold.RegularScaffold
import com.android.settingslib.spa.widget.ui.Category
import com.android.settingslib.spa.widget.ui.CircularLoadingBar
import io.github.dot166.focuslock.R
import io.github.dot166.focuslock.core.AppBlockerAccessibilityService
import io.github.dot166.focuslock.utils.PermissionUtils
import io.github.dot166.focuslock.utils.UsageUtils.hasUsageAccess


object PermissionPageProvider : SettingsPageProvider {
    override val name = "perms"
    var permissions by mutableStateOf<List<PermissionUtils.Permission>>(listOf())
    var loading by mutableStateOf(true)

    override fun getTitle(arguments: Bundle?): String {
        return SpaEnvironmentFactory.instance.appContext.getString(R.string.permissions)
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
                        refreshPermissions()
                    }

                    Lifecycle.State.STARTED -> {}
                    Lifecycle.State.RESUMED -> {
                        refreshPermissions()
                    }
                }
            }
            if (loading) {
                CircularLoadingBar(loading)
            } else {
                Category {
                    for (permission in permissions) {
                        SwitchPreference(object : SwitchPreferenceModel {
                            override val title: String
                                get() = permission.name
                            override val checked: () -> Boolean?
                                get() = { permission.granted }
                            override val changeable: () -> Boolean
                                get() = { !permission.granted }
                            override val onCheckedChange: ((newChecked: Boolean) -> Unit)
                                get() = { permission.grant() }
                        })
                    }
                }
            }
        }
    }

    @SuppressLint("BatteryLife")
    fun refreshPermissions() {
        loading = true
        val ctx = SpaEnvironmentFactory.instance.appContext
        val list = mutableListOf<PermissionUtils.Permission>()
        list.add(PermissionUtils.Permission("Usage Access Permission", hasUsageAccess(ctx)) {
            val intent =
                Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS, "package:${ctx.packageName}".toUri())
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ctx.startActivity(intent)
        })
        list.add(PermissionUtils.Permission("Overlay Permission", Settings.canDrawOverlays(ctx)) {
            val intent =
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, "package:${ctx.packageName}".toUri())
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ctx.startActivity(intent)
        })
        val pm = ctx.getSystemService(POWER_SERVICE) as PowerManager
        list.add(PermissionUtils.Permission("Battery Optimization Exemption", pm.isIgnoringBatteryOptimizations(ctx.packageName)) {
            val intent =
                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, "package:${ctx.packageName}".toUri())
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ctx.startActivity(intent)
        })
        list.add(PermissionUtils.Permission("Accessibility Service", PermissionUtils.isAccessibilityServiceEnabled(ctx, AppBlockerAccessibilityService::class.java)) {
            val intent =
                Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ctx.startActivity(intent)
            Toast.makeText(ctx, ctx.getString(R.string.please_find_and_enable_the_service, ctx.getString(R.string.app_name)), Toast.LENGTH_LONG).show()
        })
        list.add(PermissionUtils.Permission("Notification Permission", isNotificationsGranted(ctx)) {
            val intent =
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:${ctx.packageName}".toUri())
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ctx.startActivity(intent)
        })
        permissions = list
        loading = false
    }

    fun isNotificationsGranted(ctx: Context): Boolean {
        return if (Build.VERSION.SDK_INT_FULL >= Build.VERSION_CODES_FULL.TIRAMISU) {
            NotificationManagerCompat.from(ctx).areNotificationsEnabled()
        } else {
            true // notification permission was added in AOSP 33
        }
    }
}