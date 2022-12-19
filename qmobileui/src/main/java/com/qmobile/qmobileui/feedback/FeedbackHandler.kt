/*
 * Created by qmarciset on 14/12/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.feedback

import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobiledatasync.utils.FeedbackType
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.log.LogFileHelper.findCrashLogFile
import com.qmobile.qmobileui.ui.getStatusBarHeight
import com.qmobile.qmobileui.ui.setOnSingleClickListener
import com.qmobile.qmobileui.ui.setSharedAxisYExitTransition
import java.io.File

class FeedbackHandler(private val fragment: Fragment) {

    private val bottomSheetDialog: BottomSheetDialog
    private var currentCrashLog: File? = null

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
        println()
//        MaterialAlertDialogBuilder(fragment.requireContext())
//            .setMessage(log)
//            .setPositiveButton(fragment.resources.getString(R.string.crash_log_dialog_response_action), null)
//            .show()
    }

    private fun reportPreviousCrash() {
        println()
//        findCrashLogFile(fragment.requireContext())?.let { logFile ->
//            compress(logFile)?.let { zipFile ->
//                checkNetwork(logFile, zipFile)
//            }
//        }
    }

    private fun openFeedbackFragment(type: FeedbackType) {
        fragment.activity?.apply {
            fragment.setSharedAxisYExitTransition()
            BaseApp.genericNavigationResolver.navigateToFeedback(this, type)
        }
    }
}
