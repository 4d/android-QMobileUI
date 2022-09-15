/*
 * Created by qmarciset on 11/7/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.action.actionparameters

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.binding.ImageHelper.DRAWABLE_24
import com.qmobile.qmobileui.binding.ImageHelper.getDrawableFromString
import com.qmobile.qmobileui.binding.px
import com.qmobile.qmobileui.utils.ResourcesHelper

@Suppress("TooManyFunctions", "unused")
class ActionParametersUtil(private val format: String) {
    // Text
    fun isTextDefault() = format == ActionParameter.TEXT_DEFAULT.format
    fun isEmail() = format == ActionParameter.TEXT_EMAIL.format
    fun isPassword() = format == ActionParameter.TEXT_PASSWORD.format
    fun isUrl() = format == ActionParameter.TEXT_URL.format
    fun isZip() = format == ActionParameter.TEXT_ZIP.format
    fun isPhone() = format == ActionParameter.TEXT_PHONE.format
    fun isAccount() = format == ActionParameter.TEXT_ACCOUNT.format
    fun isTextArea() = format == ActionParameter.TEXT_AREA.format

    // Boolean
    fun isBooleanDefault() = format == ActionParameter.BOOLEAN_DEFAULT.format
    fun isBooleanCheck() = format == ActionParameter.BOOLEAN_CHECK.format

    // Number
    fun isNumberDefault1() = format == ActionParameter.NUMBER_DEFAULT1.format
    fun isNumberDefault2() = format == ActionParameter.NUMBER_DEFAULT2.format
    fun isScientific() = format == ActionParameter.NUMBER_SCIENTIFIC.format
    fun isPercentage() = format == ActionParameter.NUMBER_PERCENTAGE.format
    fun isInteger() = format == ActionParameter.NUMBER_INTEGER.format
    fun isSpellOut() = format == ActionParameter.NUMBER_SPELL_OUT.format

    // Date
    fun isDateDefault1() = format == ActionParameter.DATE_DEFAULT1.format
    fun isDateDefault2() = format == ActionParameter.DATE_DEFAULT2.format
    fun isShortDate() = format == ActionParameter.DATE_SHORT.format
    fun isLongDate() = format == ActionParameter.DATE_LONG.format
    fun isFullDate() = format == ActionParameter.DATE_FULL.format

    // Time
    fun isTimeDefault() = format == ActionParameter.TIME_DEFAULT.format
    fun isDuration() = format == ActionParameter.TIME_DURATION.format

    // Image
    fun isImage() = format == ActionParameter.IMAGE.format
    fun isSignature() = format == ActionParameter.SIGNATURE.format

    // Barcode
    fun isBarcode() = format == ActionParameter.BARCODE.format

    // ----------------------------------------------

    fun getInputControlDrawable(context: Context, icon: String): Drawable? {
        var drawable: Drawable? = null
        ResourcesHelper.correctIconPath(icon)?.let {
            drawable = getDrawableFromString(context, it, DRAWABLE_24.px, DRAWABLE_24.px)
        }

        if (drawable == null) {
            drawable = ContextCompat.getDrawable(context, R.drawable.empty_action)
        }

        return drawable
    }
}
