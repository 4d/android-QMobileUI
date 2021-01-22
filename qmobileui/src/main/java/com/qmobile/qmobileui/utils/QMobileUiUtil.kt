/*
 * Created by Quentin Marciset on 22/1/2021.
 * 4D SAS
 * Copyright (c) 2021 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.utils

import android.content.Context
import com.qmobile.qmobileui.model.AppUtilities

object QMobileUiUtil {
    private lateinit var bridgeUtility: BridgeUtility
    lateinit var appUtilities: AppUtilities
    fun builder(context: Context) { // builder should be initialised
        bridgeUtility = BridgeUtility(context)
        appUtilities = bridgeUtility.getAppUtil()
    }

    var listAllFilesInAsset = {path: String -> bridgeUtility.listAssetFiles(path)} // Helper Utility Function
}