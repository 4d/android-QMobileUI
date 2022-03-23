package com.qmobile.qmobileui.action

import com.qmobile.qmobiledatasync.app.BaseApp
import java.util.Date

const val MILLISECONDS_IN_SECONDS = 1000
fun shouldShowActionError(): Boolean {
    var sharedPreferencesHolder = BaseApp.sharedPreferencesHolder
    val lastDisplayErrorTime = Date(sharedPreferencesHolder.lastTimeActionErrorDisplayed).time
    val diffInSeconds = (Date().time - lastDisplayErrorTime) / MILLISECONDS_IN_SECONDS

    if (diffInSeconds >= SECONDS_IN_MINUTE) {
        // reset lastTimeActionErrorDisplayed with the currentTime
        sharedPreferencesHolder.lastTimeActionErrorDisplayed = Date().time
        return true
    }
    return false
}
