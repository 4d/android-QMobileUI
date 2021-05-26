/*
 * Created by Quentin Marciset on 22/1/2021.
 * 4D SAS
 * Copyright (c) 2021 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.model

import com.qmobile.qmobileui.utils.FieldMapping
import org.json.JSONObject

data class AppUtilities(
    var initialGlobalStamp: Int,
    var guestLogin: Boolean,
    var remoteUrl: String,
    var teams: JSONObject,
    var queryJson: JSONObject,
    var searchField: JSONObject,
    var sdkVersion: String,
    var logLevel: Int,
    var dumpedTables: String,
    var relationAvailable: Boolean = true,
    var customFormatters: Map<String, Map<String, FieldMapping>> // Map<TableName, Map<FieldName, FieldMapping>>
)
