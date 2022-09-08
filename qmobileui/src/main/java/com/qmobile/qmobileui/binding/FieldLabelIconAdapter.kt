/*
 * Created by qmarciset on 16/7/2021.
 * 4D SAS
 * Copyright (c) 2021 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.binding

import android.graphics.drawable.Drawable
import android.view.Gravity
import android.view.ViewGroup
import android.widget.TextView
import androidx.databinding.BindingAdapter
import com.qmobile.qmobileui.binding.ImageHelper.DRAWABLE_24
import com.qmobile.qmobileui.binding.ImageHelper.DRAWABLE_SPACE
import com.qmobile.qmobileui.binding.ImageHelper.getDrawableFromString

@BindingAdapter("icon")
fun bindFieldLabelIcon(view: TextView, icon: String?) {
    if (icon.isNullOrEmpty()) {
        return
    }

    val drawable: Drawable? = getDrawableFromString(view.context, icon, DRAWABLE_24.px, DRAWABLE_24.px)

    if (drawable != null) {
        drawable.setTint(view.currentTextColor)
        view.gravity = Gravity.CENTER_VERTICAL
        view.compoundDrawablePadding = DRAWABLE_SPACE.px
        (view.layoutParams as? ViewGroup.MarginLayoutParams)?.bottomMargin = DRAWABLE_SPACE.px
        view.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null)
    }
}
