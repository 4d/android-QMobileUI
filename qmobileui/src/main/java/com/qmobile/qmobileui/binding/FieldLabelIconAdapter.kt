/*
 * Created by qmarciset on 16/7/2021.
 * 4D SAS
 * Copyright (c) 2021 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.binding

import android.view.Gravity
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.databinding.BindingAdapter

@BindingAdapter("icon")
fun bindFieldLabelIcon(view: TextView, icon: String?) {
    if (icon.isNullOrEmpty())
        return

    val resId = view.resources.getIdentifier(icon, "drawable", view.context.packageName)
    if (resId > 0) {
        ContextCompat.getDrawable(view.context.applicationContext, resId)?.let { drawable ->
            drawable.setTint(view.currentTextColor)
            drawable.setBounds(0, 0, ImageHelper.drawableStartWidth.px, ImageHelper.drawableStartHeight.px)
            view.gravity = Gravity.CENTER_VERTICAL
            view.compoundDrawablePadding = ImageHelper.drawableSpace.px
            (view.layoutParams as? ViewGroup.MarginLayoutParams)?.bottomMargin = ImageHelper.drawableSpace.px
            view.setCompoundDrawables(drawable, null, null, null)
        }
    }
}
