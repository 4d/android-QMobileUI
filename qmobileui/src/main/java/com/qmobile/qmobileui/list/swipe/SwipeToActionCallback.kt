/*
 * Created by qmarciset on 23/7/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.list.swipe

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.view.View
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.binding.getColorFromAttr
import com.qmobile.qmobileui.ui.SwipeHelper
import java.util.*
import kotlin.math.max

@SuppressLint("ClickableViewAccessibility")
abstract class SwipeToActionCallback(
    private val context: Context,
    private val recyclerView: RecyclerView
) : ItemTouchHelper.SimpleCallback(
    ItemTouchHelper.ACTION_STATE_IDLE,
    ItemTouchHelper.LEFT
) {

    companion object {
        private const val BUTTON_RADIUS = 20F
        private const val BUTTON_STROKE = 10F
        private const val VERTICAL_MARGIN = 70F
        const val BUTTON_WIDTH_FACTOR = 4.5f
    }

    private var swipedPosition = -1
    private val buttonsBuffer: MutableMap<Int, List<ItemActionButton>> = mutableMapOf()

    private val paint = Paint()
    private val paintStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColorFromAttr(R.attr.colorSurface)
        style = Paint.Style.STROKE
        strokeWidth = BUTTON_STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }

    private val titleBounds = Rect()

    private val recoverQueue = object : LinkedList<Int>() {
        override fun add(element: Int): Boolean {
            if (contains(element)) return false
            return super.add(element)
        }
    }

    private val touchListener = View.OnTouchListener { _, event ->
        if (swipedPosition < 0) return@OnTouchListener false
        buttonsBuffer[swipedPosition]?.forEach { it.handleEvent(event) }
        recoverQueue.add(swipedPosition)
        swipedPosition = -1
        recoverSwipedItem()
        true
    }

    init {
        recyclerView.setOnTouchListener(touchListener)
    }

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        return false
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        val position = viewHolder.bindingAdapterPosition
        if (swipedPosition != position) recoverQueue.add(swipedPosition)
        swipedPosition = position
        recoverSwipedItem()
    }

    private fun recoverSwipedItem() {
        while (!recoverQueue.isEmpty()) {
            val position = recoverQueue.poll() ?: return
            recyclerView.adapter?.notifyItemChanged(position)
        }
    }

    private fun List<ItemActionButton>.computeIntrinsicWidth(): Float {
        if (isEmpty()) return 0.0f
        return map { it.intrinsicWidth }.reduce { acc, fl -> acc + fl }
    }

    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        val position = viewHolder.bindingAdapterPosition
        var maxDX = dX
        val itemView = viewHolder.itemView
        val isCanceled = dX == 0f && !isCurrentlyActive

        if (isCanceled) {
            SwipeHelper.clearCanvas(
                c,
                itemView.right + dX,
                itemView.top.toFloat(),
                itemView.right.toFloat(),
                itemView.bottom.toFloat()
            )
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            return
        }

        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
            if (dX < 0) {
                c.clipRect(itemView.right + dX.toInt(), itemView.top, itemView.right, itemView.bottom)

                if (!buttonsBuffer.containsKey(position)) {
                    buttonsBuffer[position] = instantiateUnderlayButton(position)
                }

                val buttons = buttonsBuffer[position]
                if (buttons.isNullOrEmpty()) return
                maxDX = max(-buttons.computeIntrinsicWidth(), dX)
                drawButtons(c, buttons, itemView)
            }
        }

        super.onChildDraw(c, recyclerView, viewHolder, maxDX, dY, actionState, isCurrentlyActive)
    }

    private fun drawButtons(
        canvas: Canvas,
        buttons: List<ItemActionButton>,
        itemView: View
    ) {
        var right: Float = itemView.right.toFloat()
        val itemHeight: Float = (itemView.bottom - itemView.top).toFloat()
        val itemWidth: Float = (itemView.right - itemView.left).toFloat()
        val buttonWidth: Float = itemWidth / BUTTON_WIDTH_FACTOR
        var left: Float

        buttons.forEach { button ->
            // Calculate position of icon
            val iconDrawable = button.icon
            left = right - buttonWidth
            val iconLeft = right - (buttonWidth / 2) - (button.iconIntrinsicWidth / 2)
            val iconTop = itemView.top + (itemHeight - button.iconIntrinsicHeight) / 2
            val iconRight = right - (buttonWidth / 2) + (button.iconIntrinsicWidth / 2)
            val iconBottom = iconTop + button.iconIntrinsicHeight

            // Draw the background
            paint.color = button.backgroundColor
            val rect = RectF(left, itemView.top.toFloat(), right, itemView.bottom.toFloat())
            button.clickableRegion = rect
            canvas.drawRoundRect(rect, BUTTON_RADIUS, BUTTON_RADIUS, paint)
            canvas.drawRoundRect(rect, BUTTON_RADIUS, BUTTON_RADIUS, paintStroke)

            // Draw the delete icon
            iconDrawable?.setBounds(iconLeft.toInt(), iconTop.toInt(), iconRight.toInt(), iconBottom.toInt())
            iconDrawable?.draw(canvas)

            // Draw title
            button.textPaint.getTextBounds(button.title, 0, button.title.length, titleBounds)
            val x: Float = buttonWidth / 2f - titleBounds.width() / 2f - titleBounds.left
            canvas.drawText(button.title, left + x, iconBottom + VERTICAL_MARGIN, button.textPaint)

            right = left
        }
    }

    abstract fun instantiateUnderlayButton(position: Int): List<ItemActionButton>
}
