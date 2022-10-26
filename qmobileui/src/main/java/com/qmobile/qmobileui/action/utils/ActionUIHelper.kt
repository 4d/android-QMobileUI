/*
 * Created by qmarciset on 25/10/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.action.utils

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.core.widget.TextViewCompat
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.action.model.Action
import com.qmobile.qmobileui.binding.ImageHelper
import com.qmobile.qmobileui.binding.ImageHelper.adjustActionDrawableMargins
import com.qmobile.qmobileui.binding.getColorFromAttr
import com.qmobile.qmobileui.binding.px

object ActionUIHelper {

    fun getActionIconDrawable(context: Context, action: Action): Drawable? {
        var drawable: Drawable? = ImageHelper.getDrawableFromString(
            context,
            action.icon,
            ImageHelper.DRAWABLE_32.px,
            ImageHelper.DRAWABLE_32.px
        )

        if (drawable == null) {
            drawable = ContextCompat.getDrawable(context, R.drawable.empty_action)
        }

        return drawable?.adjustActionDrawableMargins()
    }

    fun Drawable.paramMenuActionDrawable(context: Context) {
        this.setMenuItemColorFilter(context)
        this.setBounds(0, 0, ImageHelper.DRAWABLE_24.px, ImageHelper.DRAWABLE_24.px)
    }

    private fun Drawable.setMenuItemColorFilter(context: Context) {
        this.colorFilter = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
            context.getColorFromAttr(R.attr.colorOnSurface),
            BlendModeCompat.SRC_ATOP
        )
    }

    fun getActionButtonColor(context: Context, index: Int): Int {
        return when (index) {
            0 -> context.getColorFromAttr(R.attr.colorPrimary)
            1 -> context.getColorFromAttr(R.attr.colorSecondary)
            else -> context.getColorFromAttr(R.attr.colorTertiary)
        }
    }

    fun getActionArrayAdapter(context: Context, actionList: List<Action>): ArrayAdapter<Action> {
        val withIcons = actionList.firstOrNull { it.getIconDrawablePath() != null } != null

        return object :
            ArrayAdapter<Action>(context, R.layout.material_select_dialog_item, android.R.id.text1, actionList) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val itemView = super.getView(position, convertView, parent)
                val textView = itemView.findViewById<View>(android.R.id.text1) as TextView
                val action = actionList[position]

                if (withIcons) {
                    val drawable = getActionIconDrawable(context, action)
                    textView.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null)
                    TextViewCompat.setCompoundDrawableTintList(textView, textView.textColors)
                    // Add margin between image and text (support various screen densities)
                    val padding = ImageHelper.ICON_MARGIN.px
                    textView.compoundDrawablePadding = padding
                    textView.setPadding(padding, 0, 0, 0)
                } else {
                    textView.setPadding(ImageHelper.NO_ICON_PADDING.px, 0, 0, 0)
                }

                textView.text = action.getPreferredName()
                return itemView
            }
        }
    }
}
