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

    fun isAppUrlScheme(context: Context, url: String): Boolean {
        val scheme = context.resources.getString(R.string.deeplink_scheme)
        return url.lowercase().startsWith(scheme.lowercase())
    }

    fun isUniversalLink(context: Context, url: String): Boolean {
        val scheme = context.resources.getString(R.string.universal_link_scheme)
        val host = context.resources.getString(R.string.universal_link_host)
        return url.lowercase().startsWith(scheme + host)
    }
}
