/*
 * Created by qmarciset on 25/11/2021.
 * 4D SAS
 * Copyright (c) 2021 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.actions

import com.qmobile.qmobileui.utils.ResourcesHelper

class Action(
    val name: String,
    private val icon: String?,
    private val label: String?,
    private val shortLabel: String?,
    val parameters: List<ActionParameters>?
) {
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
}
