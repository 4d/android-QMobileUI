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

@Suppress("ReturnCount", "NestedBlockDepth", "ComplexMethod", "LongMethod")
fun TextView.setFormatterDrawable(
    drawableResPair: Pair<Int, Int?>,
    imageWidth: Int?,
    imageHeight: Int?,
    template: Boolean?
) {
    if (this is Chip) {

        if (template == true) {
            ContextCompat.getDrawable(this.context.applicationContext, drawableResPair.first)
                ?.let { drawable ->
                    this.chipIcon = drawable
                    this.chipIconTint = this.textColors
                    return
                }
        }

        if (BaseApp.nightMode()) {
            drawableResPair.second?.let { darkModeDrawableRes ->
                ContextCompat.getDrawable(this.context.applicationContext, darkModeDrawableRes)
                    ?.let { drawable ->
                        this.chipIcon = drawable
                        return
                    }
            }
        }

        ContextCompat.getDrawable(this.context.applicationContext, drawableResPair.first)
            ?.let { drawable ->
                this.chipIcon = drawable
            }
    } else { // is TextView

        if (imageWidth == null || imageHeight == null || imageWidth == 0 || imageHeight == 0) {

            if (template == true) {
                this.setCompoundDrawablesWithIntrinsicBounds(drawableResPair.first, 0, 0, 0)
                TextViewCompat.setCompoundDrawableTintList(this, this.textColors)
                return
            }

            if (BaseApp.nightMode()) {
                drawableResPair.second?.let { darkModeDrawableRes ->
                    this.setCompoundDrawablesWithIntrinsicBounds(darkModeDrawableRes, 0, 0, 0)
                    return
                }
            }

            this.setCompoundDrawablesWithIntrinsicBounds(drawableResPair.first, 0, 0, 0)
        } else {

            if (template == true) {
                ContextCompat.getDrawable(this.context.applicationContext, drawableResPair.first)
                    ?.let { drawable ->
                        drawable.setTint(this.currentTextColor)
                        drawable.setBounds(0, 0, imageWidth, imageHeight)
                        this.setCompoundDrawables(drawable, null, null, null)
                        return
                    }
            }

            if (BaseApp.nightMode()) {
                drawableResPair.second?.let { darkModeDrawableRes ->
                    ContextCompat.getDrawable(this.context.applicationContext, darkModeDrawableRes)
                        ?.let { drawable ->
                            drawable.setBounds(0, 0, imageWidth, imageHeight)
                            this.setCompoundDrawables(drawable, null, null, null)
                            return
                        }
                }
            }

            ContextCompat.getDrawable(this.context.applicationContext, drawableResPair.first)
                ?.let { drawable ->
                    drawable.setBounds(0, 0, imageWidth, imageHeight)
                    this.setCompoundDrawables(drawable, null, null, null)
                }
        }
    }
}
