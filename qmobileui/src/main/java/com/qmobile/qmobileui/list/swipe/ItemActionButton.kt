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
import androidx.core.content.ContextCompat
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.action.model.Action
import com.qmobile.qmobileui.action.utils.ActionUIHelper
import com.qmobile.qmobileui.binding.ImageHelper.DRAWABLE_32
import com.qmobile.qmobileui.binding.ImageHelper.getDrawableFromString
import com.qmobile.qmobileui.binding.getColorFromAttr
import com.qmobile.qmobileui.binding.px

class ItemActionButton(
    val context: Context,
    val action: Action?,
    horizontalIndex: Int,
    isEnabled: Boolean,
    private val onClick: () -> Unit
) {

    companion object {
        const val BUTTON_WIDTH_FACTOR = 4.5f
        private const val TRUNCATE_FACTOR = 5
        private const val HORIZONTAL_PADDING = 50.0F
        private const val TEXT_SIZE = 14 // Material Design md.sys.typescale.label-large.size
        private const val TITLE_FONT = "Roboto-Medium.ttf" // md.sys.typescale.label-large.font
        private const val DISABLED_ACTION_ALPHA = 100
    }

    private val screenWidth: Int = context.resources.displayMetrics.widthPixels
    val intrinsicWidth = (screenWidth / BUTTON_WIDTH_FACTOR)

    val icon = getIconDrawable()?.apply {
        this.setTint(context.getColorFromAttr(R.attr.colorOnPrimary))
        if (!isEnabled) {
            this.alpha = DISABLED_ACTION_ALPHA
        }
    }
    val iconIntrinsicWidth = icon?.intrinsicWidth?.toFloat() ?: 0f
    val iconIntrinsicHeight = icon?.intrinsicHeight?.toFloat() ?: 0f
    val backgroundColor = ActionUIHelper.getActionButtonColor(context, horizontalIndex)
    val textColor = context.getColorFromAttr(R.attr.colorOnPrimary)

    val textPaint: Paint = Paint().apply {
        textSize = TEXT_SIZE.px.toFloat()
        textAlign = Paint.Align.LEFT
        // trying to mimic Material Design button style
        typeface = Typeface.createFromAsset(context.assets, TITLE_FONT)
        color = textColor
        if (!isEnabled) {
            alpha = DISABLED_ACTION_ALPHA
        }
    }

    val title: String = getTitle(action, textPaint, intrinsicWidth)

    var clickableRegion: RectF? = null

    fun handleEvent(event: MotionEvent) {
        if (clickableRegion?.contains(event.x, event.y) == true) {
            onClick()
        }
    }

    private fun getIconDrawable(): Drawable? {
        val iconDrawablePath = action?.getIconDrawablePath()
        val iconDrawable: Drawable?
        when {
            !iconDrawablePath.isNullOrEmpty() -> {
                iconDrawable = getDrawableFromString(context, iconDrawablePath, DRAWABLE_32.px, DRAWABLE_32.px)
                iconDrawable?.setTint(context.getColorFromAttr(R.attr.colorOnPrimary))
            }
            action == null -> {
                iconDrawable = ContextCompat.getDrawable(context, R.drawable.more_horiz)
                iconDrawable?.setTint(context.getColorFromAttr(R.attr.colorOnPrimary))
            }
            else -> {
                iconDrawable = null
            }
        }
        return iconDrawable
    }

    private fun getTitle(action: Action?, paint: Paint, intrinsicWidth: Float): String {
        return if (action == null) {
            context.resources.getString(R.string.action_more_button_title)
        } else {
            val title = action.getPreferredShortName()
            ellipsize(title, paint, intrinsicWidth - HORIZONTAL_PADDING)
        }
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
