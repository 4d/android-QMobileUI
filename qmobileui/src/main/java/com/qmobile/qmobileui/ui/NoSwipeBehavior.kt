/*
 * Created by qmarciset on 29/11/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.ui

import android.view.View
import com.google.android.material.snackbar.BaseTransientBottomBar

class NoSwipeBehavior : BaseTransientBottomBar.Behavior() {

    override fun canSwipeDismissView(child: View): Boolean {
        return false
    }
}
