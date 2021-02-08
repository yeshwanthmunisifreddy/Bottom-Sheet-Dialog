package technology.nine.cred.utills

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import technology.nine.cred.R

class SeekBar : View {
    /**
     * The current points value.
     */
    private var mPoints = MIN

    /**
     * The min value of progress value.
     */
    private var mMin = MIN

    /**
     * The Maximum value that this SeekArc can be set to
     */
    private var mMax = MAX

    /**
     * The increment/decrement value for each movement of progress.
     */
    var step = 10

    /**
     * The Drawable for the seek arc thumbnail
     */
    private var mIndicatorIcon: Drawable? = null
    private var mProgressWidth = 12
    private var mArcWidth = 12
    var isClockwise = true
    private var mEnabled = true
    //
    // internal variables
    //
    /**
     * The counts of point update to determine whether to change previous progress.
     */
    private var mUpdateTimes = 0
    private var mPreviousProgress = -1f
    private var mCurrentProgress = 0f

    /**
     * Determine whether reach max of point.
     */
    private var isMax = false

    /**
     * Determine whether reach min of point.
     */
    private var isMin = false
    private var mArcRadius = 0
    private val mArcRect = RectF()
    private var mArcPaint: Paint? = null
    private var mProgressSweep = 0f
    private var mProgressPaint: Paint? = null
    private var mTranslateX = 0
    private var mTranslateY = 0

    // the (x, y) coordinator of indicator icon
    private var mIndicatorIconX = 0
    private var mIndicatorIconY = 0

    /**
     * The current touch angle of arc.
     */
    private var mTouchAngle = 0.0
    private var mOnSeekBarAttrChangeListener: OnSeekBarAttrChangeListener? = null

