/*
 * Created by qmarciset on 16/12/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.feedback

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.core.view.MenuProvider
import com.qmobile.qmobiledatasync.log.LogFileHelper.getCurrentDateTimeLogFormat
import com.qmobile.qmobiledatasync.toast.ToastMessage
import com.qmobile.qmobiledatasync.utils.FeedbackType
import com.qmobile.qmobiledatasync.viewmodel.FeedbackViewModel
import com.qmobile.qmobiledatasync.viewmodel.factory.getFeedbackViewModel
import com.qmobile.qmobileui.BaseFragment
import com.qmobile.qmobileui.FeedbackActivity
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.activity.mainactivity.MainActivity
import com.qmobile.qmobileui.databinding.FragmentFeedbackBinding
import com.qmobile.qmobileui.network.NetworkChecker
import com.qmobile.qmobileui.ui.SnackbarHelper
import com.qmobile.qmobileui.ui.setSharedAxisYEnterTransition
import com.qmobile.qmobileui.ui.setupToolbarTitle
import com.qmobile.qmobileui.utils.serializable
import org.json.JSONObject

class FeedbackFragment : BaseFragment(), MenuProvider {

    private var _binding: FragmentFeedbackBinding? = null
    private val binding get() = _binding!!

    private lateinit var feedbackViewModel: FeedbackViewModel

    private lateinit var type: FeedbackType
    private lateinit var feedbackActivity: FeedbackActivity

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is FeedbackActivity) {
            feedbackActivity = context
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.serializable<FeedbackType>("type")?.let { type = it }
        setSharedAxisYEnterTransition()

        activity?.onBackPressedDispatcher?.addCallback {
            (activity as? MainActivity?)?.navController?.navigateUp()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        activity?.setupToolbarTitle(getTitle())

        _binding = FragmentFeedbackBinding.inflate(inflater, container, false).apply {
            lifecycleOwner = viewLifecycleOwner
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.feedbackBodyInput.hint = getBodyHint()

        initMenuProvider()

        if (type != FeedbackType.REPORT_A_PROBLEM) {
            binding.logAttachment.visibility = View.GONE
        }

        feedbackViewModel = getFeedbackViewModel(this, feedbackActivity.feedbackApiService)
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.menu_feedback, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        if (menuItem.itemId == R.id.send) {
            checkNetwork()
        }
        return false
    }

    private fun checkNetwork() {
        delegate.checkNetwork(
            object : NetworkChecker {
                override fun onServerAccessible() {
                    sendFeedback()
                }

                override fun onServerInaccessible() {
                    SnackbarHelper.show(
                        activity,
                        activity?.getString(R.string.server_not_accessible),
                        ToastMessage.Type.WARNING
                    )
                }

                override fun onNoInternet() {
                    SnackbarHelper.show(activity, activity?.getString(R.string.no_internet), ToastMessage.Type.WARNING)
                }
            },
            feedbackServer = true
        )
    }

    private fun sendFeedback() {
        feedbackViewModel.sendFeedback(buildRequestJson()) { isSuccess ->
            if (isSuccess) {
                SnackbarHelper.show(
                    activity,
                    activity?.getString(R.string.feedback_send_success),
                    ToastMessage.Type.SUCCESS
                )
            }
        }
    }

    private fun buildRequestJson(): JSONObject {
        return JSONObject().apply {
            put("email", binding.emailEdit.toString())
            put("summary", binding.feedbackBodyEdit.toString())
            put("type", type.key)
            put("fileName", "")
            put("sendDate", getCurrentDateTimeLogFormat())
            put("isCrash", "0")
        }
    }

    private fun getTitle(): String {
        return when (type) {
            FeedbackType.TALK_TO_US -> getString(R.string.feedback_talk_to_us)
            FeedbackType.SUGGEST_IMPROVEMENT -> getString(R.string.feedback_suggest_improvement)
            FeedbackType.SHOW_CURRENT_LOG -> ""
            FeedbackType.REPORT_A_PROBLEM -> getString(R.string.feedback_report_a_problem)
            FeedbackType.REPORT_PREVIOUS_CRASH -> ""
        }
    }

    private fun getBodyHint(): String {
        return when (type) {
            FeedbackType.TALK_TO_US -> getString(R.string.feedback_talk_to_us_body)
            FeedbackType.SUGGEST_IMPROVEMENT -> getString(R.string.feedback_suggest_improvement_body)
            FeedbackType.SHOW_CURRENT_LOG -> ""
            FeedbackType.REPORT_A_PROBLEM -> getString(R.string.feedback_report_a_problem_body)
            FeedbackType.REPORT_PREVIOUS_CRASH -> ""
        }
    }
}
