/*
 * Created by Quentin Marciset on 26/6/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.activity.mainactivity

import androidx.lifecycle.Observer
import androidx.navigation.ui.setupActionBarWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.utils.setupWithNavController

/**
 * Called on first creation and when restoring state.
 */
fun MainActivity.setupBottomNavigationBar() {
    val bottomNav = this.findViewById<BottomNavigationView>(R.id.bottom_nav)
    bottomNav.menu.clear() // clear old inflated items.
    BaseApp.bottomNavigationMenu?.let {
        bottomNav.inflateMenu(it)
    }

    val navGraphIds = BaseApp.navGraphIds

    // Setup the bottom navigation view with a list of navigation graphs
    val controller = bottomNav.setupWithNavController(
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
