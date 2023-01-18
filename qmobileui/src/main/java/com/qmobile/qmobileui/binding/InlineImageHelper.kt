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
import java.util.regex.Matcher

object InlineImageHelper {

    private const val offsetLength = "<img src=\"\" />".length

    fun handle(view: TextView, text: Any) {
        (view.context as? MainActivity)?.apply {
            lifecycleScope.launch(Dispatchers.IO) {
                runCatching {
                    val stringBuilder = StringBuilder("<p>$text</p")
                    val matcher: Matcher = Patterns.WEB_URL.matcher(stringBuilder)
                    var i = 0
                    while (matcher.find()) {
                        val url = matcher.group()
                        val contentType = getContentType(url)
                        if (contentType?.startsWith("image/") == true) {
                            val start = matcher.start() + (i * offsetLength)
                            val end = matcher.end() + (i * offsetLength)
                            stringBuilder.replace(start, end, "<img src=\"$url\" />")
                            i++
                        }
                    }

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
}
