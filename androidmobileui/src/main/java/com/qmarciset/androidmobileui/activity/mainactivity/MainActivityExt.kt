/*
 * Created by Quentin Marciset on 31/3/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmarciset.androidmobileui.activity.mainactivity

import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.ui.setupActionBarWithNavController
import com.qmarciset.androidmobileapi.connectivity.NetworkUtils
import com.qmarciset.androidmobiledatasync.viewmodel.ConnectivityViewModel
import com.qmarciset.androidmobiledatasync.viewmodel.LoginViewModel
import com.qmarciset.androidmobiledatasync.viewmodel.factory.ConnectivityViewModelFactory
import com.qmarciset.androidmobiledatasync.viewmodel.factory.EntityListViewModelFactory
import com.qmarciset.androidmobiledatasync.viewmodel.factory.LoginViewModelFactory
import com.qmarciset.androidmobileui.R
import com.qmarciset.androidmobileui.app.BaseApp
import com.qmarciset.androidmobileui.utils.setupWithNavController
import kotlinx.android.synthetic.main.activity_main.*

fun MainActivity.getMainActivityViewModel() {
    // Get LoginViewModel
    loginViewModel = ViewModelProvider(
        this,
        LoginViewModelFactory(appInstance, loginApiService)
    )[LoginViewModel::class.java]

    // Get ConnectivityViewModel
    if (NetworkUtils.sdkNewerThanKitKat) {
        connectivityViewModel = ViewModelProvider(
            this,
            ConnectivityViewModelFactory(appInstance, connectivityManager)
        )[ConnectivityViewModel::class.java]
    }

    // Get EntityListViewModel list
    entityListViewModelList = mutableListOf()
    for (tableName in fromTableInterface.tableNames) {
        val kClazz = fromTableInterface.entityListViewModelClassFromTable(tableName)

        entityListViewModelList.add(
            ViewModelProvider(
                this,
                EntityListViewModelFactory(
                    appInstance,
                    tableName,
                    appDatabaseInterface,
                    apiService,
                    fromTableForViewModel
                )
            )[kClazz.java]
        )
    }
}

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
