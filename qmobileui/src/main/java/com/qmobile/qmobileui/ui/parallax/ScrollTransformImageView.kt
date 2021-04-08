/*
 * Created by Quentin Marciset on 8/4/2021.
 * 4D SAS
 * Copyright (c) 2021 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.ui.parallax

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.ViewTreeObserver
import android.widget.ImageView

open class ScrollTransformImageView : ImageView, ViewTreeObserver.OnScrollChangedListener {

    private val viewLocation: IntArray = IntArray(2)

    var viewTransformer: ViewTransformer? = null
        set(value) {
            field?.onDetached(this)
            field = value
            field?.onAttached(this)
        }
    var enableTransformer: Boolean = true

    constructor(ctx: Context) : super(ctx)
    constructor(ctx: Context, attributeSet: AttributeSet) : super(ctx, attributeSet)

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        viewTreeObserver.addOnScrollChangedListener(this)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        viewTreeObserver.removeOnScrollChangedListener(this)
    }

    override fun onDraw(canvas: Canvas) {
        if (enableTransformer) {
            getLocationInWindow(viewLocation)
            viewTransformer?.apply(this, canvas, viewLocation[0], viewLocation[1])
        }
        super.onDraw(canvas)
    }

    override fun onScrollChanged() {
        if (enableTransformer) invalidate()
    }
}
