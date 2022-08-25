/*
 * Created by qmarciset on 23/7/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.list.swipe

import android.content.Context
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.view.MotionEvent
import androidx.appcompat.content.res.AppCompatResources
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.action.model.Action
import com.qmobile.qmobileui.binding.getColorFromAttr
import com.qmobile.qmobileui.utils.ColorHelper

class ItemActionButton(
    val context: Context,
    val action: Action?,
    horizontalIndex: Int,
    private val onClick: () -> Unit
) {

    companion object {
        private const val TRUNCATE_FACTOR = 5
        private const val HORIZONTAL_PADDING = 50.0F
        private const val TEXT_SIZE = 12
        private const val BUTTON_TEXT_LETTER_SPACING = 0.0333333333F
    }

    private val screenWidth: Int = context.resources.displayMetrics.widthPixels
    val intrinsicWidth = (screenWidth / SwipeToActionCallback.BUTTON_WIDTH_FACTOR) // Set button width to screenWidth/4

    private val isDeletePreset = action?.isDeletePreset() == true
    val icon = getIconDrawable()?.apply {
        this.setTint(context.getColorFromAttr(R.attr.colorOnPrimary))
    }
    val iconIntrinsicWidth = icon?.intrinsicWidth?.toFloat() ?: 0f
    val iconIntrinsicHeight = icon?.intrinsicHeight?.toFloat() ?: 0f
    val backgroundColor = ColorHelper.getActionButtonColor(horizontalIndex, context, isDeletePreset)
    val textColor = ColorHelper.getActionButtonTextColor(context, isDeletePreset)

    val textPaint: Paint = Paint().apply {
        textSize = TEXT_SIZE * context.resources.displayMetrics.density
        textAlign = Paint.Align.LEFT
        // trying to mimic Material Design button style
        typeface = Typeface.SANS_SERIF
        letterSpacing = BUTTON_TEXT_LETTER_SPACING
        color = textColor
    }

    val title: String = getTitle(action, textPaint, intrinsicWidth)

    var clickableRegion: RectF? = null

    fun handleEvent(event: MotionEvent) {
        if (clickableRegion?.contains(event.x, event.y) == true) {
            onClick()
        }
    }

    private fun getIconDrawable(): Drawable? {
        var iconResId = 0
        val iconDrawablePath = action?.getIconDrawablePath()
        if (iconDrawablePath != null && iconDrawablePath.isNotEmpty()) {
            iconResId = context.resources.getIdentifier(iconDrawablePath, "drawable", context.packageName)
        }
        val iconDrawable: Drawable?
        when {
            iconResId != 0 -> {
                iconDrawable = AppCompatResources.getDrawable(context, iconResId)
                iconDrawable?.setTint(context.getColorFromAttr(R.attr.colorOnPrimary))
            }
            action == null -> {
                iconDrawable = AppCompatResources.getDrawable(context, R.drawable.more_horiz)
                iconDrawable?.setTint(context.getColorFromAttr(R.attr.colorOnPrimary))
            }
            else -> {
                iconDrawable = AppCompatResources.getDrawable(context, R.drawable.empty_action)
            }
        }
        return iconDrawable
    }

    private fun getTitle(action: Action?, paint: Paint, intrinsicWidth: Float): String {
        if (action == null) return ""
        val title = action.getPreferredShortName()
        return ellipsize(title, paint, intrinsicWidth - HORIZONTAL_PADDING)
    }

    private fun ellipsize(input: String, paint: Paint, maxWidth: Float): String {
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
