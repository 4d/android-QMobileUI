/*
 * Created by qmarciset on 4/2/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui

import com.qmobile.qmobileapi.network.FeedbackApiService
import com.qmobile.qmobileui.log.CrashHandler

interface FeedbackActivity {

    val feedbackApiService: FeedbackApiService

    val crashHandler: CrashHandler
}
