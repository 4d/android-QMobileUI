/*
 * Created by Quentin Marciset on 4/6/2021.
 * 4D SAS
 * Copyright (c) 2021 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.binding

import android.content.Context
import android.content.res.Resources
import android.net.Uri
import android.util.TypedValue
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobileui.R
import timber.log.Timber
import java.io.File

object ImageHelper {

    const val drawableStartWidth = 24
    const val drawableStartHeight = 24
    const val drawableSpace = 8

    /**
     * Sample avatar list
     */
    private val listOfAvatars = listOf(
        R.drawable.avatar_1_raster,
        R.drawable.avatar_2_raster,
        R.drawable.avatar_3_raster,
        R.drawable.avatar_4_raster,
        R.drawable.avatar_5_raster,
        R.drawable.avatar_6_raster
    )

    /**
     * Provides one random avatar from the sample avatar list
     */
    private fun randomAvatar(): Int = listOfAvatars.random()

    fun tryImageFromAssets(tableName: String?, key: String?, fieldName: String?): Uri? {
        BaseApp.embeddedFiles.find { it.contains(tableName + File.separator + "$tableName($key)_${fieldName}_") }
            ?.let { path ->
                Timber.d("file = $path")
                return Uri.parse("file:///android_asset/$path")
            }
        return null
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

val Int.dp: Int
    get() = (this / Resources.getSystem().displayMetrics.density).toInt()
val Int.px: Int
    get() = (this * Resources.getSystem().displayMetrics.density).toInt()
