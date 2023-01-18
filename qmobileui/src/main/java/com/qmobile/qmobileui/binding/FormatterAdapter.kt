/*
 * Created by qmarciset on 16/7/2021.
 * 4D SAS
 * Copyright (c) 2021 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.binding

import android.view.View
import android.webkit.WebView
import android.widget.TextView
import androidx.databinding.BindingAdapter
import com.google.android.material.chip.Chip
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobiledatasync.utils.FieldMapping
import com.qmobile.qmobileui.formatters.FormatterUtils
import com.qmobile.qmobileui.formatters.ImageNamed
import com.qmobile.qmobileui.webview.WebViewHelper
import timber.log.Timber
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

@BindingAdapter(
    value = ["text", "format", "tableName", "fieldName", "imageWidth", "imageHeight"],
    requireAll = false
)
fun applyFormatter(
    view: View,
    text: Any?,
    format: String?,
    tableName: String?,
    fieldName: String?,
    imageWidth: Int?,
    imageHeight: Int?
) {
    if (text == null || text.toString().isEmpty()) {
        if (view is Chip) {
            view.visibility = View.GONE
        }
        return
    }
    if (view is WebView) {
        WebViewHelper.loadUrl(view, text.toString())
        return
    }
    if (view is Chip) {
        view.visibility = View.VISIBLE
    }
    if (view is TextView) {
        if (!handleAsTextView(view, text, format, tableName, fieldName, imageWidth, imageHeight)) {
            view.text = text.toString()
        }
    }
}

private fun handleAsTextView(
    view: TextView,
    text: Any,
    format: String?,
    tableName: String?,
    fieldName: String?,
    imageWidth: Int?,
    imageHeight: Int?
): Boolean {
    if (!format.isNullOrEmpty()) {
        return when {
            format == "imageURL" -> {
                InlineImageHelper.handle(view, text)
                true
            }
            !format.startsWith("/") -> {
                view.text = FormatterUtils.applyFormat(format, text)
                true
            }
            else -> {
                applyCustomFormat(
                    view,
                    fieldName,
                    tableName,
                    text.toString(),
                    imageWidth,
                    imageHeight
                )
            }
        }
    }
    return false
}

fun getContentType(urlString: String?): String? {
    val url = URL(urlString)
    return try {
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "HEAD"
        if (isRedirect(connection.responseCode)) {
            val newUrl =
                connection.getHeaderField("Location") // get redirect url from "location" header field
            getContentType(newUrl)
        } else {
            connection.contentType
        }
    } catch (e: IOException) {
        Timber.e(e.message)
        null
    }
}

fun isRedirect(statusCode: Int): Boolean {
    return when (statusCode) {
        HttpURLConnection.HTTP_OK -> false
        HttpURLConnection.HTTP_MOVED_TEMP, HttpURLConnection.HTTP_MOVED_PERM, HttpURLConnection.HTTP_SEE_OTHER -> true
        else -> false
    }
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
        BaseApp.runtimeDataHolder.customFormatters[tableName]?.get(fieldName)?.let { fieldMapping ->
            when (fieldMapping.binding) {
                "imageNamed" -> applyImageNamedFormat(view, text, fieldMapping, imageWidth, imageHeight)
                "localizedText" -> applyLocalizedTextFormat(view, text, fieldMapping)
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
    fieldMapping.getStringInChoiceList(text)?.let { drawableName ->

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
    val formattedValue: String? = fieldMapping.getStringInChoiceList(text)
    view.text = if (formattedValue.isNullOrEmpty()) "" else formattedValue
}
