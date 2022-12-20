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
import androidx.fragment.app.Fragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.qmobile.qmobileapi.network.ApiClient
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobiledatasync.log.CurrentLogHelper.getCurrentLogText
import com.qmobile.qmobiledatasync.log.LogFileHelper.findCrashLogFile
import com.qmobile.qmobiledatasync.log.LogLevel
import com.qmobile.qmobiledatasync.log.LogLevelController
import com.qmobile.qmobiledatasync.utils.FeedbackType
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.log.CrashHandler
import com.qmobile.qmobileui.ui.SnackbarHelper
import com.qmobile.qmobileui.ui.getStatusBarHeight
import com.qmobile.qmobileui.ui.setOnSingleClickListener
import com.qmobile.qmobileui.ui.setSharedAxisYExitTransition
import java.io.File

class FeedbackHandler(private val fragment: Fragment, private val crashHandler: CrashHandler) {

    private val bottomSheetDialog: BottomSheetDialog
    private var currentCrashLog: File? = null
    private lateinit var currentLogDialog: AlertDialog

    init {
        fragment.requireContext().apply {
            bottomSheetDialog = BottomSheetDialog(this)
            bottomSheetDialog.setContentView(R.layout.feedback_options)
            bottomSheetDialog.findViewById<View>(R.id.design_bottom_sheet)?.let {
                val marginHeight =
                    resources.displayMetrics.heightPixels - getStatusBarHeight(this)
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

            currentCrashLog = findCrashLogFile(this)

            if (currentCrashLog?.exists() != true) {
                bottomSheetDialog.findViewById<TextView>(R.id.report_previous_crash)?.visibility = View.GONE
            }

            bottomSheetDialog.show()
        }
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
        fragment.requireContext().apply {
            currentLogDialog = MaterialAlertDialogBuilder(this)
                .setMessage(getCurrentLogText(this))
                .setPositiveButton(resources.getString(R.string.feedback_show_current_log_action_dismiss), null)
                .setNeutralButton(resources.getString(R.string.feedback_show_current_log_action_send)) { _, _ ->
                    openFeedbackFragment(FeedbackType.REPORT_A_PROBLEM)
                }
                .setNegativeButton(resources.getString(R.string.feedback_show_current_log_action_change_level), null)
                .create().apply {
                    setOnShowListener {
                        getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener { view ->
                            openLogLevelMenu(view)
                        }
                    }
                }
            currentLogDialog.show()
        }
    }

    private fun openLogLevelMenu(view: View) {
        val popup = PopupMenu(fragment.requireContext(), view)
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
        SnackbarHelper.show(fragment.activity, fragment.resources.getString(R.string.log_level_changed, title))
    }

    private fun reportPreviousCrash() {
        crashHandler.proceedFile()
    }

    private fun openFeedbackFragment(type: FeedbackType) {
        fragment.activity?.apply {
            fragment.setSharedAxisYExitTransition()
            BaseApp.genericNavigationResolver.navigateToFeedback(this, type)
        }
    }
}
