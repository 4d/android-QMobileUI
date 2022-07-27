/*
 * Created by qmarciset on 23/7/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.ui.swipe

import android.content.Context
import android.graphics.Paint
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.binding.getColorFromAttr

class ItemDeleteButton(
    private val context: Context,
    clickListener: SwipeHelper.UnderlayButtonClickListener
) : SwipeItemButton(context, clickListener) {

    override fun getBackgroundColor(): Int {
        return context.getColorFromAttr(R.attr.colorError)
    }

    override fun getIconDrawable(): Drawable? {
        val iconDrawable = ContextCompat.getDrawable(context, R.drawable.delete)
        iconDrawable?.setTint(context.getColorFromAttr(R.attr.colorOnError))
        return iconDrawable
    }

    override fun getTextColor(): Int {
        return context.getColorFromAttr(R.attr.colorOnError)
    }

    override fun getTitle(paint: Paint): String {
        return context.resources.getString(R.string.item_delete_button_title)
    }
}
