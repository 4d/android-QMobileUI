/*
 * Created by qmarciset on 10/2/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.action.barcode

import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.CornerPathEffect
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.media.Image
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.common.Barcode
import com.qmobile.qmobileui.R

@Suppress("MagicNumber")
class BarcodeOverlay @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = -1
) : View(context, attrs, defStyleAttr) {
    private var barcodes = listOf<Barcode>()
    private var scale = 1.0f
    private var translX = 0.0f
    private var translY = 0.0f

    private val mRectPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.barcode_reader_frame)
        style = Paint.Style.STROKE
        strokeWidth = 10F
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        pathEffect = CornerPathEffect(50F)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.apply {
            for (barcode in barcodes) {
                barcode.boundingBox?.let { boundingBox ->
                    val rect = translateRect(boundingBox)
                    canvas.drawRect(rect, mRectPaint)
                }
            }
        }
    }

    fun update(scanResult: Image, codes: List<Barcode>) {
        val pw: Float
        val ph: Float
        if (isPortraitMode()) {
            pw = scanResult.height.toFloat()
            ph = scanResult.width.toFloat()
        } else {
            pw = scanResult.width.toFloat()
            ph = scanResult.height.toFloat()
        }
        val vw = width.toFloat()
        val vh = height.toFloat()
        val pictureAspectRatio = pw / ph
        val viewAspectRatio = vw / vh
        if (pictureAspectRatio > viewAspectRatio) {
            scale = vh / ph
            translX = (pw * scale - vw) / 2
            translY = 0f
        } else {
            scale = vw / pw
            translX = 0f
            translY = (ph * scale - vh) / 2
        }
        barcodes = codes
        invalidate()
    }

    private fun isPortraitMode(): Boolean {
        val orientation: Int = resources.configuration.orientation
        return orientation == Configuration.ORIENTATION_PORTRAIT
    }

    private fun translateRect(rect: Rect) = RectF(
        rect.left * scale,
        rect.top * scale,
        rect.right * scale,
        rect.bottom * scale
    ).apply {
        offset(-translX, -translY)
    }
}
