/*
 * Created by Quentin Marciset on 23/1/2021.
 * 4D SAS
 * Copyright (c) 2021 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.utils

import android.content.Context
import com.qmobile.qmobileui.R
import timber.log.Timber
import java.io.File

object ResourcesHelper {

    private const val LAYOUT_RES_TYPE = "layout"
    private const val RV_ITEM_PREFIX = "recyclerview_item"
    private const val FRAGMENT_DETAIL_PREFIX = "fragment_detail"

    /**
     * Provides the appropriate RecyclerView item layout
     */
    fun itemLayoutFromTable(context: Context, tableName: String): Int =
        context.resources.getIdentifier(
            "${RV_ITEM_PREFIX}_$tableName".lowercase(),
            LAYOUT_RES_TYPE,
            context.packageName
        )

    /**
     * Provides the appropriate detail layout
     */
    fun detailLayoutFromTable(context: Context, tableName: String): Int =
        context.resources.getIdentifier(
            "${FRAGMENT_DETAIL_PREFIX}_$tableName".lowercase(),
            LAYOUT_RES_TYPE,
            context.packageName
        )

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

    fun correctIconPath(iconPath: String?): String? {
        if (iconPath.isNullOrEmpty())
            return null
        return try {
            iconPath
                .substring(0, iconPath.lastIndexOf('.')) // removes extension
                .replace(".+/".toRegex(), "")
                .removePrefix(File.separator)
                .lowercase()
                .replace("[^a-z0-9]+".toRegex(), "_")
        } catch (e: StringIndexOutOfBoundsException) {
            Timber.e("Could not get iconPath : ${e.message}")
            null
        }
    }
}
