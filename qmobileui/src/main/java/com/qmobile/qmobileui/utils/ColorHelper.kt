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

object ColorHelper {
    fun getActionButtonColor(horizontalIndex: Int, context: Context): Int {
        val themeColor = context.getColorFromAttr(R.attr.colorPrimary)
        val themeColorLuminance = ColorUtils.calculateLuminance(themeColor);

        if (horizontalIndex == 0)
            return themeColor

        return when {
            // dark
            themeColorLuminance < LUMINANCE_DARK_THRESHOLD -> {
                manipulateColor(themeColor, 1.1F, horizontalIndex)
            }
            // bright
            themeColorLuminance > LUMINANCE_BRIGHT_THRESHOLD -> {
                manipulateColor(themeColor, 0.96F, horizontalIndex)
            }
            // medium
            else -> {
                manipulateColor(themeColor, 1.04F, horizontalIndex)
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
                min(r, 255),
                min(g, 255),
                min(b, 255)
            )
        }
        return colorTmp
    }

}