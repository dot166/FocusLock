package io.github.dot166.focuslock

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatImageView
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.textview.MaterialTextView

class AppItemView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    var appIconView: AppCompatImageView
    var appNameView: MaterialTextView
    var appPackageNameView: MaterialTextView
    var checkBox: MaterialCheckBox

    init {
        LayoutInflater.from(context).inflate(R.layout.app_item_view, this, true)
        appIconView = findViewById<AppCompatImageView>(R.id.appIconView)
        appNameView = findViewById<MaterialTextView>(R.id.appNameView)
        appPackageNameView = findViewById<MaterialTextView>(R.id.appPackageNameView)
        checkBox = findViewById<MaterialCheckBox>(R.id.checkbox)
        appNameView.isSelected = true
        appPackageNameView.isSelected = true
    }
}