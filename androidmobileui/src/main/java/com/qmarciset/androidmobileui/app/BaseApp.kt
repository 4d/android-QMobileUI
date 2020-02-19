/*
 * Created by Quentin Marciset on 18/2/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmarciset.androidmobileui.app

import androidx.multidex.MultiDexApplication
import com.qmarciset.androidmobiledatastore.db.AppDatabaseInterface
import com.qmarciset.androidmobiledatasync.utils.FromTableForViewModel
import com.qmarciset.androidmobileui.utils.FromTableInterface
import com.qmarciset.androidmobileui.utils.NavigationInterface
import com.qmarciset.androidmobileui.utils.ViewDataBindingInterface

open class BaseApp : MultiDexApplication() {

    companion object {

        // Provides Application instance
        lateinit var instance: BaseApp

        // Provides the drawable resource id for login page logo
        var loginLogoDrawable: Int? = null

        // Provides the menu resource id for bottom navigation
        var bottomNavigationMenu: Int? = null

        // Provides navigation graphs id list for navigation
        lateinit var navGraphIds: List<Int>

        // Provides interfaces to get data coming from outside the SDK
        lateinit var appDatabaseInterface: AppDatabaseInterface
        lateinit var fromTableInterface: FromTableInterface
        lateinit var fromTableForViewModel: FromTableForViewModel
        lateinit var navigationInterface: NavigationInterface
        lateinit var viewDataBindingInterface: ViewDataBindingInterface
    }
}
