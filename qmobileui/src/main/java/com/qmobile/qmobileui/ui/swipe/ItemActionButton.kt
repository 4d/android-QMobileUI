/*
 * Created by qmarciset on 23/7/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.ui.swipe

import android.content.Context
import android.graphics.Paint
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.action.model.Action
import com.qmobile.qmobileui.binding.getColorFromAttr
import com.qmobile.qmobileui.ui.swipe.SwipeItemButton.Companion.HORIZONTAL_PADDING
import com.qmobile.qmobileui.utils.ColorHelper

class ItemActionButton(
    private val context: Context,
    private val action: Action?,
    private var horizontalIndex: Int,
    clickListener: SwipeHelper.UnderlayButtonClickListener
) : SwipeItemButton(context, clickListener) {

    override fun getBackgroundColor(): Int {
        return ColorHelper.getActionButtonColor(horizontalIndex, context)
    }

    override fun getIconDrawable(): Drawable? {
        var iconResId = 0
        val iconDrawablePath = action?.getIconDrawablePath()
        if (iconDrawablePath != null && iconDrawablePath.isNotEmpty()) {
            iconResId = context.resources.getIdentifier(iconDrawablePath, "drawable", context.packageName)
        }
        val iconDrawable: Drawable?
        if (iconResId != 0) {
            iconDrawable = AppCompatResources.getDrawable(
                context,
                iconResId
            )
            iconDrawable?.setTint(context.getColorFromAttr(R.attr.colorOnPrimary))
        } else {
            iconDrawable =
                ColorDrawable(ContextCompat.getColor(context, android.R.color.transparent))
        }
        return iconDrawable
    }

    override fun getTextColor(): Int {
        return context.getColorFromAttr(R.attr.colorOnPrimary)
    }

    override fun getTitle(paint: Paint): String {
        val title =
            action?.getPreferredShortName() ?: context.resources.getString(R.string.item_action_button_ellipsize)
        return ellipsize(title, paint, intrinsicWidth - HORIZONTAL_PADDING)
    }
}
