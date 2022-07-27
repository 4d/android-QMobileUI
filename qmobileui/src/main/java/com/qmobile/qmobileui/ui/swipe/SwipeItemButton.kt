/*
 * Created by qmarciset on 23/7/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.ui.swipe

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.view.MotionEvent
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.binding.getColorFromAttr

@Suppress("MagicNumber")
abstract class SwipeItemButton(
    private val context: Context,
    private val clickListener: SwipeHelper.UnderlayButtonClickListener
) {

    companion object {
        const val ACTION_BUTTON_RADIUS = 20F
        const val TRUNCATE_FACTOR = 5

        // Use as margin bottom from the center for icon and as margin top from the center for title
        const val VERTICAL_MARGIN = 70F
        const val HORIZONTAL_PADDING = 50.0F

        const val ICON_SIZE_FACTOR = 0.3F
        const val TEXT_SIZE = 12
    }

    private val screenWidth: Int = context.resources.displayMetrics.widthPixels
    val intrinsicWidth = (screenWidth / 4).toFloat() // Fix button width to screenWidth/4
    var clickableRegion: RectF? = null

    fun draw(canvas: Canvas, rect: RectF) {
        // Draw background
        val paint = Paint()
        val paintStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = context.getColorFromAttr(R.attr.colorSurface)
            style = Paint.Style.STROKE
            strokeWidth = 10F
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
        }
        paint.color = getBackgroundColor()
        canvas.drawRoundRect(rect, ACTION_BUTTON_RADIUS, ACTION_BUTTON_RADIUS, paint)
        canvas.drawRoundRect(rect, ACTION_BUTTON_RADIUS, ACTION_BUTTON_RADIUS, paintStroke)

        // Draw icon
        val iconWith = rect.width() * ICON_SIZE_FACTOR
        val iconHeight = iconWith
        val iconLeft = (rect.left + rect.width() / 2 - iconWith.div(2))
        val iconBottom = rect.bottom - rect.height() / 2
        val iconTop = iconBottom - iconHeight
        val iconRight = iconLeft + iconWith
        val iconDrawable = getIconDrawable()
        iconDrawable?.setBounds(
            iconLeft.toInt(),
            iconTop.toInt(),
            iconRight.toInt(),
            iconBottom.toInt()
        )
        iconDrawable?.draw(canvas)

        // Draw title
        paint.color = getTextColor()
        paint.textSize = TEXT_SIZE * context.resources.displayMetrics.density
//            val growthRatio = sqrt((rect.width() * rect.height()).toDouble()) / 250
//            paint.textSize = (12.px * growthRatio).toFloat()

        // trying to mimic Material Design button style
        paint.typeface = Typeface.SANS_SERIF
        paint.letterSpacing = 0.0333333333F

        val title = getTitle(paint)
        val titleBounds = Rect()
        paint.getTextBounds(title, 0, title.length, titleBounds)
        val x = rect.width() / 2 + titleBounds.width() / 2 - titleBounds.right
        canvas.drawText(title, rect.left + x, iconBottom + VERTICAL_MARGIN, paint)
        clickableRegion = rect
    }

    abstract fun getBackgroundColor(): Int

    abstract fun getIconDrawable(): Drawable?

    abstract fun getTextColor(): Int

    abstract fun getTitle(paint: Paint): String

    fun handle(event: MotionEvent) {
        if (clickableRegion?.contains(event.x, event.y) == true) {
            clickListener.onClick()
        }
    }

    fun ellipsize(input: String, paint: Paint, maxWidth: Float): String {
        val titleBounds = Rect()
        paint.getTextBounds(input, 0, input.length, titleBounds)
        return if (titleBounds.width() < maxWidth || input.length < TRUNCATE_FACTOR) {
            input
        } else {
            val newInput = input.substring(
                0,
                input.length - TRUNCATE_FACTOR
            ) + context.resources.getString(R.string.item_action_button_ellipsize)
            ellipsize(newInput, paint, maxWidth)
        }
    }
}
