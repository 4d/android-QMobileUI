/*
 * Created by qmarciset on 16/7/2021.
 * 4D SAS
 * Copyright (c) 2021 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.binding

import android.widget.TextView
import androidx.databinding.BindingAdapter
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobiledatasync.utils.FieldMapping
import com.qmobile.qmobileui.formatters.FormatterUtils
import com.qmobile.qmobileui.formatters.ImageNamed
import com.qmobile.qmobileui.utils.fieldAdjustment
import com.qmobile.qmobileui.utils.tableNameAdjustment

@Suppress("ReturnCount", "LongParameterList")
@BindingAdapter(
    value = ["text", "format", "tableName", "fieldName", "imageWidth", "imageHeight"],
    requireAll = false
)
fun applyFormatter(
    view: TextView,
    text: Any?,
    format: String?,
    tableName: String?,
    fieldName: String?,
    imageWidth: Int?,
    imageHeight: Int?
) {
    if (text == null || text.toString().isEmpty())
        return
    if (!format.isNullOrEmpty()) {
        if (!format.startsWith("/")) {
            view.text = FormatterUtils.applyFormat(format, text)
            return
        } else {
            val fieldMappingFound =
                applyCustomFormat(view, fieldName, tableName, text.toString(), imageWidth, imageHeight)
            if (fieldMappingFound) return
        }
    }
    view.text = text.toString()
    return
}

/**
 * Returns true if a fieldMapping was found, false otherwise
 */
private fun applyCustomFormat(
    view: TextView,
    fieldName: String?,
    tableName: String?,
    text: String,
    imageWidth: Int?,
    imageHeight: Int?
): Boolean {
    if (tableName != null && fieldName != null) {

        BaseApp.runtimeDataHolder.customFormatters[tableName.tableNameAdjustment()]?.get(
            fieldName.fieldAdjustment()
        )
            ?.let { fieldMapping ->

                when (fieldMapping.binding) {
                    "imageNamed" -> {
                        applyImageNamedFormat(view, text, fieldMapping, imageWidth, imageHeight)
                    }
                    "localizedText" -> {
                        applyLocalizedTextFormat(view, text, fieldMapping)
                    }
                    else -> view.text = ""
                }
                return true
            }
    }
    return false
}

private fun applyImageNamedFormat(
    view: TextView,
    text: String,
    fieldMapping: FieldMapping,
    imageWidth: Int?,
    imageHeight: Int?
) {
    fieldMapping.getChoiceListString(text)?.let { drawableName ->

        fieldMapping.name?.let { formatName ->
            BaseApp.genericTableFragmentHelper.getDrawableForFormatter(
                formatName,
                drawableName
            )?.let { drawableResPair ->
                ImageNamed.setFormatterDrawable(
                    view,
                    drawableResPair,
                    imageWidth,
                    imageHeight,
                    fieldMapping.tintable
                )
            }
        }
    } ?: run {
        /* There are inconsistencies with RecyclerView and imageNamed
        custom formatters : if we don't remove any compoundDrawable,
        some RecyclerView item receive a compound drawable while it
        should not receive any */
        view.setCompoundDrawables(null, null, null, null)
    }
}

private fun applyLocalizedTextFormat(view: TextView, text: String, fieldMapping: FieldMapping) {
    val formattedValue: String? =
        fieldMapping.getChoiceListString(text)
    view.text =
        if (formattedValue.isNullOrEmpty()) "" else formattedValue
}
