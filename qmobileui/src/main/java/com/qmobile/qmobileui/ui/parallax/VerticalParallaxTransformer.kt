/*
 * Created by Quentin Marciset on 8/4/2021.
 * 4D SAS
 * Copyright (c) 2021 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.ui.parallax

import android.graphics.Canvas
import android.widget.ImageView

class VerticalParallaxTransformer : ViewTransformer() {

    private var initialX = Int.MAX_VALUE
    private var initialY = Int.MAX_VALUE

    companion object {
        private const val DEFAULT_SCALE = 0.9
    }

    override fun onAttached(view: ScrollTransformImageView) {
        view.scaleType = ImageView.ScaleType.CENTER_CROP
    }

    override fun apply(view: ScrollTransformImageView, canvas: Canvas, viewX: Int, viewY: Int) {
        if (initialX == Int.MAX_VALUE) initialX = viewX
        if (initialY == Int.MAX_VALUE) initialY = viewY
        if (view.scaleType == ImageView.ScaleType.CENTER_CROP && view.drawable != null) {
//            val imageWidth = view.drawable.intrinsicWidth
            val imageHeight = view.drawable.intrinsicHeight

//            val viewWidth = view.width - view.paddingLeft - view.paddingRight
            val viewHeight = view.height - view.paddingTop - view.paddingBottom

            val deviceHeight = view.resources.displayMetrics.heightPixels

            // If this view is off screen we wont do anything
            if (viewY < -viewHeight || viewY > deviceHeight) return

            val invisibleVerticalArea = imageHeight * DEFAULT_SCALE.toFloat()

            val translationScale = invisibleVerticalArea / (deviceHeight + viewHeight)
            val centeredY = centeredY(viewY, viewHeight, deviceHeight) - centeredY(initialY, viewHeight, deviceHeight)
            val dy = centeredY * translationScale
            canvas.translate(0f, dy)
        }
    }
}
