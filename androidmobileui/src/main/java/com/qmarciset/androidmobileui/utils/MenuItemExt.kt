/*
 * Created by Quentin Marciset on 13/8/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmarciset.androidmobileui.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.text.TextPaint
import android.view.MenuItem

object MenuItemExt {

    private const val drawnLetterTextSize = 45f
    private const val defaultDpItemIcon = 24
    private const val xStartDrawOrigin = 10f
    private const val yStartDrawOrigin = 40f

    fun MenuItem.setMissingIcon(context: Context) {

        val textPaint = TextPaint()
        textPaint.color = Color.BLACK
        textPaint.textAlign = Paint.Align.LEFT
        textPaint.textSize = drawnLetterTextSize
        textPaint.isAntiAlias = true

        val text = this.title.first().toUpperCase().toString()

        val width = defaultDpItemIcon * context.resources.displayMetrics.density.toInt()
        val height = defaultDpItemIcon * context.resources.displayMetrics.density.toInt()
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        canvas.drawText(text, xStartDrawOrigin, yStartDrawOrigin, textPaint)

        val bitmapDrawable = BitmapDrawable(context.resources, bmp)
        this.icon = bitmapDrawable
    }
}
