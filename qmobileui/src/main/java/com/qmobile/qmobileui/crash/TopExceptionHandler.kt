/*
 * Created by qmarciset on 13/12/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.crash

import android.content.Context
import com.qmobile.qmobiledatasync.log.LogFileHelper.cleanOlderCrashLogs
import com.qmobile.qmobiledatasync.log.LogFileHelper.createLogFile

class TopExceptionHandler(private val context: Context) : Thread.UncaughtExceptionHandler {

    private val defaultUEH: Thread.UncaughtExceptionHandler? = Thread.getDefaultUncaughtExceptionHandler()

    override fun uncaughtException(t: Thread, e: Throwable) {
        var array = e.stackTrace
        var report = """
            $e
            
            
        """.trimIndent()
        report += "--------- Stack trace ---------\n\n"
        for (i in array.indices) {
            report += """    ${array[i]}
"""
        }
        report += "-------------------------------\n\n"

        // If the exception was thrown in a background thread inside
        // AsyncTask, then the actual exception can be found with getCause
        report += "--------- Cause ---------\n\n"
        val cause = e.cause
        if (cause != null) {
            report += """
                $cause
                
                
            """.trimIndent()
            array = cause.stackTrace
            for (i in array.indices) {
                report += """    ${array[i]}
"""
            }
        }
        report += "-------------------------------\n\n"
        cleanOlderCrashLogs(context)
        createLogFile(context, report)
        defaultUEH?.uncaughtException(t, e)
    }
}
