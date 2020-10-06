/*
 * Created by Quentin Marciset on 17/6/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.utils

import android.content.Context
import java.util.Locale

private const val LAYOUT_RES_TYPE = "layout"
private const val RV_ITEM_PREFIX = "recyclerview_item_"
private const val FRAGMENT_DETAIL_PREFIX = "fragment_detail_"

/**
 * Provides the appropriate RecyclerView item layout
 */
fun itemLayoutFromTable(context: Context, tableName: String): Int =
    context.resources.getIdentifier(
        "$RV_ITEM_PREFIX${tableName.toLowerCase(Locale.getDefault())}",
        LAYOUT_RES_TYPE,
        context.packageName
    )

/**
 * Provides the appropriate detail layout
 */
fun detailLayoutFromTable(context: Context, tableName: String): Int =
    context.resources.getIdentifier(
        "$FRAGMENT_DETAIL_PREFIX${tableName.toLowerCase(Locale.getDefault())}",
        LAYOUT_RES_TYPE,
        context.packageName
    )
