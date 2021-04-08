/*
 * Created by Quentin Marciset on 8/4/2021.
 * 4D SAS
 * Copyright (c) 2021 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.ui.parallax

import android.graphics.Canvas

abstract class ViewTransformer {

    /**
     * This will be called when it's being set into the ScrollTransformImageView
     */
    open fun onAttached(view: ScrollTransformImageView) {}

    /**
     * This will be called when it's being removed or replaced by other viewTransformer
     * from the ScrollTransformImageView
     */
    open fun onDetached(view: ScrollTransformImageView) {}

    /**
     * apply will be called every time the view scrolled and before rendered
     */
    abstract fun apply(view: ScrollTransformImageView, canvas: Canvas, viewX: Int, viewY: Int)

    /**
     * Convert top,left (0,0) coordinate
     * to the middle of the screen
     */
    protected fun centeredX(x: Int, viewWidth: Int, screenWidth: Int) =
        x + (viewWidth / 2) - (screenWidth / 2)

    protected fun centeredY(y: Int, viewHeight: Int, screenHeight: Int) =
        (screenHeight / 2) - (y + (viewHeight / 2))
}
