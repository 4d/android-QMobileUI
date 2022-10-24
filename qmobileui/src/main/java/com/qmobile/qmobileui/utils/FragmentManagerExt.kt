/*
 * Created by qmarciset on 29/9/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.utils

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.qmobile.qmobileui.R

fun FragmentManager.getParentFragment(): Fragment? {
    return this.findFragmentById(R.id.nav_host_container)?.childFragmentManager?.fragments?.get(0)
}
