package io.github.dot166.focuslock.core

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class AppBlockerAccessibilityService : AccessibilityService() {

    private var currentApp: String? = null

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString() ?: return
            if (packageName != currentApp) {
                Log.d("AccessibilityService", "App switched to: $packageName")
                currentApp = packageName
                notifyAppSwitch(packageName)
            }
        }
    }

    private fun notifyAppSwitch(packageName: String) {
        val intent = Intent(this, AppBlockService::class.java)
        intent.action = AppBlockService.ACTION_APP_SWITCH
        intent.putExtra(AppBlockService.EXTRA_PACKAGE_NAME, packageName)
        startForegroundService(intent)
    }

    override fun onInterrupt() {
        // This service doesn't hold state, so nothing to do here
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i("AccessibilityService", "Accessibility Service Connected")
    }
}