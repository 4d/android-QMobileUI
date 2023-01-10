/*
 * Created by qmarciset on 13/12/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.crash

import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobiledatasync.log.LogFileHelper.cleanOlderCrashLogs
import com.qmobile.qmobiledatasync.log.LogFileHelper.compress
import com.qmobile.qmobiledatasync.log.LogFileHelper.findCrashLogFile
import com.qmobile.qmobiledatasync.toast.ToastMessage
import com.qmobile.qmobiledatasync.viewmodel.FeedbackViewModel
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.activity.BaseActivity
import com.qmobile.qmobileui.activity.loginactivity.LoginActivity
import com.qmobile.qmobileui.activity.mainactivity.MainActivity
import com.qmobile.qmobileui.network.NetworkChecker
import com.qmobile.qmobileui.ui.SnackbarHelper
import java.io.File

class CrashHandler(val activity: BaseActivity, val feedbackViewModel: FeedbackViewModel) {

    init {
        when {
            activity is LoginActivity && !BaseApp.runtimeDataHolder.guestLogin -> displayCrashDialog()
            activity is MainActivity -> displayCrashDialog()
        }
    }

    private fun displayCrashDialog() {
        val crashLog = findCrashLogFile(activity)
        if (crashLog != null) {
            if (BaseApp.sharedPreferencesHolder.crashLogSavedForLater != crashLog.name) {
                BaseApp.sharedPreferencesHolder.crashLogSavedForLater = crashLog.name
                activity.apply {
                    MaterialAlertDialogBuilder(this)
                        .setTitle(getString(R.string.crash_log_dialog_title))
                        .setMessage(getString(R.string.crash_log_dialog_message))
                        .setPositiveButton(getString(R.string.crash_log_dialog_send)) { _, _ ->
                            proceedFile(crashLog)
                        }
                        .setNegativeButton(getString(R.string.crash_log_dialog_deny)) { _, _ ->
                            cleanOlderCrashLogs(this)
                        }
                        /*.setNeutralButton(getString(R.string.crash_log_dialog_save_for_later)) { _, _ ->
                            BaseApp.sharedPreferencesHolder.crashLogSavedForLater = crashLog.name
                        }*/
                        .show()
                }
            }
        }
    }

    fun proceedFile(file: File) {
        compress(file)?.let { zipFile ->
            checkNetworkAndSend(zipFile, file.name)
        }
    }

    private fun checkNetworkAndSend(zipFile: File, fileName: String) {
        activity.queryNetwork(
            object : NetworkChecker {
                override fun onServerAccessible() {
                    sendCrashReport(zipFile, fileName)
                }

                override fun onServerInaccessible() {
                    BaseApp.sharedPreferencesHolder.crashLogSavedForLater = fileName
                    SnackbarHelper.show(
                        activity,
                        activity.getString(R.string.server_not_accessible),
                        ToastMessage.Type.WARNING
                    )
                }

                override fun onNoInternet() {
                    BaseApp.sharedPreferencesHolder.crashLogSavedForLater = fileName
                    SnackbarHelper.show(activity, activity.getString(R.string.no_internet), ToastMessage.Type.WARNING)
                }
            },
            feedbackServer = true
        )
    }

    private fun sendCrashReport(zipFile: File, fileName: String) {
        feedbackViewModel.sendCrash(zipFile) { isSuccess ->
            if (isSuccess) {
                cleanOlderCrashLogs(activity)
            } else {
                BaseApp.sharedPreferencesHolder.crashLogSavedForLater = fileName
            }
            displayCrashSent(isSuccess)
        }
    }

    private fun displayCrashSent(isSuccess: Boolean) {
        activity.apply {
            val message = if (isSuccess) {
                getString(R.string.crash_log_dialog_response_message_success, "ticket")
            } else {
                getString(R.string.crash_log_dialog_response_message_fail)
            }
            MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.crash_log_dialog_response_title))
                .setMessage(message)
                .setPositiveButton(getString(R.string.crash_log_dialog_response_action), null)
                .show()
        }
    }
}
