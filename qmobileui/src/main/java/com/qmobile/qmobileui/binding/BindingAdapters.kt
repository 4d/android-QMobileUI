/*
 * Created by Quentin Marciset on 11/2/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.binding

import android.net.Uri
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.databinding.BindingAdapter
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.transition.DrawableCrossFadeFactory
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.binding.ImageHelper.drawableSpace
import com.qmobile.qmobileui.binding.ImageHelper.drawableStartHeight
import com.qmobile.qmobileui.binding.ImageHelper.drawableStartWidth
import com.qmobile.qmobileui.glide.CustomRequestListener
import com.qmobile.qmobileui.utils.FormatterUtils.applyFormat
import com.qmobile.qmobileui.utils.QMobileUiUtil
import com.qmobile.qmobileui.utils.fieldAdjustment
import com.qmobile.qmobileui.utils.getChoiceListString
import com.qmobile.qmobileui.utils.tableNameAdjustment
import kotlin.math.roundToInt

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

    val imageFromAssetUri: Uri? = ImageHelper.tryImageFromAssets(tableName, key, fieldName)

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

@Suppress("ReturnCount", "LongParameterList")
@BindingAdapter(
    value = ["text", "format", "tableName", "fieldName", "imageWidth", "imageHeight"],
    requireAll = false
)
fun applyFormatter(
    view: TextView,
    text: String?,
    format: String?,
    tableName: String?,
    fieldName: String?,
    imageWidth: Int?,
    imageHeight: Int?
) {
    if (text.isNullOrEmpty())
        return
    if (!format.isNullOrEmpty()) {
        if (!format.startsWith("/")) {
            view.text = applyFormat(format, text)
            return
        } else {
            if (tableName != null && fieldName != null) {

                QMobileUiUtil.appUtilities.customFormatters[tableName.tableNameAdjustment()]?.get(
                    fieldName.fieldAdjustment()
                )
                    ?.let { fieldMapping ->

                        when (fieldMapping.binding) {
                            "imageNamed" -> {
                                getChoiceListString(fieldMapping, text)?.let { drawableName ->

                                    fieldMapping.name?.let { formatName ->
                                        BaseApp.genericTableFragmentHelper.getDrawableForFormatter(
                                            formatName,
                                            drawableName
                                        )?.let { drawableResPair ->
                                            view.setFormatterDrawable(
                                                drawableResPair,
                                                imageWidth,
                                                imageHeight,
                                                fieldMapping.tintable
                                            )
                                        }
                                    }
                                }
                            }
                            "localizedText" -> {
                                val formattedValue: String? =
                                    getChoiceListString(fieldMapping, text)
                                view.text =
                                    if (formattedValue.isNullOrEmpty()) "" else formattedValue
                            }
                            else -> view.text = ""
                        }
                        return
                    }
            }
        }
    }
    view.text = text
    return
}

@BindingAdapter("progress")
fun bindCircularProgressIndicator(view: CircularProgressIndicator, progress: Any?) {
    view.progress = when (progress) {
        is Int -> progress
        is Float -> progress.roundToInt()
        else -> 0
    }
}

@BindingAdapter("icon")
fun bindFieldLabelIcon(view: TextView, icon: String?) {
    if (view.text.isNullOrEmpty())
        return
    if (icon.isNullOrEmpty())
        return

    val resId = view.resources.getIdentifier(icon, "drawable", view.context.packageName)
    if (resId > 0) {
        ContextCompat.getDrawable(view.context.applicationContext, resId)?.let { drawable ->
            drawable.setTint(view.currentTextColor)
            drawable.setBounds(0, 0, drawableStartWidth.px, drawableStartHeight.px)
            view.gravity = Gravity.CENTER_VERTICAL
            view.compoundDrawablePadding = drawableSpace.px
            (view.layoutParams as? ViewGroup.MarginLayoutParams)?.bottomMargin = drawableSpace.px
            view.setCompoundDrawables(drawable, null, null, null)
        }
    }
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
