package com.example.dotprogressbar

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import com.qmobile.qmobileui.R

const val NUMBER_OF_DOTS = 3
const val ANIMATION_DURATION = 1000L
const val MIN_SCALE = 0.1f
const val MAX_SCALE = 0.7f
const val MARGIN = 0.1f
const val RADIUS = 2f

class DotProgressBar : FrameLayout {
    private var margin: Int = convertDpToPixel(MARGIN, context)
    private var dotRadius: Int = convertDpToPixel(RADIUS, context)
    private val animators = mutableListOf<Animator>()
    private var primaryAnimator: ValueAnimator? = null
    private lateinit var progressBar: LinearLayout
    private var dotBackground = R.drawable.ic_dot
    private var dotAnimator: ValueAnimator? = null

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init()
    }

    private fun init() {
        background = ContextCompat.getDrawable(context, R.drawable.circle_bg)

        clipChildren = false
        clipToPadding = false
        progressBar = LinearLayout(context)
        val progressBarLayoutParams =
            LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        progressBarLayoutParams.gravity = Gravity.CENTER
        progressBar.layoutParams = progressBarLayoutParams
        progressBar.clipChildren = false
        progressBar.clipToPadding = false
        addView(progressBar)
        animators.clear()
        repeat((0 until NUMBER_OF_DOTS).count()) {
            val dot = View(context)
            val layoutParams = LayoutParams(dotRadius * 2, dotRadius * 2)
            layoutParams.setMargins(margin, margin, margin, margin)
            dot.layoutParams = layoutParams
            dot.scaleX = MIN_SCALE
            dot.scaleY = MIN_SCALE
            dot.setBackgroundResource(dotBackground)
            progressBar.addView(dot)
            val animator = getScaleAnimator(dot)
            animators.add(animator)
        }
        primaryAnimator?.cancel()
        primaryAnimator = ValueAnimator.ofInt(0, NUMBER_OF_DOTS)
        primaryAnimator?.addUpdateListener {
            if (it.animatedValue != NUMBER_OF_DOTS)
                animators[it.animatedValue as Int].start()
        }
        primaryAnimator?.repeatMode = ValueAnimator.RESTART
        primaryAnimator?.repeatCount = ValueAnimator.INFINITE
        primaryAnimator?.duration = ANIMATION_DURATION.toLong()
        primaryAnimator?.interpolator = LinearInterpolator()

        startAnimation()
    }

    private fun getScaleAnimator(view: View): Animator {
        if (dotAnimator != null)
            return dotAnimator as ValueAnimator
        val animator = ValueAnimator.ofFloat(MIN_SCALE, MAX_SCALE)
        animator.addUpdateListener {
            view.scaleX = it.animatedValue as Float
            view.scaleY = it.animatedValue as Float
        }
        animator.duration = ANIMATION_DURATION / NUMBER_OF_DOTS.toLong()
        animator.repeatCount = 1
        animator.repeatMode = ValueAnimator.REVERSE
        animator.interpolator = LinearInterpolator()
        return animator
    }

    private fun stopAnimation() {
        primaryAnimator?.cancel()
    }

    private fun startAnimation() {
        primaryAnimator?.start()
    }

    fun isAnimationRunning(): Boolean {
        return primaryAnimator?.isRunning ?: false
    }

    override fun setVisibility(visibility: Int) {
        if (visibility == View.VISIBLE) startAnimation()
        else stopAnimation()
        super.setVisibility(visibility)
    }

/* Could be useful later
    class Builder {
        private var margin = 4
        private var dotRadius = 8
        private var numberOfDots = 3
        private var animationDuration = 1000L
        private var minScale = 0.5f
        private var maxScale = 1f
        private var primaryAnimator: ValueAnimator? = null
        private var dotBackground = if (BaseApp.nightMode()) {
            R.drawable.ic_dot_dark
        } else {
            R.drawable.ic_dot
        }

        fun build(context: Context): DotProgressBar {
            val dotProgressBar = DotProgressBar(context)
            dotProgressBar.maxScale = maxScale
            dotProgressBar.minScale = minScale
            dotProgressBar.numberOfDots = numberOfDots
            dotProgressBar.animationDuration = animationDuration
            dotProgressBar.margin = convertDpToPixel(margin.toFloat(), context)
            dotProgressBar.dotRadius = convertDpToPixel(dotRadius.toFloat(), context)
            dotProgressBar.primaryAnimator = primaryAnimator
            dotProgressBar.dotBackground = dotBackground
            dotProgressBar.init()
            return dotProgressBar
        }

        fun setMargin(margin: Int): Builder {
            this.margin = margin
            return this
        }

        fun setdotRadius(dotRadius: Int): Builder {
            this.dotRadius = dotRadius
            return this
        }

        fun setNumberOfDots(numberOfDots: Int): Builder {
            this.numberOfDots = numberOfDots
            return this
        }

        fun setMaxScale(maxScale: Float): Builder {
            this.maxScale = maxScale
            return this
        }

        fun setMinScale(minScale: Float): Builder {
            this.minScale = minScale
            return this
        }

        fun setAnimationDuration(duration: Long): Builder {
            this.animationDuration = duration
            return this
        }

        fun setDotBackground(dotBackground: Int): Builder {
            this.dotBackground = dotBackground
            return this
        }
    }
*/

    companion object {
        fun convertDpToPixel(dp: Float, context: Context): Int {
            return (
                dp * (
                    context.resources.displayMetrics
                        .densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT
                    )
                ).toInt()
        }
    }
}
