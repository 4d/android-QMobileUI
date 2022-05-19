/*
 * Created by qmarciset on 4/2/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.action

import com.qmobile.qmobileui.ActionActivity

interface ActionProvider {

    val tableName: String
    val actionActivity: ActionActivity

    fun getActionContent(itemId: String?): MutableMap<String, Any>
}
