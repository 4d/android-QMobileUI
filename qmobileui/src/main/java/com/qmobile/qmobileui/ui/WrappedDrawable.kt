/*
 * Created by qmarciset on 7/9/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.ui

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.drawable.Drawable

class WrappedDrawable(private val drawable: Drawable) : Drawable() {

    override fun setBounds(left: Int, top: Int, right: Int, bottom: Int) {
        super.setBounds(left, top, right, bottom)
        drawable.setBounds(left, top, right, bottom)
    }

    override fun setAlpha(alpha: Int) {
        drawable.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        drawable.colorFilter = colorFilter
    }

    @Deprecated("Deprecated in Java")
    @Suppress("deprecation")
    override fun getOpacity(): Int = drawable.opacity

    override fun draw(canvas: Canvas) {
        drawable.draw(canvas)
    }

    override fun getIntrinsicWidth(): Int = drawable.bounds.width()

    override fun getIntrinsicHeight(): Int = drawable.bounds.height()
}
