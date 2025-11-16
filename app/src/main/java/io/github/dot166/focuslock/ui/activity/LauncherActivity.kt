package io.github.dot166.focuslock.ui.activity

import android.content.Intent
import android.os.Bundle
import io.github.dot166.jlib.app.jActivity

class LauncherActivity : jActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        invokeSettings()
        finish()
    }

    private fun invokeSettings() {
        val intent = Intent()
        intent.setClass(this, MainActivity::class.java)
        intent.setFlags(
            Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                    or Intent.FLAG_ACTIVITY_CLEAR_TOP
        )
        startActivity(intent)
    }
}
