package io.github.dot166.focuslock.core

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.android.settingslib.widget.SettingsSpinnerAdapter
import com.android.settingslib.widget.SettingsSpinnerPreference2

class RestrictedApp(val packageName: String, var allowedTimeInMinutes: Long) {
    private var mItemPref: SettingsSpinnerPreference2? = null

    fun getItemPreference(ctx: Context): SettingsSpinnerPreference2 {
        if (mItemPref == null) {
            mItemPref = SettingsSpinnerPreference2(ctx)
            val packageManager = ctx.packageManager
            mItemPref!!.summary = packageName
            mItemPref!!.title =
                packageManager.getApplicationLabel(packageManager.getApplicationInfo(packageName, 0)) as String
            mItemPref!!.icon = packageManager.getApplicationIcon(packageName)
            val adapter = SettingsSpinnerAdapter<DurationOption>(ctx)
            adapter.addAll(durationOptions)
            mItemPref!!.setAdapter(adapter)
        }
        return mItemPref!!
    }

    fun toLoggableString(): String {
        return javaClass.getName() + ".values = {packageName = " + this.packageName + ", allowedTimeInMinutes = " + this.allowedTimeInMinutes.toString() + "}"
    }

    override fun toString(): String {
        return this.packageName + ",#*#," + this.allowedTimeInMinutes.toString()
    }

    companion object {
        fun fromString(str: String): RestrictedApp {
            val strArr: Array<String?> =
                str.split(",#\\*#,".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            return RestrictedApp(strArr[0]!!, strArr[1]!!.toLong())
        }
    }
}