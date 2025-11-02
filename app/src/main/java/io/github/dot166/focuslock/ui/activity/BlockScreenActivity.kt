package io.github.dot166.focuslock.ui.activity

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.addCallback
import com.google.android.material.button.MaterialButton
import io.github.dot166.focuslock.R
import io.github.dot166.jlib.app.jActivity

class BlockScreenActivity : jActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_block_screen)
        this.onBackPressedDispatcher.addCallback(this) {
            // Do nothing to prevent the activity from being closed by back gestures or buttons
        }

        val exitButton = findViewById<MaterialButton>(R.id.exitButton)
        val launcherIntent = Intent(Intent.ACTION_MAIN)
        launcherIntent.addCategory(Intent.CATEGORY_HOME)
        launcherIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        exitButton.text = getString(R.string.exit, packageManager.getApplicationLabel(packageManager.getApplicationInfo(packageManager.resolveActivity(launcherIntent, PackageManager.MATCH_DEFAULT_ONLY)!!.activityInfo.packageName, 0)))
        exitButton.setOnClickListener {
            startActivity(launcherIntent)
            finishAffinity()
        }
    }
}