package io.github.dot166.focuslock.ui

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.widget.TextView
import androidx.annotation.AttrRes
import androidx.annotation.StyleRes
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import io.github.dot166.focuslock.R

/**
 * A Preference for the screen time graphic.
 */
class ScreenTimeGraphicPreference : Preference {
    /** app time to count mapping.  */
    private var mUsages: MutableMap<String, Pair<Int, String>> =
        HashMap()

    constructor(
        context: Context, attrs: AttributeSet?,
        @AttrRes defStyleAttr: Int, @StyleRes defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes) {
        init()
    }

    constructor(
        context: Context, attrs: AttributeSet?,
        @AttrRes defStyleAttr: Int
    ) : super(context, attrs, defStyleAttr) {
        init()
    }

    constructor(
        context: Context,
        attrs: AttributeSet?
    ) : super(context, attrs) {
        init()
    }

    constructor(context: Context) : super(context) {
        init()
    }

    private fun init() {
        layoutResource = R.layout.screen_time_graphic
        isSelectable = false
    }

    /** Sets permission group usages: map of group name to usage count.  */
    fun setUsages(usages: MutableMap<String, Pair<Int, String>>) {
        if (mUsages != usages) {
            mUsages = usages
            notifyChanged()
        }
    }

    @SuppressLint("Recycle")
    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        val isUsagesEmpty = isUsagesEmpty

        val ccv =
            holder.findViewById(R.id.composite_circle_view) as CompositeCircleView
        val ccvl = holder.findViewById(
            R.id.composite_circle_view_labeler
        ) as CompositeCircleViewLabeler

        // Set center text.
        val centerLabel = TextView(context)
        centerLabel.textAlignment = TextView.TEXT_ALIGNMENT_CENTER

        centerLabel.text = context.getString(R.string.scrntime_label_24h)
        centerLabel.setTextAppearance(R.style.ScreenTimeGraphicLabel)

        val colourOne =
            context.obtainStyledAttributes(intArrayOf(androidx.appcompat.R.attr.colorPrimary))
                .getColor(0, -0x3f35)
        val colourTwo =
            context.obtainStyledAttributes(intArrayOf(com.google.android.material.R.attr.colorOnPrimary))
                .getColor(0, -0x980000)
        val colourThree =
            context.obtainStyledAttributes(intArrayOf(com.google.android.material.R.attr.colorSecondary))
                .getColor(0, -0xff9800)
        val colorOther =
            context.obtainStyledAttributes(intArrayOf(com.google.android.material.R.attr.colorOnSecondary))
                .getColor(0, -0xffff98)

        // Create labels, counts, and colors.
        val labels: Array<TextView?>
        val counts: Array<Pair<Int, String>>
        val colors: IntArray?
        if (isUsagesEmpty) {
            // Special case if usages are empty.
            labels = arrayOf(TextView(context))
            labels[0] = null
            counts = arrayOf(Pair(0, ""))
            colors = intArrayOf(0)
        } else {
            labels = arrayOf(
                TextView(context),
                TextView(context),
                TextView(context),
                TextView(context)
            )
            counts = arrayOf(
                getUsageCount("App 1"),
                getUsageCount("App 2"),
                getUsageCount("App 3"),
                getUsageCount("Others")
            )
            var total = 0
            for (i in counts.indices) {
                total = total + counts[i].first
                labels[i]!!.text = counts[i].second
            }
            val hours = (total / (1000 * 60 * 60))
            val minutes = ((total % (1000 * 60 * 60)) / (1000 * 60))
            val seconds = ((total % (1000 * 60)) / 1000)
            val timeString = String.format("Today\n%02dh%02dm%02ds", hours, minutes, seconds)
            centerLabel.text = timeString
            colors = intArrayOf(
                colourOne,
                colourTwo,
                colourThree,
                colorOther
            )

            // Set label styles.
            for (i in labels.indices) {
                if (labels[i] != null) {
                    labels[i]!!.setTextAppearance(R.style.ScreenTimeGraphicLabel)
                }
            }
        }

        // Get circle-related dimensions.
        val outValue = TypedValue()
        context.resources.getValue(
            R.dimen.scrntime_label_radius_scalar,
            outValue, true
        )
        val labelRadiusScalar = outValue.float
        val circleStrokeWidth = context.resources.getDimension(
            R.dimen.scrntime_circle_stroke_width
        ).toInt()

        // Configure circle and labeler.
        ccvl.configure(R.id.composite_circle_view, centerLabel, labels, labelRadiusScalar)
        // Start at angle 300 (top right) to allow for small segments.
        ccv.configure(300f, counts, colors, circleStrokeWidth, labels)
    }

    private fun getUsageCount(group: String?): Pair<Int, String> {
        val count = mUsages[group]
        if (count == null) {
            return Pair(0, "")
        }
        return count
    }

    private fun getUsageCountExcluding(vararg excludeGroups: String?): Int {
        var count = 0
        val exclude = listOf(*excludeGroups)
        for (entry in mUsages.entries) {
            if (exclude.indexOf(entry.key) >= 0) {
                continue
            }
            count += entry.value.first
        }
        return count
    }

    private val isUsagesEmpty: Boolean
        get() = getUsageCountExcluding() == 0
}
