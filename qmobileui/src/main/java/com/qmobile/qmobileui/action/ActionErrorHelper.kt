package com.qmobile.qmobileui.action

import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobiledatasync.app.MILLISECONDS_IN_SECONDS
import java.util.Date


fun shouldShowActionError(): Boolean {
    var sharedPreferencesHolder = BaseApp.sharedPreferencesHolder
    val lastDisplayErrorTime =
        Date(sharedPreferencesHolder.lastTimeActionErrorDisplayed).time
    val diffInSeconds = (Date().time - lastDisplayErrorTime) / MILLISECONDS_IN_SECONDS

    if (diffInSeconds >= com.qmobile.qmobiledatasync.app.SECONDS_IN_MINUTE) {
        // reset lastTimeActionErrorDisplayed with the currentTime
        sharedPreferencesHolder.lastTimeActionErrorDisplayed = Date().time
        return true
    }
    return false
}