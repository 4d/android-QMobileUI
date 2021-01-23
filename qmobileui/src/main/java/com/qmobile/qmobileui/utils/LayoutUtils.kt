/*
 * Created by Quentin Marciset on 17/6/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.utils

import android.content.Context
import com.qmobile.qmobileui.model.QMobileUiConstants

// Generic Layout Inflater
fun layoutFromTable(context: Context, tableName: String, defType: String = QMobileUiConstants.LAYOUT): Int {
    when ((tableName.split("_"))[0]) {
        "recyclerview" -> {
            return context.resources.getIdentifier(tableName, defType, context.packageName)
        }
        "fragment" -> {
            return context.resources.getIdentifier(tableName, defType, context.packageName)
        }

    }
    return 0
}
