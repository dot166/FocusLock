package io.github.dot166.focuslock.ui.activity

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.android.settingslib.spa.framework.theme.SettingsTheme
import io.github.dot166.focuslock.R
import io.github.dot166.jlib.app.jActivity

class BlockScreenActivity : jActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.onBackPressedDispatcher.addCallback(this) {
            // Do nothing to prevent the activity from being closed by back gestures or buttons
        }
        val launcherIntent = Intent(Intent.ACTION_MAIN)
        launcherIntent.addCategory(Intent.CATEGORY_HOME)
        launcherIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        setContent {
            SettingsTheme {
                Box(contentAlignment = Alignment.Center) {
                    Column {
                        Text(text = stringResource(R.string.stay_focused_you_got_this))
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(onClick = {
                            startActivity(launcherIntent)
                            finishAffinity()
                        }) {
                            Text(
                                stringResource(
                                    R.string.exit,
                                    packageManager.getApplicationLabel(
                                        packageManager.getApplicationInfo(
                                            packageManager.resolveActivity(
                                                launcherIntent,
                                                PackageManager.MATCH_DEFAULT_ONLY
                                            )!!.activityInfo.packageName,
                                            0
                                        )
                                    )
                                )
                            )
                        }
                    }

                }
            }
        }
    }
}