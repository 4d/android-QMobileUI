/*
 * Created by Quentin Marciset on 7/2/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui

/**
 * Base interface for fragments
 */
interface BaseFragment {

    var delegate: FragmentCommunication

    fun getViewModels()

    fun observe()
}
