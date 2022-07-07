/*
 * Created by Quentin Marciset on 11/2/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.binding

import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.databinding.BindingAdapter
import com.qmobile.qmobileui.R

/**
 * Use Glide to load image url in a view
 */
@BindingAdapter(
    value = ["imageUrl", "imageFieldName", "imageKey", "imageTableName", "imageTransform"],
    requireAll = false
)
fun bindImage(
    view: ImageView,
    imageUrl: String?,
    fieldName: String?,
    key: String?,
    tableName: String?,
    transform: String? = null
) {
    val image: Any = ImageHelper.getImage(imageUrl, fieldName, key, tableName)
    val glideRequest = ImageHelper.getGlideRequest(view, image)

    Transformations.getTransformation(
        transform,
        view.context.getColorFromAttr(android.R.attr.colorPrimary)
    )?.let { transformation ->
        glideRequest.transform(transformation)
    }

    glideRequest.into(view)
}

/**
 * Use Glide to load image drawable in a view
 */
@BindingAdapter("imageDrawable")
fun bindImageFromDrawable(view: ImageView, imageDrawable: Int?) {
    if (imageDrawable == null) {
        return
    }

    ImageHelper.getGlideRequest(view, imageDrawable).into(view)
}

@BindingAdapter("visibleGone")
fun showHide(view: View, show: Boolean) {
    view.visibility = if (show) View.VISIBLE else View.GONE
}

@BindingAdapter("linkColor")
fun bindRelationLinkColor(view: TextView, textColor: Boolean?) {
    if (textColor == true) {
        view.setTextColor(ContextCompat.getColor(view.context, R.color.relation_link))
    }
}

@BindingAdapter(
    value = ["buttonText", "entryRelation", "altButtonText"],
    requireAll = false
)
fun buttonText(view: Button, buttonText: String?, entryRelation: Any?, altButtonText: String?) {
    view.text = if (entryRelation == null) altButtonText else buttonText
}
