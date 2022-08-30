/*
 * Created by Quentin Marciset on 4/3/2021.
 * 4D SAS
 * Copyright (c) 2021 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.binding

import android.graphics.Bitmap
import androidx.annotation.ColorInt
import com.bumptech.glide.load.Transformation
import jp.wasabeef.glide.transformations.BlurTransformation
import jp.wasabeef.glide.transformations.CropCircleWithBorderTransformation
import jp.wasabeef.glide.transformations.CropSquareTransformation
import jp.wasabeef.glide.transformations.GrayscaleTransformation
import jp.wasabeef.glide.transformations.RoundedCornersTransformation

object Transformations {

    private const val CROP_CIRCLE_WITH_BORDER_SIZE = 4
    private const val BLUR_RADIUS = 50
    private const val BLUR_SAMPLING = 3
    private const val ROUNDED_CORNERS_RADIUS = 128
    private const val ROUNDED_CORNERS_MARGIN = 0

    fun getTransformation(transform: String?, @ColorInt borderColor: Int? = null): Transformation<Bitmap>? {
        return when (transform) {
            "CropCircle" -> CropCircleWithBorderTransformation(0, 0)
            "CropCircleWithBorder" -> CropCircleWithBorderTransformation(CROP_CIRCLE_WITH_BORDER_SIZE, borderColor ?: 0)
            "Blur" -> BlurTransformation(BLUR_RADIUS, BLUR_SAMPLING)
            "RoundedCorners" -> RoundedCornersTransformation(
                ROUNDED_CORNERS_RADIUS,
                ROUNDED_CORNERS_MARGIN,
                RoundedCornersTransformation.CornerType.BOTTOM
            )
            "CropSquare" -> CropSquareTransformation()
//        "ColorFilter" -> ColorFilterTransformation()
            "Grayscale" -> GrayscaleTransformation()
            else -> null
        }
    }
}
