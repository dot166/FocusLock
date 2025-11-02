package io.github.dot166.focuslock.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.View

/**
 * Draws a partial circular shape given some simple parameters:
 *  * Color via [.setColor]
 *  * Stroke width via [.setStrokeWidth]
 *  * Start angle via [.setStartAngle]
 *  * Sweep angle via [.setSweepAngle]
 */
class PartialCircleView : View {
    /** Some defaults that should be obvious if not changed.  */
    private var mColor = Color.MAGENTA
    private var mStrokeWidth = 2f
    private var mStartAngle = 0f
    private var mSweepAngle = 360f

    constructor(context: Context?) : super(context)

    /** Sets the color used to draw this partial circle.  */
    fun setColor(color: Int) {
        if (mColor != color) {
            mColor = color
            invalidate()
        }
    }

    /** Sets the stroke width used to draw this partial circle.  */
    fun setStrokeWidth(strokeWidth: Float) {
        if (mStrokeWidth != strokeWidth) {
            mStrokeWidth = strokeWidth
            invalidate()
        }
    }

    /** Sets the start angle used to draw this partial circle.  */
    fun setStartAngle(startAngle: Float) {
        if (mStartAngle != startAngle) {
            mStartAngle = startAngle
            invalidate()
        }
    }

    /** Sets the sweep angle used to draw this partial circle.  */
    fun setSweepAngle(sweepAngle: Float) {
        if (mSweepAngle != sweepAngle) {
            mSweepAngle = sweepAngle
            invalidate()
        }
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val width = width.toFloat()
        val height = height.toFloat()

        // We subtract half stroke width as to fit the outside edge of the circle just inside
        // our bounds.
        val radius = (if (width > height) height / 2 else width / 2) - (mStrokeWidth / 2)

        val paint = Paint()
        paint.setColor(mColor)
        paint.strokeWidth = mStrokeWidth
        paint.style = Paint.Style.FILL
        paint.isAntiAlias = true

        val oval = RectF()
        val x = width / 2
        val y = height / 2
        oval.set(
            x - radius,
            y - radius,
            x + radius,
            y + radius
        )
        paint.style = Paint.Style.STROKE
        canvas.drawArc(oval, mStartAngle, mSweepAngle, false, paint)
    }
}
