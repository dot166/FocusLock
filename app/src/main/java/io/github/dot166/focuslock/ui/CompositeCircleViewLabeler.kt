package io.github.dot166.focuslock.ui

import android.content.Context
import android.util.AttributeSet
import android.view.WindowManager
import android.widget.RelativeLayout
import android.widget.TextView
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import androidx.core.view.size

/**
 * Encapsulates a [CompositeCircleView], labeling each of its colored partial circles.
 */
class CompositeCircleViewLabeler : RelativeLayout {
    private var mCircleId = 0
    private var mCenterLabel: TextView? = null
    private lateinit var mLabels: Array<TextView?>
    private var mLabelRadiusScalar = 0f

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(
        context: Context, attrs: AttributeSet?,
        defStyleAttr: Int
    ) : super(context, attrs, defStyleAttr)

    /**
     * Sets labels to surround the contained [CompositeCircleView] with, and the radius
     * scalar to place them at.
     *
     * @param circleId view ID of the circle being labeled
     * @param centerLabel the center label
     * @param labels labels labels to position next to each circle value segment
     * @param labelRadiusScalar scalar to multiply the contained circle radius by to get the
     * radius at which we want to show labels
     */
    fun configure(
        circleId: Int, centerLabel: TextView?, labels: Array<TextView?>,
        labelRadiusScalar: Float
    ) {
        // Remove previous text content first.
        run {
            var i = 0
            while (i < size) {
                if (getChildAt(i) is TextView) {
                    removeViewAt(i)
                    i--
                }
                i++
            }
        }
        mCircleId = circleId
        mCenterLabel = centerLabel
        if (centerLabel != null) {
            addView(centerLabel)
        }
        mLabels = labels
        for (i in labels.indices) {
            if (labels[i] != null) {
                addView(labels[i])
            }
        }
        mLabelRadiusScalar = labelRadiusScalar
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)

        // Gather CCV geometry.
        val ccv = findViewById<CompositeCircleView>(mCircleId)
        val ccvWidth = ccv.width
        val ccvHeight = ccv.height
        val ccvCenterX = ccv.x + (ccvWidth * 0.5f)
        val ccvCenterY = ccv.y + (ccvHeight * 0.5f)
        val ccvRadius = min(ccvWidth, ccvHeight) * 0.5f
        val labelRadius = ccvRadius * mLabelRadiusScalar
        val centerLabelX = (ccvCenterX - (mCenterLabel!!.width * 0.5f)).toInt()
        val centerLabelY = (ccvCenterY - (mCenterLabel!!.height * 0.5f)).toInt()

        // Position center label.
        if (mCenterLabel != null) {
            mCenterLabel!!.x = centerLabelX.toFloat()
            mCenterLabel!!.y = centerLabelY.toFloat()
        }

        // For each provided label, determine position angle.
        for (i in mLabels.indices) {
            val label = mLabels[i]
            if (label == null) {
                continue
            }
            label.visibility = if (ccv.getValue(i) > 0) VISIBLE else GONE
            label.measure(0, 0)
            val width = label.measuredWidth
            val height = label.measuredHeight

            // For circle path, top angle is 270d. Convert to unit circle rads.
            val angle = Math.toRadians((360 - ccv.getPartialCircleCenterAngle(i)).toDouble())
            var x = ccvCenterX + (cos(angle) * labelRadius)
            var y = ccvCenterY - (sin(angle) * labelRadius)

            // Determine anchor corner for text, adjust accordingly.
            if (angle < (Math.PI * 0.5)) {
                y -= height.toDouble()
            } else if (angle < Math.PI) {
                x -= width.toDouble()
                y -= height.toDouble()
            } else if (angle < (Math.PI * 1.5)) {
                x -= width.toDouble()
            }
            val wm = context.getSystemService(WindowManager::class.java)
            val metrics = wm.currentWindowMetrics
            val maxX = metrics.bounds.right

            var offset: Double
            if (x < 0) {
                x = 0.0
            } else if ((x + width) > maxX) {
                offset = x + width - maxX
                x -= offset
            }

            val labelMinX = x
            val labelMaxX = x + width
            val labelMinY = y
            val labelMaxY = y + height
            val centerLabelMinX = centerLabelX.toDouble()
            val centerLabelMaxX = (centerLabelX + mCenterLabel!!.width).toDouble()
            val centerLabelMinY = centerLabelY.toDouble()
            val centerLabelMaxY = (centerLabelY + mCenterLabel!!.height).toDouble()

            if (isOverlapping(
                    labelMinX, labelMaxX, labelMinY, labelMaxY,
                    centerLabelMinX, centerLabelMaxX, centerLabelMinY, centerLabelMaxY
                )
            ) {
                if (shouldMoveLabelUp(labelMinY, labelMaxY, centerLabelMinY, centerLabelMaxY)) {
                    y += centerLabelMaxY - labelMinY
                } else {
                    y -= labelMaxY - centerLabelMinY
                }
            }

            label.x = x.toInt().toFloat()
            label.y = y.toInt().toFloat()
        }
    }

    /**
     * Given the minimum and maximum X and Y values of the label and center label,
     * determine whether they overlap.
     * @return whether the label overlaps with the center label
     */
    private fun isOverlapping(
        labelMinX: Double, labelMaxX: Double, labelMinY: Double, labelMaxY: Double,
        centerLabelMinX: Double, centerLabelMaxX: Double,
        centerLabelMinY: Double, centerLabelMaxY: Double
    ): Boolean {
        // If they overlap, the condition inside the parentheses will not be true
        return !(labelMinY > centerLabelMaxY || labelMaxY < centerLabelMinY || labelMinX > centerLabelMaxX || labelMaxX < centerLabelMinX)
    }

    /**
     * Determines the minimum distance to move the label along the Y axis in order to make it
     * not overlap with the center label. Up means the positive direction in Java.
     * @return whether we should move the label up
     */
    private fun shouldMoveLabelUp(
        labelMinY: Double, labelMaxY: Double, centerLabelMinY: Double, centerLabelMaxY: Double
    ): Boolean {
        // this returns (the distance to move the label up) < (the distance to move the label down)
        return centerLabelMaxY - labelMinY < labelMaxY - centerLabelMinY
    }
}
