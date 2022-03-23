/*
 * Created by htemanni on 13/9/2021.
 * 4D SAS
 * Copyright (c) 2021 htemanni. All rights reserved.
 */

package com.qmobile.qmobileui.action

import com.qmobile.qmobileui.utils.ResourcesHelper
import org.json.JSONArray
import java.util.Locale
import java.util.UUID

class Action(
    val name: String = "",
    private val icon: String?,
    private val label: String?,
    private val shortLabel: String?,
    val preset: String?,
    val parameters: JSONArray
) {
    val id = UUID.randomUUID().toString()
    fun getIconDrawablePath(): String? =
        ResourcesHelper.correctIconPath(icon)

    fun getPreferredName(): String {
        return if (!label.isNullOrEmpty())
            label
        else if (!shortLabel.isNullOrEmpty())
            shortLabel
        else
            name
    }

    fun getPreferredShortName(): String {
        return if (!shortLabel.isNullOrEmpty())
            shortLabel
        else if (!label.isNullOrEmpty())
            label
        else
            name
    }

    fun isOfflineCompatible(): Boolean {
        return preset?.lowercase(Locale.getDefault()) != "share"
    }
}
