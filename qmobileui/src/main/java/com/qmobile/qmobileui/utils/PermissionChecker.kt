/*
 * Created by qmarciset on 3/2/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.utils

import android.content.Context
import com.qmobile.qmobileui.activity.mainactivity.MainActivity

interface PermissionChecker {

    fun askPermission(
        context: Context,
        permission: String,
        rationale: String,
        callback: (isGranted: Boolean) -> Unit
    ) {
        (context as MainActivity?)?.askPermission(permission, rationale, callback)
    }
}
