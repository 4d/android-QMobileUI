/*
 * Created by qmarciset on 24/5/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.action.utils

import androidx.room.TypeConverter
import com.qmobile.qmobileapi.utils.parseToString
import com.qmobile.qmobileapi.utils.parseToType
import com.qmobile.qmobiledatastore.dao.ActionInfo
import com.qmobile.qmobiledatasync.app.BaseApp

class ActionInfoConverter {

    @TypeConverter
    fun actionInfoToString(obj: ActionInfo?): String = BaseApp.mapper.parseToString(obj)

    @TypeConverter
    fun stringToActionInfo(str: String?): ActionInfo? = BaseApp.mapper.parseToType(str)
}
