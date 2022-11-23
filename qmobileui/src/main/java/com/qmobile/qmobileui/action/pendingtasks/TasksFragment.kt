/*
 * Created by qmarciset on 3/6/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.action.pendingtasks

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.divider.MaterialDividerItemDecoration
import com.qmobile.qmobiledatastore.dao.ActionTask
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobileui.ActionActivity
import com.qmobile.qmobileui.BaseFragment
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.action.utils.ActionHelper
import com.qmobile.qmobileui.binding.px
import com.qmobile.qmobileui.databinding.FragmentActionTasksBinding
import com.qmobile.qmobileui.network.NetworkChecker
import com.qmobile.qmobileui.ui.SnackbarHelper
import com.qmobile.qmobileui.ui.SnackbarHelper.UNDO_ACTION_DURATION
import com.qmobile.qmobileui.ui.setFadeThroughEnterTransition
import com.qmobile.qmobileui.ui.setOnSingleClickListener
import com.qmobile.qmobileui.ui.setSharedAxisXEnterTransition
import com.qmobile.qmobileui.ui.setSharedAxisXExitTransition
import com.qmobile.qmobileui.ui.setupToolbarTitle
import java.util.*

class TasksFragment : BaseFragment(), NetworkChecker {

    private var _binding: FragmentActionTasksBinding? = null
    private val binding get() = _binding!!
    internal lateinit var pendingAdapter: TaskListAdapter
    internal lateinit var completedAdapter: TaskListAdapter

    internal lateinit var actionActivity: ActionActivity

    internal var tableName: String = ""
    internal var currentItemId: String = ""

    private lateinit var noInternetString: String
    private lateinit var serverAccessibleString: String
    private lateinit var serverNotAccessibleString: String
    private lateinit var checkingString: String

    private var serverAccessibleDrawable: Drawable? = null
    private var serverNotAccessibleDrawable: Drawable? = null
    private var noInternetDrawable: Drawable? = null

    companion object {
        internal const val MAX_PENDING_TASKS = 10
        private const val DIVIDER_INSET_START = 56
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is ActionActivity) {
            actionActivity = context
        }
        noInternetString = resources.getString(R.string.no_internet)
        serverAccessibleString = resources.getString(R.string.server_accessible)
        serverNotAccessibleString = resources.getString(R.string.server_not_accessible)
        checkingString = resources.getString(R.string.remote_url_checking)
        serverAccessibleDrawable = ContextCompat.getDrawable(context, R.drawable.domain)
        serverNotAccessibleDrawable = ContextCompat.getDrawable(context, R.drawable.domain_disabled)
        noInternetDrawable = ContextCompat.getDrawable(context, R.drawable.signal_disconnected)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.getString("tableName")?.let { tableName = it }
        arguments?.getString("currentItemId")?.let { currentItemId = it }
        if (tableName.isEmpty()) { // from Settings fragment
            setSharedAxisXEnterTransition()
        } else {
            setFadeThroughEnterTransition()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        activity?.setupToolbarTitle(resources.getString(R.string.pending_task_navbar_title))

        _binding = FragmentActionTasksBinding.inflate(inflater, container, false).apply {
            lifecycleOwner = viewLifecycleOwner
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.serverStatus.setOnSingleClickListener {
            onServerStatusClick()
        }
        initSwipeToDelete()
        setupAdapters()
        initRecyclerViews()
        initOnRefreshListener()
        TasksFragmentObserver(this).initObservers()
        delegate.checkNetwork(this)
    }

    /**
     * Initialize recyclerView
     */
    private fun initRecyclerViews() {
        binding.tasksLinear.setPadding(0, 0, 0, getPaddingBottom())
        val divider = MaterialDividerItemDecoration(requireContext(), LinearLayoutManager.VERTICAL)
        divider.isLastItemDecorated = false
        divider.dividerInsetStart = DIVIDER_INSET_START.px
        divider.dividerColor = ContextCompat.getColor(requireContext(), R.color.divider_color)

        binding.pendingRv.addItemDecoration(divider)
        binding.pendingRv.layoutManager = LinearLayoutManager(activity, RecyclerView.VERTICAL, false)
        binding.pendingRv.adapter = pendingAdapter

        binding.completedRv.addItemDecoration(divider)
        binding.completedRv.layoutManager = LinearLayoutManager(activity, RecyclerView.VERTICAL, false)
        binding.completedRv.adapter = completedAdapter
    }

    /**
     * Initialize Swipe to delete
     */
    private fun initSwipeToDelete() {
        val swipeHandler = object : SwipeToDeleteCallback(requireContext()) {
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                pendingAdapter.getItemByPosition(viewHolder.bindingAdapterPosition)?.let { actionTask ->
                    removeTask(actionTask)
                }
            }
        }
        val itemTouchHelper = ItemTouchHelper(swipeHandler)
        itemTouchHelper.attachToRecyclerView(binding.pendingRv)
    }

    private fun removeTask(actionTask: ActionTask) {
        activity?.let {
            actionActivity.getTaskViewModel().deleteOne(actionTask.id)

            SnackbarHelper.showAction(
                activity = it,
                message = resources.getString(R.string.pending_task_cancelled),
                actionText = resources.getString(R.string.pending_task_cancelled_undo),
                onActionClick = {
                    actionActivity.getTaskViewModel().insert(actionTask)
                },
                duration = UNDO_ACTION_DURATION
            )
        }
    }

    /**
     * Initialize Pull to refresh
     */
    private fun initOnRefreshListener() {
        binding.fragmentTasksSwipeToRefresh.setOnRefreshListener {
            actionActivity.sendPendingTasks()
            binding.fragmentTasksSwipeToRefresh.isRefreshing = false
        }
    }

    private fun setupAdapters() {
        val isFromSettings = tableName.isEmpty()
        pendingAdapter = TaskListAdapter(isFromSettings, ActionTask.Status.PENDING) { actionTask ->
            onActionClick(actionTask)
        }

        completedAdapter = TaskListAdapter(isFromSettings, ActionTask.Status.SUCCESS) { actionTask ->
            onActionClick(actionTask)
        }
    }

    private fun onActionClick(actionTask: ActionTask) {
        val shouldNavigateToActionForm =
            !actionTask.isSuccess() && actionTask.actionInfo.allParameters?.isNotEmpty() == true
        when {
            shouldNavigateToActionForm -> {
                setSharedAxisXExitTransition()
                BaseApp.genericNavigationResolver.navigateToActionForm(
                    viewDataBinding = binding,
                    tableName = actionTask.actionInfo.tableName,
                    itemId = "",
                    relationName = "",
                    parentItemId = "",
                    pendingTaskId = actionTask.id,
                    actionUUID = actionTask.actionInfo.actionUUID,
                    navbarTitle = actionTask.actionInfo.preferredShortName
                )
            }
            actionTask.isErrorServer() -> {
                actionActivity.getTaskViewModel().deleteOne(actionTask.id)
                // As it's sent as a new action we have to update the date with the current date
                actionTask.date = Date()
                // UUID.randomUUID() to send action as new fresh action otherwise will be ignored by the
                // server (server doesn't treat same actions with same id)
                ActionHelper.updateActionContentId(actionTask.actionContent)
                actionActivity.sendAction(actionTask, tableName) {
                    // Nothing to do
                }
            }
        }
    }

    override fun onServerAccessible() {
        binding.serverStatus.text = serverAccessibleString
        binding.serverStatus.chipIcon = serverAccessibleDrawable
    }

    override fun onServerInaccessible() {
        binding.serverStatus.text = serverNotAccessibleString
        binding.serverStatus.chipIcon = serverNotAccessibleDrawable
    }

    override fun onNoInternet() {
        binding.serverStatus.text = noInternetString
        binding.serverStatus.chipIcon = noInternetDrawable
    }

    private fun onServerStatusClick() {
        binding.serverStatus.text = checkingString
        delegate.checkNetwork(this)
        actionActivity.sendPendingTasks()
    }
}
