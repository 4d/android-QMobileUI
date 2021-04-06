/*
 * Created by Quentin Marciset on 22/1/2021.
 * 4D SAS
 * Copyright (c) 2021 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.model

import org.json.JSONObject

data class AppUtilities(
    var globalStamp: Int,
    var guestLogin: Boolean,
    var embeddedData: Boolean,
    var remoteUrl: String,
    var teams: JSONObject,
    var queryJson: JSONObject,
    var searchField: JSONObject,
    var sdkVersion: String,
    var logLevel: Int
)
