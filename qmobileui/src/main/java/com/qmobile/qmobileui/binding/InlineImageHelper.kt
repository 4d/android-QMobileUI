/*
 * Created by qmarciset on 18/1/2023.
 * 4D SAS
 * Copyright (c) 2023 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.binding

import android.util.Patterns
import android.widget.TextView
import androidx.core.text.HtmlCompat
import androidx.lifecycle.lifecycleScope
import com.qmobile.qmobileui.activity.mainactivity.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.regex.Matcher

object InlineImageHelper {

    private const val offsetLength = "<img src=\"\" />".length

    fun handle(view: TextView, text: Any) {
        (view.context as? MainActivity)?.apply {
            lifecycleScope.launch(Dispatchers.IO) {
                runCatching {
                    val stringBuilder = StringBuilder(text.toString())
                    val matcher: Matcher = Patterns.WEB_URL.matcher(text.toString())
                    var i = 0
                    while (matcher.find()) {
                        val url = matcher.group()
                        val contentType = getContentType(url)
                        val isImage = when {
                            contentType == null -> urlContainsImageFormat(text.toString())
                            contentType.startsWith("image/") -> true
                            else -> false
                        }
                        if (isImage) {
                            val start = matcher.start() + (i * offsetLength)
                            val end = matcher.end() + (i * offsetLength)
                            stringBuilder.replace(start, end, "<img src=\"$url\" />")
                            i++
                        }
                    }
                    stringBuilder.insert(0, "<p>")
                    stringBuilder.append("</p>")

                    withContext(Dispatchers.Main) {
                        val imageGetter = HtmlImageGetter(lifecycleScope, resources, view)
                        val styledText = HtmlCompat.fromHtml(
                            stringBuilder.toString(),
                            HtmlCompat.FROM_HTML_MODE_LEGACY,
                            imageGetter,
                            null
                        )
                        view.text = styledText
                    }
                }
            }
        }
    }

    private fun urlContainsImageFormat(input: String): Boolean {
        listOf("jpg", "jpeg", "png", "webp", "avif", "gif").forEach { format ->
            if (input.contains(".$format")) {
                return true
            }
        }
        return false
    }

    private fun getContentType(urlString: String?): String? {
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
            Timber.e(e.message.orEmpty())
            null
        }
    }

    private fun isRedirect(statusCode: Int): Boolean {
        return when (statusCode) {
            HttpURLConnection.HTTP_OK -> false
            HttpURLConnection.HTTP_MOVED_TEMP,
            HttpURLConnection.HTTP_MOVED_PERM,
            HttpURLConnection.HTTP_SEE_OTHER -> true
            else -> false
        }
    }
}
