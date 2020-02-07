/*
 * Created by Quentin Marciset on 7/2/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmarciset.androidmobileui.utils

import android.view.View

/**
 * Interface implemented by MainActivity to provide different elements depending of the generated type
 */
interface NavigationInterface {

    /**
     * Navigates from ListView to ViewPager (which displays one DetailView)
     */
    fun navigateFromListToViewPager(view: View, position: Int, tableName: String)
}
