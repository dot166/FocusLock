package io.github.dot166.focuslock.ui.activity

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.LinearLayout
import io.github.dot166.focuslock.R
import io.github.dot166.focuslock.ui.widget.AppUsageItemView
import io.github.dot166.focuslock.utils.UsageUtils
import java.util.Locale

class AppUsageActivity : CoreActivity() {

    @SuppressLint("BatteryLife")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_usage)
        configureToolBar(findViewById(R.id.toolbar))

        val appsList = findViewById<LinearLayout>(R.id.appsList)
        val packageManager = packageManager
        val usages = UsageUtils.getUsages(this, true)

        usages.sortedBy {
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(it.first!!, 0)).toString().lowercase(Locale.getDefault())
        }

        for (i in 0 until usages.size) {
            val appInfo = packageManager.getApplicationInfo(usages[i].first!!, 0)
            val totalTime = usages[i].second!!
            val hours = (totalTime / (1000 * 60 * 60)).toInt()
            val minutes = ((totalTime % (1000 * 60 * 60)) / (1000 * 60)).toInt()
            val seconds = ((totalTime % (1000 * 60)) / 1000).toInt()

            val timeString = String.format("%02d:%02d:%02d", hours, minutes, seconds)
            val appUsageItemView = AppUsageItemView(this)
            appUsageItemView.appTimeView.text = timeString
            appUsageItemView.appNameView.text = packageManager.getApplicationLabel(appInfo)
            appUsageItemView.appIconView.setImageDrawable(packageManager.getApplicationIcon(appInfo))
            appsList.addView(appUsageItemView)
        }
    }
}