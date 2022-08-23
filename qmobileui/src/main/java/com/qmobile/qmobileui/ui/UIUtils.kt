/*
 * Created by qmarciset on 7/7/2021.
 * 4D SAS
 * Copyright (c) 2021 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.ui

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.qmobile.qmobileui.R

object UIConstants {
    const val longClickDuration = 1500L
    const val clickDebounceTime = 600L
}

fun FragmentActivity?.setupToolbarTitle(title: String) {
    this?.findViewById<CollapsingToolbarLayout>(R.id.collapsing_toolbar)?.let { collapsingToolbarLayout ->
        collapsingToolbarLayout.title = title
    }
}

// Provides if dark mode is enabled
fun Application.isNightMode(): Boolean =
    when (this.resources?.configuration?.uiMode?.and(Configuration.UI_MODE_NIGHT_MASK)) {
        Configuration.UI_MODE_NIGHT_YES -> true
        Configuration.UI_MODE_NIGHT_NO -> false
        Configuration.UI_MODE_NIGHT_UNDEFINED -> false
        else -> false
    }

fun getShakeAnimation(context: Context): Animation = AnimationUtils.loadAnimation(context, R.anim.shake)

class OnSingleClickListener(private val debounceTime: Long, private val block: () -> Unit) : View.OnClickListener {

    private var lastClickTime = 0L

    override fun onClick(view: View) {
        if (SystemClock.elapsedRealtime() - lastClickTime < debounceTime) {
            return
        }
        lastClickTime = SystemClock.elapsedRealtime()
        block()
    }
}

fun View.setOnSingleClickListener(debounceTime: Long = UIConstants.clickDebounceTime, block: () -> Unit) {
    setOnClickListener(OnSingleClickListener(debounceTime, block))
}

fun View.setOnVeryLongClickListener(listener: () -> Unit) {
    setOnTouchListener(object : View.OnTouchListener {

        private val handler = Handler(Looper.getMainLooper())

        override fun onTouch(v: View?, event: MotionEvent?): Boolean {
            when (event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    handler.postDelayed(
                        {
                            performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                            listener.invoke()
                        },
                        UIConstants.longClickDuration
                    )
                }
                MotionEvent.ACTION_UP -> {
                    handler.removeCallbacksAndMessages(null)
                    v?.performClick()
                }
            }
            return true
        }
    })
}

fun View.clearViewInParent() {
    if (this.parent != null) {
        (this.parent as ViewGroup).removeView(this)
    }
}

fun View.disableLink() {
    when (this) { // Button first has a button is also a TextView
        is Button -> this.isEnabled = false
        is TextView -> this.setTextColor(
            ContextCompat.getColor(
                this.context,
                android.R.color.darker_gray
            )
        )
    }
}

fun View.enableLink() {
    when (this) { // Button first has a button is also a TextView
        is Button -> this.isEnabled = true
        is TextView -> this.setTextColor(
            ContextCompat.getColor(
                this.context,
                R.color.relation_link
            )
        )
    }
}
