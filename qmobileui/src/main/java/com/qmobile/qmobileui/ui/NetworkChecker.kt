/*
 * Created by qmarciset on 9/7/2021.
 * 4D SAS
 * Copyright (c) 2021 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.ui

interface NetworkChecker {
    fun onServerAccessible()
    fun onServerInaccessible()
    fun onNoInternet()
}
