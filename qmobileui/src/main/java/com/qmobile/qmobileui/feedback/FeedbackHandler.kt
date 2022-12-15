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
import com.qmobile.qmobiledatasync.viewmodel.FeedbackViewModel
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.ui.getStatusBarHeight
import com.qmobile.qmobileui.ui.setOnSingleClickListener
import org.json.JSONObject

class FeedbackHandler(fragment: Fragment, private val feedbackViewModel: FeedbackViewModel) {

    companion object {
        const val numberClickToTrigger = 5
    }

    enum class Type {
        TALK_TO_US, SUGGEST_IMPROVEMENT, SHOW_CURRENT_LOG, REPORT_A_PROBLEM, REPORT_PREVIOUS_CRASH
    }

    init {
        fragment.requireContext().apply {
            val bottomSheetDialog = BottomSheetDialog(this)
            bottomSheetDialog.setContentView(R.layout.feedback_options)
            bottomSheetDialog.findViewById<View>(R.id.design_bottom_sheet)?.let {
                val marginHeight =
                    resources.displayMetrics.heightPixels - getStatusBarHeight(this)
                BottomSheetBehavior.from(it).peekHeight = marginHeight
            }
            bottomSheetDialog.findViewById<TextView>(R.id.talk_to_us)?.setOnSingleClickListener {
                showForm(Type.TALK_TO_US)
            }
            bottomSheetDialog.findViewById<TextView>(R.id.suggest_improvement)?.setOnSingleClickListener {
                showForm(Type.SUGGEST_IMPROVEMENT)
            }
            bottomSheetDialog.findViewById<TextView>(R.id.show_current_log)?.setOnSingleClickListener {
                showForm(Type.SHOW_CURRENT_LOG)
            }
            bottomSheetDialog.findViewById<TextView>(R.id.report_a_problem)?.setOnSingleClickListener {
                showForm(Type.REPORT_A_PROBLEM)
            }
            bottomSheetDialog.findViewById<TextView>(R.id.report_previous_crash)?.setOnSingleClickListener {
                showForm(Type.REPORT_PREVIOUS_CRASH)
            }
            bottomSheetDialog.show()
        }
    }

    private fun showForm(type: Type) {
        when (type) {
            Type.TALK_TO_US -> {}
            Type.SUGGEST_IMPROVEMENT -> {}
            Type.SHOW_CURRENT_LOG -> {}
            Type.REPORT_A_PROBLEM -> {}
            Type.REPORT_PREVIOUS_CRASH -> {}
        }
    }

    private fun sendFeedback(feedback: JSONObject) {
        feedbackViewModel.sendFeedback(feedback) { isSuccess ->
        }
    }
}
