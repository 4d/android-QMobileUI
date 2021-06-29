/*
 * Created by Quentin Marciset on 4/6/2021.
 * 4D SAS
 * Copyright (c) 2021 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.binding

import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.widget.TextViewCompat
import com.google.android.material.chip.Chip
import com.qmobile.qmobiledatasync.app.BaseApp

@Suppress("ComplexMethod")
fun TextView.setFormatterDrawable(
    drawableResPair: Pair<Int, Int>,
    imageWidth: Int?,
    imageHeight: Int?,
    tintable: Boolean?
) {
    if (this is Chip) {
        this.setChipDrawable(drawableResPair, tintable)
    } else { // is TextView
        if (imageWidth == null || imageHeight == null || imageWidth == 0 || imageHeight == 0) {
            this.setTextViewDrawableWithoutSize(drawableResPair, tintable)
        } else {
            this.setTextViewDrawableWithSize(drawableResPair, tintable, imageWidth, imageHeight)
        }
    }
}

private fun Chip.setChipDrawable(drawableResPair: Pair<Int, Int>, tintable: Boolean?) {
    if (BaseApp.nightMode() && drawableResPair.second != 0)
        this.chipIcon =
            ContextCompat.getDrawable(this.context.applicationContext, drawableResPair.second)
    else
        this.chipIcon =
            ContextCompat.getDrawable(this.context.applicationContext, drawableResPair.first)

    if (tintable == true)
        this.chipIconTint = this.textColors
}

private fun TextView.setTextViewDrawableWithoutSize(
    drawableResPair: Pair<Int, Int>,
    tintable: Boolean?
) {
    if (BaseApp.nightMode() && drawableResPair.second != 0)
        this.setCompoundDrawablesWithIntrinsicBounds(drawableResPair.second, 0, 0, 0)
    else
        this.setCompoundDrawablesWithIntrinsicBounds(drawableResPair.first, 0, 0, 0)

    if (tintable == true)
        TextViewCompat.setCompoundDrawableTintList(this, this.textColors)
}

private fun TextView.setTextViewDrawableWithSize(
    drawableResPair: Pair<Int, Int>,
    tintable: Boolean?,
    imageWidth: Int,
    imageHeight: Int
) {
    val drawable = if (BaseApp.nightMode() && drawableResPair.second != 0)
        ContextCompat.getDrawable(this.context.applicationContext, drawableResPair.second)
    else
        ContextCompat.getDrawable(this.context.applicationContext, drawableResPair.first)

    drawable?.let {
        if (tintable == true)
            drawable.setTint(this.currentTextColor)
        drawable.setBounds(0, 0, imageWidth, imageHeight)
        this.setCompoundDrawables(drawable, null, null, null)
    }
}
