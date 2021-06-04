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
    template: Boolean?
) {
    if (this is Chip) {

        if (BaseApp.nightMode() && drawableResPair.second != 0)
            this.chipIcon = ContextCompat.getDrawable(this.context.applicationContext, drawableResPair.second)
        else
            this.chipIcon = ContextCompat.getDrawable(this.context.applicationContext, drawableResPair.first)

        if (template == true)
            this.chipIconTint = this.textColors
    } else { // is TextView

        if (imageWidth == null || imageHeight == null || imageWidth == 0 || imageHeight == 0) {

            if (BaseApp.nightMode() && drawableResPair.second != 0)
                this.setCompoundDrawablesWithIntrinsicBounds(drawableResPair.second, 0, 0, 0)
            else
                this.setCompoundDrawablesWithIntrinsicBounds(drawableResPair.first, 0, 0, 0)

            if (template == true)
                TextViewCompat.setCompoundDrawableTintList(this, this.textColors)
        } else {

            val drawable = if (BaseApp.nightMode() && drawableResPair.second != 0)
                ContextCompat.getDrawable(this.context.applicationContext, drawableResPair.second)
            else
                ContextCompat.getDrawable(this.context.applicationContext, drawableResPair.first)

            drawable?.let {
                if (template == true)
                    drawable.setTint(this.currentTextColor)
                drawable.setBounds(0, 0, imageWidth, imageHeight)
                this.setCompoundDrawables(drawable, null, null, null)
            }
        }
    }
}
