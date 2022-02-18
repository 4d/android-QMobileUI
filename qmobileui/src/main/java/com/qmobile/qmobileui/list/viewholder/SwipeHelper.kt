package com.qmobile.qmobileui.list.viewholder

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.action.Action
import com.qmobile.qmobileui.binding.getColorFromAttr
import com.qmobile.qmobileui.utils.ColorHelper
import java.util.LinkedList
import kotlin.math.abs
import kotlin.math.max

const val HORIZONTAL_PADDING = 50.0f
const val BUTTON_TEXT_SIZE = 10.0f
// Use as margin bottom from the center for icon and as margin top from the center for title
const val VERTICAL_MARGIN = 25F
const val ICON_WIDTH_FACTOR = 0.3F
const val SCREEN_WIDTH_FACTOR = 4
const val TRUNCATE_FACTOR = 5

@SuppressLint("ClickableViewAccessibility")
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
        val position = viewHolder.bindingAdapterPosition
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
        val position = viewHolder.bindingAdapterPosition
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
        private var horizontalIndex: Int,
        private val clickListener: UnderlayButtonClickListener
    ) {
        private var title: String = action?.getPreferredShortName() ?: "..."
        private var clickableRegion: RectF? = null
        private val textSizeInPixel: Float =
            BUTTON_TEXT_SIZE * context.resources.displayMetrics.density // dp to px
        val intrinsicWidth: Float

        init {
            val screenWidth: Int = context.resources.displayMetrics.widthPixels
            val paint = Paint()
            paint.textSize = textSizeInPixel
            paint.typeface = Typeface.DEFAULT_BOLD
            paint.textAlign = Paint.Align.LEFT
            val titleBounds = Rect()

            paint.getTextBounds(title, 0, title.length, titleBounds)
            intrinsicWidth = (screenWidth / SCREEN_WIDTH_FACTOR).toFloat() // Fix button width to screenWidth/4
        }

        fun draw(canvas: Canvas, rect: RectF) {
            val paint = Paint()
            // Draw background
            paint.color = ColorHelper.getActionButtonColor(horizontalIndex, context)
            canvas.drawRect(rect, paint)

            // Draw icon
            var iconResId = 0
            val iconDrawablePath = action?.getIconDrawablePath()
            if (iconDrawablePath != null && iconDrawablePath.isNotEmpty()) {
                iconResId = context.resources.getIdentifier(iconDrawablePath, "drawable", context.packageName)
            }
            val iconWith = rect.width() * (ICON_WIDTH_FACTOR)
            val iconHeight = iconWith

            val iconLeft =
                (rect.left + rect.width() / 2 - iconWith.div(2))

            val iconBottom = rect.bottom - rect.height() / 2
            val iconTop = iconBottom - iconHeight
            val iconRight = iconLeft + iconWith

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

            iconDrawable?.setBounds(
                iconLeft.toInt(),
                iconTop.toInt(),
                iconRight.toInt(),
                iconBottom.toInt()
            )
            iconDrawable?.draw(canvas)

            // Draw title
            paint.color = context.getColorFromAttr(R.attr.colorOnPrimary)
            paint.textSize = textSizeInPixel
            paint.typeface = Typeface.DEFAULT_BOLD
            paint.textAlign = Paint.Align.LEFT

            title = ellipsize(title, paint, intrinsicWidth - HORIZONTAL_PADDING)
            val titleBounds = Rect()
            paint.getTextBounds(title, 0, title.length, titleBounds)
            val x = rect.width() / 2 + titleBounds.width() / 2 - titleBounds.right
            canvas.drawText(title, rect.left + x, iconBottom + VERTICAL_MARGIN, paint)
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

fun ellipsize(input: String, paint: Paint, maxWidth: Float): String {
    val titleBounds = Rect()
    paint.getTextBounds(input, 0, input.length, titleBounds)
    return if (titleBounds.width() < maxWidth)
        input
    else
        ellipsize(input.substring(0, input.length - TRUNCATE_FACTOR) + "...", paint, maxWidth)
}

private fun List<SwipeHelper.ItemActionButton>.intrinsicWidth(): Float {
    if (isEmpty()) return 0.0f
    return map { it.intrinsicWidth }.reduce { acc, fl -> acc + fl }
}
