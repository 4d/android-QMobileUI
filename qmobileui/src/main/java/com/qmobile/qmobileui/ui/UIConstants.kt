/*
 * Created by qmarciset on 7/7/2021.
 * 4D SAS
 * Copyright (c) 2021 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.ui

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.webkit.WebView
import androidx.core.view.children
import com.qmobile.qmobileui.R

object UIConstants {
    const val longClickDuration = 1500L
    const val clickDebounceTime = 600L
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
            if (event?.action == MotionEvent.ACTION_DOWN) {
                handler.postDelayed(
                    {
                        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        listener.invoke()
                    },
                    UIConstants.longClickDuration
                )
            } else if (event?.action == MotionEvent.ACTION_UP) {
                handler.removeCallbacksAndMessages(null)
                v?.performClick()
            }
            return true
        }
    })
}

fun View.clearViewInParent() {
    if (this.parent != null)
        (this.parent as ViewGroup).removeView(this)
}

@Suppress("ReturnCount")
fun View.checkIfChildIsWebView(): WebView? {
    if (this is WebView) return this
    if (this as? ViewGroup != null) {
        (this as? ViewGroup)?.children?.forEach { child ->
            if (child as? ViewGroup != null) {
                return child.checkIfChildIsWebView()
            }
            if (child is WebView) return child
        }
    }
    return null
}
