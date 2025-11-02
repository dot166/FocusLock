package io.github.dot166.focuslock.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatImageView
import com.google.android.material.textview.MaterialTextView
import io.github.dot166.focuslock.R

class AppUsageItemView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    var appIconView: AppCompatImageView
    var appNameView: MaterialTextView
    var appTimeView: MaterialTextView

    init {
        LayoutInflater.from(context).inflate(R.layout.app_usage_item_view, this, true)
        appIconView = findViewById<AppCompatImageView>(R.id.appIconView)
        appNameView = findViewById<MaterialTextView>(R.id.appNameView)
        appTimeView = findViewById<MaterialTextView>(R.id.appTimeView)
        appNameView.isSelected = true
    }
}