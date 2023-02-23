/*
 * Created by qmarciset on 7/2/2023.
 * 4D SAS
 * Copyright (c) 2023 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.utils

import android.content.Context
import com.qmobile.qmobileui.R

object DeepLinkUtil {

    const val PN_DEEPLINK_DATACLASS = "push_notification_dataClass"
    const val PN_DEEPLINK_PRIMARY_KEY = "push_notification_primaryKey"

    fun hasAppUrlScheme(context: Context, url: String): Boolean {
        val schemes = context.resources.getStringArray(R.array.url_schemes)
        schemes.forEach { scheme ->
            if (url.startsWith(scheme)) {
                return true
            }
        }
        return false
    }
}