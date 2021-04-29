/*
 * Created by Quentin Marciset on 22/1/2021.
 * 4D SAS
 * Copyright (c) 2021 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.utils

import android.content.Context
import com.qmobile.qmobileui.model.AppUtilities
import com.qmobile.qmobileui.model.DeviceUtility
import com.qmobile.qmobileui.utils.converter.NumberToWordFormatter

object QMobileUiUtil {
    lateinit var appUtilities: AppUtilities
    private lateinit var bridgeUtility: BridgeUtility
    lateinit var deviceUtility: DeviceUtility
    private val numberToWord = NumberToWordFormatter
    /*var queryHolder: QueryHolder =
        QueryHolder(null)*/

    fun builder(context: Context) { // builder should be initialised
        bridgeUtility = BridgeUtility(context)
        appUtilities = bridgeUtility.getAppUtil()
        deviceUtility = DeviceUtilitiesGenerator(context).getDeviceUtility
    }

    var listAllFilesInAsset =
        { path: String -> bridgeUtility.listAssetFiles(path) } // Helper Utility Function

    var WordFormatter =
        { number: String -> numberToWord.convertNumberToWord(number) } // convert number To literal word

    /*fun setQuery(sqlQuery: SimpleSQLiteQuery, isSearchActive: Boolean) { // this part has tobe added
        queryHolder = QueryHolder(
            sqlQuery,
            isSearchActive = isSearchActive
        )
    }*/
}
