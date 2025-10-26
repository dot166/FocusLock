package io.github.dot166.focuslock

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.addCallback
import io.github.dot166.jlib.app.jActivity

class BlockScreenActivity : jActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_block_screen)
        this.onBackPressedDispatcher.addCallback(this) {
            // Do nothing to prevent the activity from being closed by back gestures or buttons
        }

        val exitButton = findViewById<Button>(R.id.exitButton)
        exitButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_MAIN)
            intent.addCategory(Intent.CATEGORY_HOME)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finishAffinity()
        }
    }
}