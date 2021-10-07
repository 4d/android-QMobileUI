package com.qmobile.qmobileui.list.viewholder

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.qmobile.qmobileui.Action
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.binding.getColorFromAttr
import java.util.LinkedList
import kotlin.math.abs
import kotlin.math.max

const val HORIZONTAL_PADDING = 50.0f
const val BUTTON_TEXT_SIZE = 14.0f
const val ICON_RATIO = 0.25F
const val TITLE_MARGIN_TOP_RATIO = 0.75F

abstract class SwipeHelper(
    private val recyclerView: RecyclerView
) : ItemTouchHelper.SimpleCallback(
    ItemTouchHelper.ACTION_STATE_IDLE,
    ItemTouchHelper.LEFT
) {
    private var swipedPosition = -1
    private val buttonsBuffer: MutableMap<Int, List<ItemActionButton>> = mutableMapOf()
    private val recoverQueue = object : LinkedList<Int>() {
        override fun add(element: Int): Boolean {
            if (contains(element)) return false
            return super.add(element)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private val touchListener = View.OnTouchListener { _, event ->
        if (swipedPosition < 0) return@OnTouchListener false
        buttonsBuffer[swipedPosition]?.forEach { it.handle(event) }
        recoverQueue.add(swipedPosition)
        swipedPosition = -1
        recoverSwipedItem()
        true
    }

    init {
        recyclerView.setOnTouchListener(touchListener)
    }

    private fun recoverSwipedItem() {
        while (!recoverQueue.isEmpty()) {
            val position = recoverQueue.poll() ?: return
            recyclerView.adapter?.notifyItemChanged(position)
        }
    }

    private fun drawButtons(
        canvas: Canvas,
        buttons: List<ItemActionButton>,
        itemView: View,
        dX: Float
    ) {
        var right = itemView.right
        buttons.forEach { button ->
            val width = button.intrinsicWidth / buttons.intrinsicWidth() * abs(dX)
            val left = right - width
            button.draw(
                canvas,
                RectF(left, itemView.top.toFloat(), right.toFloat(), itemView.bottom.toFloat())
            )
            right = left.toInt()
        }
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
        val position = viewHolder.adapterPosition
        var maxDX = dX
        val itemView = viewHolder.itemView

        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
            if (dX < 0) {
                if (!buttonsBuffer.containsKey(position)) {
                    buttonsBuffer[position] = instantiateUnderlayButton(position)
                }

                val buttons = buttonsBuffer[position] ?: return
                if (buttons.isEmpty()) return
                maxDX = max(-buttons.intrinsicWidth(), dX)
                drawButtons(c, buttons, itemView, maxDX)
            }
        }

        super.onChildDraw(
            c,
            recyclerView,
            viewHolder,
            maxDX,
            dY,
            actionState,
            isCurrentlyActive
        )
    }

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        return false
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        val position = viewHolder.adapterPosition
        if (swipedPosition != position) recoverQueue.add(swipedPosition)
        swipedPosition = position
        recoverSwipedItem()
    }

    abstract fun instantiateUnderlayButton(position: Int): List<ItemActionButton>
    interface UnderlayButtonClickListener {
        fun onClick()
    }

    class ItemActionButton(
        private val context: Context,
        private val action: Action?,
        horizontalIndex: Int,
        private val clickListener: UnderlayButtonClickListener
    ) {
        private var title: String
        private var clickableRegion: RectF? = null
        private val textSizeInPixel: Float =
            BUTTON_TEXT_SIZE * context.resources.displayMetrics.density // dp to px
        val intrinsicWidth: Float
        private var horizontalIndex: Int;

        init {
            title = action?.getPreferredShortName() ?: "..."
            this.horizontalIndex = horizontalIndex
            val paint = Paint()
            paint.textSize = textSizeInPixel
            paint.typeface = Typeface.DEFAULT_BOLD
            paint.textAlign = Paint.Align.LEFT
            val titleBounds = Rect()

            paint.getTextBounds(title, 0, title.length, titleBounds)
            intrinsicWidth = titleBounds.width() + 2 * HORIZONTAL_PADDING
        }

        fun draw(canvas: Canvas, rect: RectF) {
            val paint = Paint()
            // Draw background
            val color =
                if (horizontalIndex % 2 == 0) android.R.attr.colorPrimary else R.attr.colorPrimaryVariant
            paint.color = context.getColorFromAttr(color)
            canvas.drawRect(rect, paint)
            // Draw icon
            var iconResId = 0
            val iconDrawablePath = action?.getIconDrawablePath()
            if (iconDrawablePath != null && iconDrawablePath.isNotEmpty()) {
                iconResId =
                    context.resources.getIdentifier(
                        iconDrawablePath,
                        "drawable",
                        context.packageName
                    )
            }

            if (iconResId != 0) {
                val iconDrawable = AppCompatResources.getDrawable(
                    context,
                    iconResId
                )
                val iconWith = rect.width() * ICON_RATIO
                val iconHeight = rect.height() * ICON_RATIO

                iconDrawable?.apply {
                    setTint(context.getColorFromAttr(R.attr.colorOnPrimary))
                    val iconLeft =
                        (rect.left + rect.width() / 2 - iconWith.div(2))
                    val iconTop =
                        (rect.top + rect.height()* ICON_RATIO)
                    val iconBottom = iconTop + iconHeight
                    val iconRight = iconLeft + iconWith
                    setBounds(
                        iconLeft.toInt(),
                        iconTop.toInt(),
                        iconRight.toInt(),
                        iconBottom.toInt()
                    )
                    draw(canvas)

                    // Draw title
                    paint.color = ContextCompat.getColor(context, android.R.color.white)
                    paint.textSize = textSizeInPixel
                    paint.typeface = Typeface.DEFAULT
                    paint.textAlign = Paint.Align.LEFT
                    val titleBounds = Rect()
                    paint.getTextBounds(title, 0, title.length, titleBounds)
                    val x = rect.width() / 2 + titleBounds.width().div(2) - titleBounds.right
                    val y = rect.top + rect.height() * TITLE_MARGIN_TOP_RATIO
                    canvas.drawText(title, rect.left + x, y, paint)
                }

            } else {
                // Draw title
                paint.color = ContextCompat.getColor(context, android.R.color.white)
                paint.textSize = textSizeInPixel
                paint.typeface = Typeface.DEFAULT_BOLD
                paint.textAlign = Paint.Align.LEFT
                val titleBounds = Rect()
                paint.getTextBounds(title, 0, title.length, titleBounds)
                val x = rect.width() / 2 + titleBounds.width() / 2 - titleBounds.right
                val y = rect.height() / 2 + titleBounds.height() / 2 - titleBounds.bottom
                canvas.drawText(title, rect.left + x, rect.top + y, paint)
            }
            clickableRegion = rect
        }

        fun handle(event: MotionEvent) {
            clickableRegion?.let {
                if (it.contains(event.x, event.y)) {
                    clickListener.onClick()
                }
            }
        }
    }
}

private fun List<SwipeHelper.ItemActionButton>.intrinsicWidth(): Float {
    if (isEmpty()) return 0.0f
    return map { it.intrinsicWidth }.reduce { acc, fl -> acc + fl }
}
