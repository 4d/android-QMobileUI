/*
 * Created by Quentin Marciset on 26/6/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmarciset.androidmobileui.activity.mainactivity

import androidx.lifecycle.Observer
import androidx.navigation.ui.setupActionBarWithNavController
import com.qmarciset.androidmobiledatasync.app.BaseApp
import com.qmarciset.androidmobileui.R
import com.qmarciset.androidmobileui.utils.setupWithNavController
import kotlinx.android.synthetic.main.activity_main.*

/**
 * Called on first creation and when restoring state.
 */
fun MainActivity.setupBottomNavigationBar() {
    bottom_nav.menu.clear() // clear old inflated items.
    BaseApp.bottomNavigationMenu?.let {
        bottom_nav.inflateMenu(it)
    }
    val navGraphIds = BaseApp.navGraphIds

    // Setup the bottom navigation view with a list of navigation graphs
    val controller = bottom_nav.setupWithNavController(
        navGraphIds = navGraphIds,
        fragmentManager = supportFragmentManager,
        containerId = R.id.nav_host_container,
        intent = intent
    )
    // Whenever the selected controller changes, setup the action bar.
    controller.observe(
        this,
        Observer { navController ->
            setupActionBarWithNavController(navController)
        }
    )
    currentNavController = controller
}
