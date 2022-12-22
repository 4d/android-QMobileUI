/*
 * Created by qmarciset on 21/12/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.crash

import android.content.Context
import com.qmobile.qmobiledatasync.log.LogFileHelper
import org.jetbrains.annotations.TestOnly
import timber.log.Timber

/** Handle Signal crashes (ex: SIGSEGV) and write basic log after it */
class SignalHandler(private val context: Context) {

    private var isSignalHandlerInitialized = false

    fun initSignalHandler() {
        if (!isSignalHandlerInitialized) {
            val logsDirPath = LogFileHelper.getLogsDirectoryFromPathOrFallback(context.filesDir.absolutePath)
            nativeCreateLogFile(logsDirPath, LogFileHelper.sigCrashLogFileName)
            nativeRegisterSignalHandler()
            isSignalHandlerInitialized = true
//            crashAndGetExceptionMessage(null)
        } else {
            Timber.e("SignalHandler already initialized")
        }
    }

    fun clearSignalHandler() {
        if (isSignalHandlerInitialized) {
            nativeUnregisterSignalHandler()
        } else {
            Timber.e("SignalHandler not initialized")
        }
    }

    /**
     * C++ functions
     */
    private external fun nativeRegisterSignalHandler()
    private external fun nativeUnregisterSignalHandler()
    private external fun nativeCreateLogFile(logsDirPath: String, logFileName: String)

    @Suppress("unused")
    private external fun crashAndGetExceptionMessage(exception: Exception?)
}
