/*
 * Created by qmarciset on 3/6/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.action.pendingtasks

import android.content.Context
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
import com.qmobile.qmobileui.action.utils.ActionHelper.Companion.getActionContent
import com.qmobile.qmobileui.binding.px
import com.qmobile.qmobileui.databinding.FragmentActionTasksBinding
import com.qmobile.qmobileui.network.NetworkChecker
import com.qmobile.qmobileui.ui.SnackbarHelper
import com.qmobile.qmobileui.ui.setOnSingleClickListener
import com.qmobile.qmobileui.ui.setupToolbarTitle
import com.qmobile.qmobileui.ui.swipe.ItemDeleteButton
import com.qmobile.qmobileui.ui.swipe.SwipeHelper
import java.util.*

class TasksFragment : BaseFragment(), NetworkChecker {

    private var _binding: FragmentActionTasksBinding? = null
    val binding get() = _binding!!
    lateinit var pendingAdapter: TaskListAdapter
    lateinit var completedAdapter: TaskListAdapter

    internal lateinit var actionActivity: ActionActivity

    internal var tableName: String = ""
    internal var currentItemId: String = ""

    private lateinit var noInternetString: String
    private lateinit var serverAccessibleString: String
    private lateinit var serverNotAccessibleString: String

    companion object {
        internal const val MAX_PENDING_TASKS = 10
        private const val DIVIDER_INSET_START = 56
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        setHasOptionsMenu(true)
        arguments?.getString("tableName")?.let { tableName = it }
        arguments?.getString("currentItemId")?.let { currentItemId = it }

        activity?.setupToolbarTitle("Pending tasks")

        _binding = FragmentActionTasksBinding.inflate(inflater, container, false).apply {
            lifecycleOwner = viewLifecycleOwner
        }
        binding.serverStatus.setOnSingleClickListener {
            onServerStatusClick()
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initCellSwipe()
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
    private fun initCellSwipe() {
        val itemTouchHelper =
            ItemTouchHelper(object : SwipeHelper(binding.pendingRv) {
                override fun instantiateUnderlayButton(position: Int): List<ItemDeleteButton> {
                    val swipeButtons = mutableListOf<ItemDeleteButton>()
                    val swipeButton = createSwipeButton(position) { actionTask ->

                        activity?.let {
                            actionActivity.getTaskViewModel().deleteOne(actionTask.id)

                            SnackbarHelper.showAction(
                                activity = it,
                                message = resources.getString(R.string.pending_task_cancelled),
                                actionText = resources.getString(R.string.pending_task_cancelled_undo),
                                onActionClick = {
                                    actionActivity.getTaskViewModel().insert(actionTask)
                                }
                            )
                        }
                    }
                    swipeButtons.add(swipeButton)
                    return swipeButtons
                }
            })
        itemTouchHelper.attachToRecyclerView(binding.pendingRv)
    }

    private fun createSwipeButton(
        position: Int,
        onDelete: (actionTask: ActionTask) -> Unit
    ): ItemDeleteButton {
        return ItemDeleteButton(
            requireContext(),
            object : SwipeHelper.UnderlayButtonClickListener {
                override fun onClick() {
                    pendingAdapter.getItemByPosition(position)?.let { actionTask ->
                        onDelete(actionTask)
                    }
                }
            }
        )
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is ActionActivity) {
            actionActivity = context
        }
        noInternetString = resources.getString(R.string.no_internet)
        serverAccessibleString = resources.getString(R.string.server_accessible)
        serverNotAccessibleString = resources.getString(R.string.server_not_accessible)
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
                actionActivity.sendAction(
                    actionContent = getActionContent(actionTask.id, UUID.randomUUID().toString()),
                    actionTask = actionTask,
                    tableName = tableName
                ) {
                    // Nothing to do
                }
            }
        }
    }

    override fun onServerAccessible() {
        binding.serverStatus.text = serverAccessibleString
        binding.serverStatus.chipIcon = ContextCompat.getDrawable(requireContext(), R.drawable.domain)
    }

    override fun onServerInaccessible() {
        binding.serverStatus.text = serverNotAccessibleString
        binding.serverStatus.chipIcon = ContextCompat.getDrawable(requireContext(), R.drawable.domain_disabled)
    }

    override fun onNoInternet() {
        binding.serverStatus.text = noInternetString
        binding.serverStatus.chipIcon = ContextCompat.getDrawable(requireContext(), R.drawable.signal_disconnected)
    }

    private fun onServerStatusClick() {
        delegate.checkNetwork(this)
        actionActivity.sendPendingTasks()
    }
}
