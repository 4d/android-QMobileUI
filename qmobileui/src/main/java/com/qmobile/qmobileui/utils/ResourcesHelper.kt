/*
 * Created by Quentin Marciset on 23/1/2021.
 * 4D SAS
 * Copyright (c) 2021 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.utils

import android.content.Context
import com.qmobile.qmobileui.R

object ResourcesHelper {

    private const val LAYOUT_RES_TYPE = "layout"
    const val RV_ITEM_PREFIX = "recyclerview_item"
    const val FRAGMENT_DETAIL_PREFIX = "fragment_detail"

    // Generic Layout Inflater
    fun layoutFromTable(
        context: Context,
        tableName: String,

    ): Int {
        return when ((tableName.split("_"))[0]) {
            "recyclerview" -> {
                context.resources.getIdentifier(tableName, LAYOUT_RES_TYPE, context.packageName)
            }
            "fragment" -> {
                context.resources.getIdentifier(tableName, LAYOUT_RES_TYPE, context.packageName)
            }
            else -> 0
        }
    }

    /**
     * Gets a resource string to display to the user. As it's called in a ViewModel, there is no
     * context available
     */
    fun fetchResourceString(context: Context, string: String): String {
        return when (string) {
            "try_refresh_data" -> context.getString(R.string.try_refresh_data)
            else -> string
        }
    }
}
