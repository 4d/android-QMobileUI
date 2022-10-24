/*
 * Created by qmarciset on 9/7/2021.
 * 4D SAS
 * Copyright (c) 2021 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.formatters

import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.widget.TextViewCompat
import com.google.android.material.chip.Chip
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobileui.binding.ImageHelper
import com.qmobile.qmobileui.ui.isNightMode

object ImageNamed {

    @Suppress("ComplexCondition")
    fun setFormatterDrawable(
        view: TextView,
        drawableResPair: Pair<Int, Int>,
        imageWidth: Int?,
        imageHeight: Int?,
        tintable: Boolean?
    ) {
        when (view) {
            is Chip -> view.setChipDrawable(drawableResPair, tintable)
            else -> {
                if (imageWidth == null || imageHeight == null || imageWidth == 0 || imageHeight == 0) {
                    view.setDrawableWithoutSize(drawableResPair, tintable)
                } else {
                    view.setDrawableWithSize(drawableResPair, tintable, imageWidth, imageHeight)
                }
            }
        }
    }

    private fun Chip.setChipDrawable(drawableResPair: Pair<Int, Int>, tintable: Boolean?) {
        if (BaseApp.instance.isNightMode() && drawableResPair.second != 0) {
            this.chipIcon =
                ContextCompat.getDrawable(this.context.applicationContext, drawableResPair.second)
        } else {
            this.chipIcon =
                ContextCompat.getDrawable(this.context.applicationContext, drawableResPair.first)
        }

        if (tintable == true) {
            this.chipIconTint = this.textColors
        }
    }

    private fun TextView.setDrawableWithoutSize(drawableResPair: Pair<Int, Int>, tintable: Boolean?) {
        if (BaseApp.instance.isNightMode() && drawableResPair.second != 0) {
            this.setDrawableWithoutSize(drawableResPair.second, tintable)
        } else {
            this.setDrawableWithoutSize(drawableResPair.first, tintable)
        }
    }

    private fun TextView.setDrawableWithoutSize(resId: Int, tintable: Boolean?) {
        this.setCompoundDrawablesWithIntrinsicBounds(resId, 0, 0, 0)

        if (tintable == true) {
            TextViewCompat.setCompoundDrawableTintList(this, this.textColors)
        }
    }

    private fun TextView.setDrawableWithSize(
        drawableResPair: Pair<Int, Int>,
        tintable: Boolean?,
        imageWidth: Int,
        imageHeight: Int
    ) {
        if (BaseApp.instance.isNightMode() && drawableResPair.second != 0) {
            setDrawableWithSize(drawableResPair.second, tintable, imageWidth, imageHeight)
        } else {
            setDrawableWithSize(drawableResPair.first, tintable, imageWidth, imageHeight)
        }
    }

    private fun TextView.setDrawableWithSize(
        resId: Int,
        tintable: Boolean?,
        imageWidth: Int,
        imageHeight: Int
    ) {
        val drawable = ContextCompat.getDrawable(this.context.applicationContext, resId)

        drawable?.let {
            if (tintable == true) {
                drawable.setTint(this.currentTextColor)
            }
            drawable.setBounds(0, 0, imageWidth, imageHeight)
            this.setCompoundDrawables(drawable, null, null, null)
        }
    }

    @Suppress("ComplexCondition")
    fun setDrawable(view: TextView, iconName: String, imageWidth: Int?, imageHeight: Int?) {
        val resId = ImageHelper.getResId(view.context, iconName)
        if (resId != 0) {
            if (imageWidth == null || imageHeight == null || imageWidth == 0 || imageHeight == 0) {
                view.setDrawableWithoutSize(resId, null)
            } else {
                view.setDrawableWithSize(resId, null, imageWidth, imageHeight)
            }
        }
    }
}
