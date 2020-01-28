package com.qmarciset.androidmobileui.utils

import android.view.View

// Interface implemented by MainActivity to provide different elements depending of the generated type
interface NavigationInterface {

    // Navigates from ListView to ViewPager (which displays one DetailView)
    fun navigateFromListToViewPager(view: View, position: Int, tableName: String)
}
