package com.qmobile.qmobileui.utils

import android.content.Context
import android.graphics.Color
import androidx.core.graphics.ColorUtils
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.binding.getColorFromAttr
import kotlin.math.min
import kotlin.math.roundToInt

private const val LUMINANCE_DARK_THRESHOLD = 0.5F
private const val LUMINANCE_BRIGHT_THRESHOLD = 0.8F
private const val DARK_COLOR_FACTOR = 1.1F
private const val BRIGHT_COLOR_FACTOR = 0.96F
private const val MEDIUM_COLOR_FACTOR = 1.04F
private const val ARGB_MAX_VALUE = 255

object ColorHelper {
    fun getActionButtonColor(horizontalIndex: Int, context: Context): Int {
        val themeColor = context.getColorFromAttr(R.attr.colorPrimary)
        val themeColorLuminance = ColorUtils.calculateLuminance(themeColor)

        if (horizontalIndex == 0)
            return themeColor

        return when {
            // dark
            themeColorLuminance < LUMINANCE_DARK_THRESHOLD -> {
                manipulateColor(themeColor, DARK_COLOR_FACTOR, horizontalIndex)
            }
            // bright
            themeColorLuminance > LUMINANCE_BRIGHT_THRESHOLD -> {
                manipulateColor(themeColor, BRIGHT_COLOR_FACTOR, horizontalIndex)
            }
            // medium
            else -> {
                manipulateColor(themeColor, MEDIUM_COLOR_FACTOR, horizontalIndex)
            }
        }
    }

    private fun manipulateColor(color: Int, factor: Float, horizontalIndex: Int): Int {
        var colorTmp = color
        for (i in 1..horizontalIndex) {
            val a = Color.alpha(colorTmp)
            val r = (Color.red(colorTmp) * factor).roundToInt()
            val g = (Color.green(colorTmp) * factor).roundToInt()
            val b = (Color.blue(colorTmp) * factor).roundToInt()
            colorTmp = Color.argb(
                a,
                min(r, ARGB_MAX_VALUE),
                min(g, ARGB_MAX_VALUE),
                min(b, ARGB_MAX_VALUE)
            )
        }
        return colorTmp
    }
}
