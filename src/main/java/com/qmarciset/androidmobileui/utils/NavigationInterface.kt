package com.qmarciset.androidmobileui.utils

import android.view.View

interface NavigationInterface {

    fun navigateFromListToViewPager(view: View, position: Int, tableName: String)
}
