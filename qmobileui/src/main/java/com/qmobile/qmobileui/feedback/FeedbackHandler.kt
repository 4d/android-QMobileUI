/*
 * Created by qmarciset on 14/12/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.feedback

import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.qmobile.qmobileapi.network.ApiClient
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobiledatasync.log.CurrentLogHelper
import com.qmobile.qmobiledatasync.log.CurrentLogHelper.getCurrentLogText
import com.qmobile.qmobiledatasync.log.LogFileHelper.findCrashLogFile
import com.qmobile.qmobiledatasync.log.LogLevel
import com.qmobile.qmobiledatasync.log.LogLevelController
import com.qmobile.qmobiledatasync.toast.ToastMessage
import com.qmobile.qmobiledatasync.utils.FeedbackType
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.activity.BaseActivity
import com.qmobile.qmobileui.network.NetworkChecker
import com.qmobile.qmobileui.ui.SnackbarHelper
import com.qmobile.qmobileui.ui.getStatusBarHeight
import com.qmobile.qmobileui.ui.setOnSingleClickListener
import java.io.File

class FeedbackHandler(private val activity: BaseActivity) {

    private val bottomSheetDialog: BottomSheetDialog = BottomSheetDialog(activity)
    private var currentCrashLog: File? = null
    private lateinit var currentLogDialog: AlertDialog

    init {
        bottomSheetDialog.setContentView(R.layout.feedback_options)
        bottomSheetDialog.findViewById<View>(R.id.design_bottom_sheet)?.let {
            val marginHeight =
                activity.resources.displayMetrics.heightPixels - getStatusBarHeight(activity)
            BottomSheetBehavior.from(it).peekHeight = marginHeight
        }
        bottomSheetDialog.findViewById<TextView>(R.id.talk_to_us)?.setOnSingleClickListener {
            showForm(FeedbackType.TALK_TO_US)
        }
        bottomSheetDialog.findViewById<TextView>(R.id.suggest_improvement)?.setOnSingleClickListener {
            showForm(FeedbackType.SUGGEST_IMPROVEMENT)
        }
        bottomSheetDialog.findViewById<TextView>(R.id.show_current_log)?.setOnSingleClickListener {
            showForm(FeedbackType.SHOW_CURRENT_LOG)
        }
        bottomSheetDialog.findViewById<TextView>(R.id.report_a_problem)?.setOnSingleClickListener {
            showForm(FeedbackType.REPORT_A_PROBLEM)
        }
        bottomSheetDialog.findViewById<TextView>(R.id.report_previous_crash)?.setOnSingleClickListener {
            showForm(FeedbackType.REPORT_PREVIOUS_CRASH)
        }

        currentCrashLog = findCrashLogFile(activity)

        if (currentCrashLog == null || !BaseApp.runtimeDataHolder.crashLogs) {
            bottomSheetDialog.findViewById<TextView>(R.id.report_previous_crash)?.visibility = View.GONE
        }

        bottomSheetDialog.show()
    }

    private fun showForm(type: FeedbackType) {
        bottomSheetDialog.dismiss()
        when (type) {
            FeedbackType.SHOW_CURRENT_LOG -> showCurrentLog()
            FeedbackType.REPORT_PREVIOUS_CRASH -> reportPreviousCrash()
            else -> openFeedbackFragment(type)
        }
    }

    private fun showCurrentLog() {
        val currentLogText = getCurrentLogText(activity)
        currentLogDialog = MaterialAlertDialogBuilder(activity)
            .setMessage(currentLogText)
            .setPositiveButton(activity.getString(R.string.feedback_show_current_log_action_dismiss), null)
            .setNeutralButton(activity.getString(R.string.feedback_show_current_log_action_send)) { _, _ ->
                checkNetworkAndSend()
            }
            .setNegativeButton(activity.getString(R.string.feedback_show_current_log_action_change_level), null)
            .create().apply {
                setOnShowListener {
                    getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener { view ->
                        openLogLevelMenu(view)
                    }
                }
            }
        currentLogDialog.show()
    }

    private fun openLogLevelMenu(view: View) {
        val popup = PopupMenu(activity, view)
        popup.menuInflater.inflate(R.menu.menu_log_level, popup.menu)

        popup.setOnMenuItemClickListener { menuItem: MenuItem ->
            val level = when (menuItem.itemId) {
                R.id.log_verbose -> LogLevel.VERBOSE.level
                R.id.log_debug -> LogLevel.DEBUG.level
                R.id.log_info -> LogLevel.INFO.level
                R.id.log_warn -> LogLevel.WARN.level
                R.id.log_error -> LogLevel.ERROR.level
                R.id.log_assert -> LogLevel.ASSERT.level
                R.id.log_none -> LogLevel.NONE.level
                else -> LogLevel.NONE.level
            }
            currentLogDialog.dismiss()
            setLogLevel(level, menuItem.title.toString())
            true
        }
        popup.show()
    }

    private fun setLogLevel(level: Int, title: String) {
        BaseApp.runtimeDataHolder.logLevel = level
        LogLevelController.level = level
        ApiClient.setLogBody(level <= Log.VERBOSE)
        SnackbarHelper.show(activity, activity.getString(R.string.log_level_changed, title))
    }

    private fun reportPreviousCrash() {
        currentCrashLog?.let { activity.crashHandler.proceedFile(it) }
    }

    private fun checkNetworkAndSend() {
        activity.queryNetwork(
            object : NetworkChecker {
                override fun onServerAccessible() {
                    sendCurrentLog()
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

    private fun sendCurrentLog() {
        val currentLogZip = CurrentLogHelper.getCurrentLogZip(activity)
        activity.crashHandler.feedbackViewModel.sendCurrentLogs(currentLogZip) { isSuccess ->
            if (isSuccess) {
                SnackbarHelper.show(
                    activity,
                    activity.getString(R.string.feedback_send_current_logs),
                    ToastMessage.Type.SUCCESS
                )
            }
        }
    }

    private fun openFeedbackFragment(type: FeedbackType) {
        BaseApp.genericNavigationResolver.navigateToFeedback(activity, type)
    }
}
