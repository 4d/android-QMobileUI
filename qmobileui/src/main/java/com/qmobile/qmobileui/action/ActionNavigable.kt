/*
 * Created by qmarciset on 4/2/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.action

interface ActionNavigable : ActionProvider {

    fun navigationToActionForm(action: Action, itemId: String?)
}
