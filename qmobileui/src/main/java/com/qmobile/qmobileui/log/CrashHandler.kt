/*
 * Created by qmarciset on 13/12/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.log

import android.content.Context
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobiledatasync.viewmodel.FeedbackViewModel
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.activity.loginactivity.LoginActivity
import com.qmobile.qmobileui.activity.mainactivity.MainActivity
import com.qmobile.qmobileui.log.LogFileHelper.compress
import com.qmobile.qmobileui.log.LogFileHelper.findCrashLogFile
import java.io.File

class CrashHandler(private val context: Context, private val feedbackViewModel: FeedbackViewModel) {

    init {
        if (BaseApp.sharedPreferencesHolder.displayCrashDialog && findCrashLogFile(context)?.exists() == true) {
            when {
                context is LoginActivity && !BaseApp.runtimeDataHolder.guestLogin -> displayCrashDialog()
                context is MainActivity && BaseApp.runtimeDataHolder.guestLogin -> displayCrashDialog()
            }
        }
    }

    private fun displayCrashDialog() {
        context.apply {
            MaterialAlertDialogBuilder(this)
                .setTitle(resources.getString(R.string.crash_log_dialog_title))
                .setMessage(resources.getString(R.string.crash_log_dialog_message))
                .setPositiveButton(resources.getString(R.string.crash_log_dialog_send)) { _, _ ->
                    sendCrashReport()
                    BaseApp.sharedPreferencesHolder.displayCrashDialog = false
                }
                .setNegativeButton(resources.getString(R.string.crash_log_dialog_deny)) { _, _ ->
                    BaseApp.sharedPreferencesHolder.displayCrashDialog = false
                }
                .setNeutralButton(resources.getString(R.string.crash_log_dialog_save_for_later), null)
                .show()
        }
    }

    private fun sendCrashReport() {
        findCrashLogFile(context)?.let { logFile ->
            compress(logFile)?.let { zipFile ->
                feedbackViewModel.sendCrashReport(zipFile) { isSuccess ->
                    if (isSuccess) {
                        deleteFiles(logFile, zipFile)
                    }
                    displayCrashSent(isSuccess)
                }
            }
        }
    }

    private fun deleteFiles(logFile: File, zipFile: File) {
        logFile.delete()
        zipFile.delete()
    }

    private fun displayCrashSent(isSuccess: Boolean) {
        context.apply {
            val message = if (isSuccess) {
                resources.getString(R.string.crash_log_dialog_response_message_success, "ticket")
            } else {
                resources.getString(R.string.crash_log_dialog_response_message_fail)
            }
            MaterialAlertDialogBuilder(this)
                .setTitle(resources.getString(R.string.crash_log_dialog_response_title))
                .setMessage(message)
                .setPositiveButton(resources.getString(R.string.crash_log_dialog_response_action), null)
                .show()
        }
    }
}
