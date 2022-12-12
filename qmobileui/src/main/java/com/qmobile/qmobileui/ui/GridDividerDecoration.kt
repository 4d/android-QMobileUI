/*
 * Created by qmarciset on 26/7/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.ui

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.view.View
import androidx.annotation.ColorInt
import androidx.annotation.Px
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration

/**
 * An [ItemDecoration] that adds Material-style dividers between grid items. This is meant to
 * be used with [GridLayoutManager] and only supports vertical orientation.
 *
 *
 * This decoration will draw both horizontal and vertical lines along the edges of each view. It
 * will only draw dividers that are internal to the grid, meaning it will not draw lines for the
 * outermost left, top, right, or bottom edges.
 */
class GridDividerDecoration(@Px dividerSize: Int, @ColorInt dividerColor: Int, spanCount: Int) : ItemDecoration() {

    private val spanCount: Int
    private val dividerPaint: Paint = Paint()
    private val bounds = Rect()

    init {
        dividerPaint.color = dividerColor
        dividerPaint.strokeWidth = dividerSize.toFloat()
        dividerPaint.style = Paint.Style.STROKE
        dividerPaint.isAntiAlias = true
        this.spanCount = spanCount
    }

    override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        drawHorizontal(c, parent)
        drawVertical(c, parent)
    }

    private fun drawHorizontal(canvas: Canvas, parent: RecyclerView) {
        val itemCount = parent.adapter!!.itemCount
        val childCount = parent.childCount
        val lastRowChildCount = getLastRowChildCount(itemCount)
        for (i in 0 until childCount) {
            val child = parent.getChildAt(i)
            if (isChildInLastRow(parent, child, itemCount, lastRowChildCount)) {
                continue
            }
            parent.getDecoratedBoundsWithMargins(child, bounds)
            val y = bounds.bottom
            val startX = bounds.left
            val stopX = bounds.right
            canvas.drawLine(startX.toFloat(), y.toFloat(), stopX.toFloat(), y.toFloat(), dividerPaint)
        }
    }

    private fun drawVertical(canvas: Canvas, parent: RecyclerView) {
        val childCount = parent.childCount
        val isRTL = ViewCompat.getLayoutDirection(parent) == ViewCompat.LAYOUT_DIRECTION_RTL
        for (i in 0 until childCount) {
            val child = parent.getChildAt(i)
            if (isChildInLastColumn(parent, child)) {
                continue
            }
            parent.getDecoratedBoundsWithMargins(child, bounds)
            val x = if (isRTL) bounds.left else bounds.right
            val startY = bounds.top
            val stopY = bounds.bottom
            canvas.drawLine(x.toFloat(), startY.toFloat(), x.toFloat(), stopY.toFloat(), dividerPaint)
        }
    }

    private fun getLastRowChildCount(itemCount: Int): Int {
        val gridChildRemainder = itemCount % spanCount
        return if (gridChildRemainder == 0) spanCount else gridChildRemainder
    }

    private fun isChildInLastRow(
        parent: RecyclerView,
        child: View,
        itemCount: Int,
        lastRowChildCount: Int
    ): Boolean {
        return parent.getChildAdapterPosition(child) >= itemCount - lastRowChildCount
    }

    private fun isChildInLastColumn(parent: RecyclerView, child: View): Boolean {
        return parent.getChildAdapterPosition(child) % spanCount == spanCount - 1
    }
}
