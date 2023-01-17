/*
 * Created by qmarciset on 16/1/2023.
 * 4D SAS
 * Copyright (c) 2023 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.binding

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.text.Html
import android.widget.TextView
import androidx.lifecycle.LifecycleCoroutineScope
import com.bumptech.glide.Glide
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

class HtmlImageGetter(
    private val scope: LifecycleCoroutineScope,
    private val res: Resources,
    private val htmlTextView: TextView
) : Html.ImageGetter {

    override fun getDrawable(url: String): Drawable {
        val holder = BitmapDrawablePlaceHolder(res, null)

        scope.launch(Dispatchers.IO) {
            runCatching {
                val bitmap = Glide.with(htmlTextView.context)
                    .asBitmap()
                    .load(url)
                    .submit()
                    .get()

                val drawable = BitmapDrawable(res, bitmap)

                val scale = 1.0 // This makes the image scale in size.
                val width = (drawable.intrinsicWidth * scale).roundToInt()
                val height = (drawable.intrinsicHeight * scale).roundToInt()
                drawable.setBounds(0, 0, width, height)

                holder.setDrawable(drawable)
                holder.setBounds(0, 0, width, height)

                withContext(Dispatchers.Main) {
                    htmlTextView.text = htmlTextView.text
                }
            }
        }

        return holder
    }

    internal class BitmapDrawablePlaceHolder(res: Resources, bitmap: Bitmap?) : BitmapDrawable(res, bitmap) {
        private var drawable: Drawable? = null

        override fun draw(canvas: Canvas) {
            drawable?.run { draw(canvas) }
        }

        fun setDrawable(drawable: Drawable) {
            this.drawable = drawable
        }
    }
}
