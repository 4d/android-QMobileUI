/*
 * Created by qmarciset on 13/12/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.log

import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobiledatasync.toast.ToastMessage
import com.qmobile.qmobiledatasync.utils.FeedbackType
import com.qmobile.qmobiledatasync.viewmodel.FeedbackViewModel
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.activity.BaseActivity
import com.qmobile.qmobileui.activity.loginactivity.LoginActivity
import com.qmobile.qmobileui.activity.mainactivity.MainActivity
import com.qmobile.qmobileui.log.LogFileHelper.compress
import com.qmobile.qmobileui.log.LogFileHelper.findCrashLogFile
import com.qmobile.qmobileui.network.NetworkChecker
import com.qmobile.qmobileui.ui.SnackbarHelper
import org.json.JSONObject
import java.io.File

class CrashHandler(private val activity: BaseActivity, private val feedbackViewModel: FeedbackViewModel) {

    init {
        if (BaseApp.sharedPreferencesHolder.displayCrashDialog && findCrashLogFile(activity)?.exists() == true) {
            when {
                activity is LoginActivity && !BaseApp.runtimeDataHolder.guestLogin -> displayCrashDialog()
                activity is MainActivity && BaseApp.runtimeDataHolder.guestLogin -> displayCrashDialog()
            }
        }
    }

    private fun displayCrashDialog() {
        activity.apply {
            MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.crash_log_dialog_title))
                .setMessage(getString(R.string.crash_log_dialog_message))
                .setPositiveButton(getString(R.string.crash_log_dialog_send)) { _, _ ->
                    proceedFile()
                    BaseApp.sharedPreferencesHolder.displayCrashDialog = false
                }
                .setNegativeButton(getString(R.string.crash_log_dialog_deny)) { _, _ ->
                    BaseApp.sharedPreferencesHolder.displayCrashDialog = false
                }
                .setNeutralButton(getString(R.string.crash_log_dialog_save_for_later), null)
                .show()
        }
    }

    private fun proceedFile() {
        findCrashLogFile(activity)?.let { logFile ->
            compress(logFile)?.let { zipFile ->
                checkNetwork(logFile, zipFile)
            }
        }
    }

    private fun checkNetwork(logFile: File, zipFile: File) {
        activity.queryNetwork(
            object : NetworkChecker {
                override fun onServerAccessible() {
                    sendCrashReport(logFile, zipFile)
                }

                override fun onServerInaccessible() {
                    SnackbarHelper.show(
                        activity,
                        activity.getString(R.string.server_not_accessible),
                        ToastMessage.Type.WARNING
                    )
                }

                override fun onNoInternet() {
                    SnackbarHelper.show(activity, activity.getString(R.string.no_internet), ToastMessage.Type.WARNING)
                }
            },
            feedbackServer = true
        )
    }

    private fun buildRequestJson(): JSONObject {
        return JSONObject().apply {
            put("type", FeedbackType.REPORT_PREVIOUS_CRASH.key)
            put("fileName", "")
            put("sendDate", LogFileHelper.getCurrentDateTimeLogFormat())
            put("isCrash", "1")
        }
    }

    private fun sendCrashReport(logFile: File, zipFile: File) {
        feedbackViewModel.sendCrashReport(zipFile) { isSuccess ->
            if (isSuccess) {
                deleteFiles(logFile, zipFile)
            }
            displayCrashSent(isSuccess)
        }
    }

    private fun deleteFiles(logFile: File, zipFile: File) {
        logFile.delete()
        zipFile.delete()
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
