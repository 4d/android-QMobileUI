/*
 * Created by htemanni on 13/9/2021.
 * 4D SAS
 * Copyright (c) 2021 htemanni. All rights reserved.
 */

package com.qmobile.qmobileui

import com.qmobile.qmobileui.utils.ResourcesHelper

class Action(
    val name: String,
    private val icon: String?,
    private val label: String?,
    private val shortLabel: String?,
    val parameters: Array<Pair<String, Any>>
) {
    fun getIconDrawablePath(): String? {
        return icon?.let {
            ResourcesHelper.correctIconPath(it)
        }
    }

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
}
