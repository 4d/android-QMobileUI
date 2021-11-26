/*
 * Created by Quentin Marciset on 3/3/2021.
 * 4D SAS
 * Copyright (c) 2021 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.ui

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class ItemDecorationSimpleCollection(private val gridSpacingPx: Int, private val gridSize: Int) :
    RecyclerView.ItemDecoration() {

    private var mNeedLeftSpacing = false

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val frameWidth: Int =
            ((parent.width - gridSpacingPx.toFloat() * (gridSize - 1)) / gridSize).toInt()
        val padding: Int = parent.width / gridSize - frameWidth
        val itemPosition: Int = (view.layoutParams as RecyclerView.LayoutParams).bindingAdapterPosition
        if (itemPosition < gridSize) {
            outRect.top = padding
        } else {
            outRect.top = gridSpacingPx
        }
        if (itemPosition % gridSize == 0) {
            outRect.left = padding
            outRect.right = padding
            mNeedLeftSpacing = true
        } else if ((itemPosition + 1) % gridSize == 0) {
            mNeedLeftSpacing = false
            outRect.right = padding
            outRect.left = padding
        } else if (mNeedLeftSpacing) {
            mNeedLeftSpacing = false
            outRect.left = gridSpacingPx - padding
            if ((itemPosition + 2) % gridSize == 0) {
                outRect.right = gridSpacingPx - padding
            } else {
                outRect.right = gridSpacingPx / 2
            }
        } else if ((itemPosition + 2) % gridSize == 0) {
            mNeedLeftSpacing = false
            outRect.left = gridSpacingPx / 2
            outRect.right = gridSpacingPx - padding
        } else {
            mNeedLeftSpacing = false
            outRect.left = gridSpacingPx / 2
            outRect.right = gridSpacingPx / 2
        }
        outRect.bottom = 0
    }
}
