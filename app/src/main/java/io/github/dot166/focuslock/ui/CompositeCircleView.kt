package io.github.dot166.focuslock.ui

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import android.widget.TextView
import io.github.dot166.focuslock.R
import kotlin.math.roundToInt

/**
 * Configured to draw a set of contiguous partial circles via [PartialCircleView], which
 * are generated from the relative weight of values and corresponding colors given to
 * [.configure].
 */
class CompositeCircleView : FrameLayout {
    /** Values being represented by this circle.  */
    private lateinit var mValues: Array<Pair<Int, String>>

    /**
     * Angles toward the middle of each colored partial circle, calculated in
     * [.configure]. Can be used to position text relative to the
     * partial circles, by index.
     */
    private lateinit var mPartialCircleCenterAngles: FloatArray

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(
        context: Context, attrs: AttributeSet?,
        defStyleAttr: Int
    ) : super(context, attrs, defStyleAttr)

    /**
     * Configures the [CompositeCircleView] to draw a set of contiguous partial circles that
     * are generated from the relative weight of the given values and corresponding colors. The
     * first segment starts at the top, and drawing proceeds clockwise from there.
     *
     * @param startAngle the angle at which to start segments
     * @param values relative weights, used to size the partial circles
     * @param colors colors corresponding to relative weights
     * @param strokeWidth stroke width to apply to all contained partial circles
     * @param labels the permission labels to set the ContentDescription with % value
     */
    fun configure(
        startAngle: Float, values: Array<Pair<Int, String>>, colors: IntArray, strokeWidth: Int,
        labels: Array<TextView?>
    ) {
        var startAngle = startAngle
        removeAllViews()
        mValues = values

        // Get total values and number of values over 0.
        var total = 0f
        var numValidValues = 0
        for (i in values.indices) {
            total += values[i].first.toFloat()
            if (values[i].first > 0) {
                numValidValues++
            }
        }

        // Add small spacing to the first angle to make the little space between segments, but only
        // if we have more than one segment.
        if (values.size > 1) {
            startAngle = startAngle + (SEGMENT_ANGLE_SPACING_DEG * 0.5f)
        }
        mPartialCircleCenterAngles = FloatArray(values.size)

        // Number of degrees allocated to drawing circle segments.
        var allocatedDegrees = 360f
        if (values.size > 1) {
            allocatedDegrees -= (numValidValues * SEGMENT_ANGLE_SPACING_DEG).toFloat()
        }

        // Total label bump degrees so far.
        var totalBumpDegrees = 0f
        var labelBumps = 0

        for (i in values.indices) {
            if (values[i].first <= 0) {
                continue
            }

            val pcv = PartialCircleView(context)
            addView(pcv)
            pcv.setStartAngle(startAngle)
            pcv.setColor(colors[i])
            pcv.setStrokeWidth(strokeWidth.toFloat())

            // Calculate sweep, which is (value / total) * 360, keep track of segment center
            // angles for later reference.
            val sweepAngle = (values[i].first / total) * allocatedDegrees
            pcv.setSweepAngle(sweepAngle)

            if (labels[i] != null) {
                val percentage = ((values[i].first / total) * 100).roundToInt()
                val contextDescription = context.getString(
                    R.string.scrntime_usage_percent, labels[i]!!.getText(), percentage
                )
                labels[i]!!.setContentDescription(contextDescription)
            }

            mPartialCircleCenterAngles[i] = (startAngle + (sweepAngle * 0.5f)) % 360
            if (i > 0) {
                val angleDiff =
                    ((mPartialCircleCenterAngles[i] - mPartialCircleCenterAngles[i - 1])
                            + 360) % 360
                if (angleDiff < LABEL_BUMP_DEGREES) {
                    val bump: Float = LABEL_BUMP_DEGREES - angleDiff
                    mPartialCircleCenterAngles[i] += bump
                    totalBumpDegrees += bump
                    labelBumps++
                } else {
                    spreadPreviousLabelBumps(labelBumps, totalBumpDegrees, i)
                    totalBumpDegrees = 0f
                    labelBumps = 0
                }
            }

            // Move to next segment.
            startAngle += sweepAngle
            startAngle += SEGMENT_ANGLE_SPACING_DEG.toFloat()
            startAngle %= 360f
        }

        // If any label bumps remaining, spread now.
        spreadPreviousLabelBumps(labelBumps, totalBumpDegrees, values.size)
    }

    /**
     * If we've been bumping labels further from previous labels to make space, we use this method
     * to spread the bumps back along the circle, so that labels are as close as possible to their
     * corresponding segments.
     *
     * @param labelBumps total number of previous segments under the size threshold
     * @param totalBumpDegrees the total degrees to spread along previous labels
     * @param behindIndex the index behind which we were bumping labels
     */
    private fun spreadPreviousLabelBumps(
        labelBumps: Int,
        totalBumpDegrees: Float,
        behindIndex: Int
    ) {
        if (labelBumps > 0) {
            val spread = totalBumpDegrees * 0.5f
            for (i in 1..labelBumps + 1) {
                val index = behindIndex - i
                var angle = mPartialCircleCenterAngles[index]
                angle -= spread
                angle += 360f
                angle %= 360f
                mPartialCircleCenterAngles[index] = angle
            }
        }
    }

    /** Returns the value for the given index.  */
    fun getValue(index: Int): Int {
        return mValues[index].first
    }

    /** Returns the center angle for the given partial circle index.  */
    fun getPartialCircleCenterAngle(index: Int): Float {
        return mPartialCircleCenterAngles[index]
    }

    companion object {
        /** Spacing between circle segments in degrees.  */
        private const val SEGMENT_ANGLE_SPACING_DEG = 2

        /** How far apart to bump labels so that they have more space.  */
        private const val LABEL_BUMP_DEGREES = 15f
    }
}
