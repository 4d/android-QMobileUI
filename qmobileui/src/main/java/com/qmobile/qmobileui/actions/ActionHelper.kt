/*
 * Created by qmarciset on 25/11/2021.
 * 4D SAS
 * Copyright (c) 2021 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.actions

import com.qmobile.qmobileapi.utils.getSafeArray
import com.qmobile.qmobileapi.utils.parseToType
import com.qmobile.qmobiledatasync.app.BaseApp
import org.json.JSONObject

const val DROP_DOWN_WIDTH = 600

fun MutableList<Action>.addActions(sourceJSONObject: JSONObject, tableName: String) {
    sourceJSONObject.getSafeArray(tableName)?.length()?.let { length ->
        for (i in 0 until length) {
            val jsonObject = sourceJSONObject.getSafeArray(tableName)?.getJSONObject(i)
            BaseApp.mapper.parseToType<Action>(jsonObject.toString())?.let { action ->
                this.add(action)
            }
        }
    }
}