    constructor(context: Context) : super(context) {
        init(context, null)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context, attrs)
    }

    private fun init(context: Context, attrs: AttributeSet?) {
        val density = resources.displayMetrics.density

        // Defaults, may need to link this into theme settings
        var arcColor = ContextCompat.getColor(context, R.color.pink_200)
        var progressColor = ContextCompat.getColor(context, R.color.pink_500)
        mProgressWidth = (mProgressWidth * density).toInt()
        mArcWidth = (mArcWidth * density).toInt()
        mIndicatorIcon = ContextCompat.getDrawable(context, R.drawable.ic_circle_diagonal_line)
        if (attrs != null) {
            // Attribute initialization
            val a = context.obtainStyledAttributes(
                attrs,
                R.styleable.SeekBarAttr, 0, 0
            )
            val indicatorIcon = a.getDrawable(R.styleable.SeekBarAttr_indicatorIcon)
            if (indicatorIcon != null) mIndicatorIcon = indicatorIcon
            val indicatorIconHalfWidth = mIndicatorIcon!!.intrinsicWidth / 2
            val indicatorIconHalfHeight = mIndicatorIcon!!.intrinsicHeight / 2
            mIndicatorIcon!!.setBounds(
                -indicatorIconHalfWidth, -indicatorIconHalfHeight, indicatorIconHalfWidth,
                indicatorIconHalfHeight
            )
            mPoints = a.getInteger(R.styleable.SeekBarAttr_points, mPoints)
            mMin = a.getInteger(R.styleable.SeekBarAttr_min, mMin)
            mMax = a.getInteger(R.styleable.SeekBarAttr_max, mMax)
            step = a.getInteger(R.styleable.SeekBarAttr_step, step)
            mProgressWidth =
                a.getDimension(R.styleable.SeekBarAttr_progressWidth, mProgressWidth.toFloat())
                    .toInt()
            progressColor = a.getColor(R.styleable.SeekBarAttr_progressColor, progressColor)
            mArcWidth =
                a.getDimension(R.styleable.SeekBarAttr_arcWidth, mArcWidth.toFloat()).toInt()
            arcColor = a.getColor(R.styleable.SeekBarAttr_arcColor, arcColor)
            isClockwise = a.getBoolean(
                R.styleable.SeekBarAttr_clockwise,
                isClockwise
            )
            mEnabled = a.getBoolean(R.styleable.SeekBarAttr_enabled, mEnabled)
            a.recycle()
        }

        // range check
        mPoints = if (mPoints > mMax) mMax else mPoints
        mPoints = if (mPoints < mMin) mMin else mPoints
        mProgressSweep = mPoints.toFloat() / valuePerDegree()
        mArcPaint = Paint()
        mArcPaint!!.color = arcColor
        mArcPaint!!.isAntiAlias = true
        mArcPaint!!.style = Paint.Style.STROKE
        mArcPaint!!.strokeWidth = mArcWidth.toFloat()
        mProgressPaint = Paint()
        mProgressPaint!!.color = progressColor
        mProgressPaint!!.isAntiAlias = true
        mProgressPaint!!.style = Paint.Style.STROKE
        mProgressPaint!!.strokeWidth = mProgressWidth.toFloat()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = getDefaultSize(suggestedMinimumWidth, widthMeasureSpec)
        val height = getDefaultSize(suggestedMinimumHeight, heightMeasureSpec)
        val min = Math.min(width, height)
        mTranslateX = (width * 0.5f).toInt()
        mTranslateY = (height * 0.5f).toInt()
        val arcDiameter = min - paddingLeft
        mArcRadius = arcDiameter / 2
        val top = (height / 2 - arcDiameter / 2).toFloat()
        val left = (width / 2 - arcDiameter / 2).toFloat()
        mArcRect[left, top, left + arcDiameter] = top + arcDiameter
        updateIndicatorIconPosition()
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    override fun onDraw(canvas: Canvas) {
        if (!isClockwise) {
            canvas.scale(-1f, 1f, mArcRect.centerX(), mArcRect.centerY())
        }

        // draw the arc and progress
        canvas.drawArc(mArcRect, ANGLE_OFFSET.toFloat(), 360f, false, mArcPaint!!)
        canvas.drawArc(mArcRect, ANGLE_OFFSET.toFloat(), mProgressSweep, false, mProgressPaint!!)
        if (mEnabled) {
            // draw the indicator icon
            canvas.translate(
                (mTranslateX - mIndicatorIconX).toFloat(),
                (mTranslateY - mIndicatorIconY).toFloat()
            )
            mIndicatorIcon!!.draw(canvas)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (mEnabled) {
            this.parent.requestDisallowInterceptTouchEvent(true)
            when (event.action) {
                MotionEvent.ACTION_DOWN -> if (mOnSeekBarAttrChangeListener != null) mOnSeekBarAttrChangeListener!!.onStartTrackingTouch(
                    this
                )
                MotionEvent.ACTION_MOVE -> updateOnTouch(event)
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (mOnSeekBarAttrChangeListener != null) mOnSeekBarAttrChangeListener!!.onStopTrackingTouch(
                        this
                    )
                    isPressed = false
                    this.parent.requestDisallowInterceptTouchEvent(false)
                }
            }
            return true
        }
        return false
    }

    override fun drawableStateChanged() {
        super.drawableStateChanged()
        if (mIndicatorIcon != null && mIndicatorIcon!!.isStateful) {
            val state = drawableState
            mIndicatorIcon!!.state = state
        }
        invalidate()
    }
    private fun updateOnTouch(event: MotionEvent) {
        isPressed = true
        mTouchAngle = convertTouchEventPointToAngle(event.x, event.y)
        val progress = convertAngleToProgress(mTouchAngle)
        updateProgress(progress, true)
    }

    private fun convertTouchEventPointToAngle(xPos: Float, yPos: Float): Double {
        // transform touch coordinate into component coordinate
        var x = xPos - mTranslateX
        val y = yPos - mTranslateY
        x = if (isClockwise) x else -x
        var angle = Math.toDegrees(Math.atan2(y.toDouble(), x.toDouble()) + Math.PI / 2)
        angle = if (angle < 0) angle + 360 else angle
        //		System.out.printf("(%f, %f) %f\n", x, y, angle);
        return angle
    }

    private fun convertAngleToProgress(angle: Double): Int {
        return Math.round(valuePerDegree() * angle).toInt()
    }

    private fun valuePerDegree(): Float {
        return mMax.toFloat() / 360.0f
    }

    private fun updateIndicatorIconPosition() {
        val thumbAngle = (mProgressSweep + 90).toInt()
        mIndicatorIconX = (mArcRadius * Math.cos(Math.toRadians(thumbAngle.toDouble()))).toInt()
        mIndicatorIconY = (mArcRadius * Math.sin(Math.toRadians(thumbAngle.toDouble()))).toInt()
    }

    private fun updateProgress(progress: Int, fromUser: Boolean) {

        // detect points change closed to max or min
        var progress = progress
        val maxDetectValue = (mMax.toDouble() * 0.95).toInt()
        val minDetectValue = (mMax.toDouble() * 0.05).toInt() + mMin
        //		System.out.printf("(%d, %d) / (%d, %d)\n", mMax, mMin, maxDetectValue, minDetectValue);
        mUpdateTimes++
        if (progress == INVALID_VALUE) {
            return
        }

        // avoid accidentally touch to become max from original point
        if (progress > maxDetectValue && mPreviousProgress == INVALID_VALUE.toFloat()) {
//			System.out.printf("Skip (%d) %.0f -> %.0f %s\n",
//					progress, mPreviousProgress, mCurrentProgress, isMax ? "Max" : "");
            return
        }


        // record previous and current progress change
        if (mUpdateTimes == 1) {
            mCurrentProgress = progress.toFloat()
        } else {
            mPreviousProgress = mCurrentProgress
            mCurrentProgress = progress.toFloat()
        }

//		if (mPreviousProgress != mCurrentProgress)
//			System.out.printf("Progress (%d)(%f) %.0f -> %.0f (%s, %s)\n",
//					progress, mTouchAngle,
//					mPreviousProgress, mCurrentProgress,
//					isMax ? "Max" : "",
//					isMin ? "Min" : "");
        mPoints = progress - progress % step

        if (mUpdateTimes > 1 && !isMin && !isMax) {
            if (mPreviousProgress >= maxDetectValue && mCurrentProgress <= minDetectValue && mPreviousProgress > mCurrentProgress) {
                isMax = true
                progress = mMax
                mPoints = mMax
                //				System.out.println("Reach Max " + progress);
                if (mOnSeekBarAttrChangeListener != null) {
                    mOnSeekBarAttrChangeListener!!
                        .onPointsChanged(this, progress, fromUser)
                    return
                }
            } else if (mCurrentProgress >= maxDetectValue && mPreviousProgress <= minDetectValue && mCurrentProgress > mPreviousProgress || mCurrentProgress <= mMin) {
                isMin = true
                progress = mMin
                mPoints = mMin
                //				Log.d("Reach", "Reach Min " + progress);
                if (mOnSeekBarAttrChangeListener != null) {
                    mOnSeekBarAttrChangeListener!!
                        .onPointsChanged(this, progress, fromUser)
                    return
                }
            }
            invalidate()
        } else {

            // Detect whether decreasing from max or increasing from min, to unlock the update event.
            // Make sure to check in detect range only.
            if (isMax and (mCurrentProgress < mPreviousProgress) && mCurrentProgress >= maxDetectValue) {
//				System.out.println("Unlock max");
                isMax = false
            }
            if (isMin
                && mPreviousProgress < mCurrentProgress
                && mPreviousProgress <= minDetectValue && mCurrentProgress <= minDetectValue && mPoints >= mMin
            ) {
//				Log.d("Unlock", String.format("Unlock min %.0f, %.0f\n", mPreviousProgress, mCurrentProgress));
                isMin = false
            }
        }
        if (!isMax && !isMin) {
            progress = if (progress > mMax) mMax else progress
            progress = if (progress < mMin) mMin else progress
            if (mOnSeekBarAttrChangeListener != null) {
                progress = progress - progress % step
                mOnSeekBarAttrChangeListener!!
                    .onPointsChanged(this, progress, fromUser)
            }
            mProgressSweep = progress.toFloat() / valuePerDegree()
            //			if (mPreviousProgress != mCurrentProgress)
//				System.out.printf("-- %d, %d, %f\n", progress, mPoints, mProgressSweep);
            updateIndicatorIconPosition()
            invalidate()
        }
    }

    interface OnSeekBarAttrChangeListener {

        fun onPointsChanged(seekBar: SeekBar?, points: Int, fromUser: Boolean)
        fun onStartTrackingTouch(seekBar: SeekBar?)
        fun onStopTrackingTouch(SeekBar: SeekBar?)
    }

    var points: Int
        get() = mPoints
        set(points) {
            var points = points
            points = if (points > mMax) mMax else points
            points = if (points < mMin) mMin else points
            updateProgress(points, false)
        }
    var progressWidth: Int
        get() = mProgressWidth
        set(mProgressWidth) {
            this.mProgressWidth = mProgressWidth
            mProgressPaint!!.strokeWidth = mProgressWidth.toFloat()
        }
    var arcWidth: Int
        get() = mArcWidth
        set(mArcWidth) {
            this.mArcWidth = mArcWidth
            mArcPaint!!.strokeWidth = mArcWidth.toFloat()
        }

    override fun isEnabled(): Boolean {
        return mEnabled
    }

    override fun setEnabled(enabled: Boolean) {
        mEnabled = enabled
    }

    var progressColor: Int
        get() = mProgressPaint!!.color
        set(color) {
            mProgressPaint!!.color = color
            invalidate()
        }
    var arcColor: Int
        get() = mArcPaint!!.color
        set(color) {
            mArcPaint!!.color = color
            invalidate()
        }

    fun getMax(): Int {
        return mMax
    }

    fun setMax(mMax: Int) {
        require(mMax > mMin) { "Max should not be less than min." }
        this.mMax = mMax
    }

    fun getMin(): Int {
        return mMin
    }

    fun setMin(min: Int) {
        require(mMax > mMin) { "Min should not be greater than max." }
        mMin = min
    }

    fun setOnSeekBarAttrChangeListener(onSeekBarAttrChangeListener: OnSeekBarAttrChangeListener?) {
        mOnSeekBarAttrChangeListener = onSeekBarAttrChangeListener
    }

    companion object {
        var INVALID_VALUE = -1
        const val MAX = 100
        const val MIN = 0

        /**
         * Offset = -90 indicates that the progress starts from 12 o'clock.
         */
        private const val ANGLE_OFFSET = -90
    }
}