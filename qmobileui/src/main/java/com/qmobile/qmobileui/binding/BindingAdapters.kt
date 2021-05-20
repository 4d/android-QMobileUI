/*
 * Created by Quentin Marciset on 11/2/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.binding

import android.content.Context
import android.net.Uri
import android.util.TypedValue
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.appcompat.content.res.AppCompatResources
import androidx.databinding.BindingAdapter
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.transition.DrawableCrossFadeFactory
import com.qmobile.qmobileapi.utils.getSafeObject
import com.qmobile.qmobileapi.utils.getSafeString
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.glide.CustomRequestListener
import com.qmobile.qmobileui.utils.QMobileUiUtil
import com.qmobile.qmobileui.utils.applyFormat
import timber.log.Timber
import java.io.File

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

/**
 * Use Glide to load image url in a view
 */
@Suppress("LongParameterList")
@BindingAdapter(
    value = ["imageUrl", "imageFieldName", "imageKey", "imageTableName", "imageTransform"],
    requireAll = false
)
fun bindImageFromUrl(
    view: ImageView,
    imageUrl: String?,
    fieldName: String?,
    key: String?,
    tableName: String?,
    transform: String? = null
) {
    val factory =
        DrawableCrossFadeFactory.Builder().setCrossFadeEnabled(true).build()

    val imageFromAssetUri: Uri? = tryImageFromAssets(tableName, key, fieldName)

    val glideRequest = Glide.with(view.context.applicationContext)
        .load(
            imageFromAssetUri
                ?: if (!imageUrl.isNullOrEmpty()) imageUrl else R.drawable.ic_placeholder
        )
        .transition(DrawableTransitionOptions.withCrossFade(factory))
        .diskCacheStrategy(DiskCacheStrategy.ALL)
//        .listener(listener)
        .listener(CustomRequestListener())
        .error(R.drawable.ic_placeholder)
//        .placeholder(R.drawable.ic_placeholder)

    Transformations.getTransformation(
        transform,
        view.context.getColorFromAttr(android.R.attr.colorPrimary)
    )?.let { transformation ->
        glideRequest.transform(transformation)
    }

    glideRequest.into(view)
}

fun tryImageFromAssets(tableName: String?, key: String?, fieldName: String?): Uri? {
    BaseApp.embeddedFiles.find { it.contains(tableName + File.separator + "$tableName($key)_${fieldName}_") }
        ?.let { path ->
            Timber.d("file = $path")
            return try {
                Uri.parse("file:///android_asset/$path")
            } catch (e: NullPointerException) {
                null
            }
        }
    return null
}

@BindingAdapter(value = ["text", "format", "tableName", "fieldName"], requireAll = false)
fun applyFormatter(view: TextView, text: String?, format: String?, tableName: String?, fieldName: String?) {
    if (text.isNullOrEmpty())
        return
    if (!format.isNullOrEmpty()) {
        if (!format.startsWith("/")) {
            view.text = applyFormat(format, text)
            return
        } else {
            if (tableName != null && fieldName != null) {
                QMobileUiUtil.appUtilities.customFormatterJson.getSafeObject(tableName)?.getSafeObject(fieldName)?.let { fieldFormatter ->
                    val formatChoiceMap = fieldFormatter.getSafeObject("formatchoice")?.getSafeObject("map")

                    if (fieldFormatter.getSafeString("binding") == "imageNamed") {
                        formatChoiceMap?.getSafeString(text)?.let { drawableName ->

                            fieldFormatter.getSafeString("formatType")?.let { formatName ->
                                BaseApp.fragmentUtil.getDrawableForFormatter(formatName, drawableName)?.let { drawable ->
                                    view.setCompoundDrawablesWithIntrinsicBounds(drawable, 0, 0, 0)
                                    // todo : use Glide
                                    return
                                }
                            }
                        }
                    } else {
                        val mapResult = formatChoiceMap?.getSafeString(text)
                        view.text = if (mapResult.isNullOrBlank()) text else mapResult
                        return
                    }
                }
            }
        }
    }
    view.text = text
    return
}

/**
 * Use Glide to load image drawable in a view
 */
@BindingAdapter("imageDrawable")
fun bindImageFromDrawable(view: ImageView, imageDrawable: Int?) {
    if (imageDrawable == null)
        return

    val factory =
        DrawableCrossFadeFactory.Builder().setCrossFadeEnabled(true).build()

    Glide.with(view.context.applicationContext)
        .load(imageDrawable)
        .transition(DrawableTransitionOptions.withCrossFade(factory))
        .diskCacheStrategy(DiskCacheStrategy.ALL)
        .error(R.drawable.ic_error_outline)
        .into(view)
}

@BindingAdapter("visibleGone")
fun showHide(view: View, show: Boolean) {
    view.visibility = if (show) View.VISIBLE else View.GONE
}

@BindingAdapter("concatStringRatio_1", "concatStringRatio_2")
fun concatStringRatio(view: TextView, str1: String? = "0", str2: String? = "0") {
    view.text = "$str1/$str2"
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
