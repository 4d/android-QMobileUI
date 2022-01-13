/*
 * Created by Quentin Marciset on 4/6/2021.
 * 4D SAS
 * Copyright (c) 2021 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.binding

import android.content.Context
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.graphics.drawable.InsetDrawable
import android.net.Uri
import android.os.Build
import android.util.TypedValue
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.core.graphics.ColorUtils
import com.qmobile.qmobiledatasync.app.BaseApp
import timber.log.Timber
import java.io.File

object ImageHelper {

    const val drawableStartWidth = 24
    const val drawableStartHeight = 24
    const val drawableSpace = 8
    const val luminanceThreshold = 0.5
    const val ICON_MARGIN = 8

    fun tryImageFromAssets(tableName: String?, key: String?, fieldName: String?): Uri? {
        BaseApp.runtimeDataHolder.embeddedFiles.find {
            it.contains(tableName + File.separator + "$tableName($key)_${fieldName}_")
        }?.let { path ->
            Timber.d("file = $path")
            return Uri.parse("file:///android_asset/$path")
        }
        return null
    }

    fun Drawable?.adjustActionDrawableMargins(context: Context): Drawable {
        val iconMarginPx =
            TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                ICON_MARGIN.toFloat(),
                context.resources.displayMetrics
            )
                .toInt()

        return if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            InsetDrawable(this, iconMarginPx, 0, iconMarginPx, 0)
        } else {
            object : InsetDrawable(this, iconMarginPx, 0, iconMarginPx, 0) {
                override fun getIntrinsicWidth(): Int {
                    return intrinsicHeight + iconMarginPx + iconMarginPx
                }
            }
        }
    }
}

@ColorInt
fun Context.getColorFromAttr(
    @AttrRes attrColor: Int,
    typedValue: TypedValue = TypedValue(),
    resolveRefs: Boolean = true
): Int {
    theme.resolveAttribute(attrColor, typedValue, resolveRefs)
    return typedValue.data
}

fun isDarkColor(@ColorInt color: Int): Boolean =
    ColorUtils.calculateLuminance(color) < ImageHelper.luminanceThreshold

val Int.dp: Int
    get() = (this / Resources.getSystem().displayMetrics.density).toInt()
val Int.px: Int
    get() = (this * Resources.getSystem().displayMetrics.density).toInt()
