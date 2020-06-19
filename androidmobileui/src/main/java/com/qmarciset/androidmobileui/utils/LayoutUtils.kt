/*
 * Created by Quentin Marciset on 17/6/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmarciset.androidmobileui.utils

import android.content.Context
import java.util.Locale

/**
 * Provides the appropriate RecyclerView item layout
 */
fun itemLayoutFromTable(context: Context, tableName: String): Int =
    context.resources.getIdentifier(
        "recyclerview_item_${tableName.toLowerCase(Locale.getDefault())}",
        "layout",
        context.packageName
    )

/**
 * Provides the appropriate detail layout
 */
fun detailLayoutFromTable(context: Context, tableName: String): Int =
    context.resources.getIdentifier(
        "fragment_detail_${tableName.toLowerCase(Locale.getDefault())}",
        "layout",
        context.packageName
    )
