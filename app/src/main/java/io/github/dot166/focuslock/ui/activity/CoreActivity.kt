package io.github.dot166.focuslock.ui.activity

import android.os.Bundle
import android.view.MenuItem
import com.google.android.material.appbar.MaterialToolbar
import io.github.dot166.jlib.app.jActivity

open class CoreActivity  : jActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    fun configureToolBar(toolbar: MaterialToolbar) {
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setHomeActionContentDescription(androidx.appcompat.R.string.abc_action_bar_up_description)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}