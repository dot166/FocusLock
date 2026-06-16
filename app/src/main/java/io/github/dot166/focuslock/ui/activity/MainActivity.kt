package io.github.dot166.focuslock.ui.activity

import android.content.Intent
import android.os.Bundle
import io.github.dot166.focuslock.core.AppBlockService
import io.github.dot166.jlib.app.PreferenceMainActivity

class MainActivity: PreferenceMainActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intent = Intent(this, AppBlockService::class.java)
        startForegroundService(intent)
    }
}