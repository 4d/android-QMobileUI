/*
 * Created by qmarciset on 3/6/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.action.pendingtasks

import android.content.Context
import android.graphics.Canvas
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.qmobile.qmobiledatastore.dao.ActionTask
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobileui.ActionActivity
import com.qmobile.qmobileui.ActivitySettingsInterface
import com.qmobile.qmobileui.BaseFragment
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.action.utils.ActionHelper.Companion.getActionContent
import com.qmobile.qmobileui.action.utils.SwipeToDeleteCallback
import com.qmobile.qmobileui.databinding.FragmentActionTasksBinding
import com.qmobile.qmobileui.network.NetworkChecker
import com.qmobile.qmobileui.ui.BounceEdgeEffectFactory
import java.util.UUID
import java.util.Date

class TasksFragment : BaseFragment(), NetworkChecker {

    private var _binding: FragmentActionTasksBinding? = null
    val binding get() = _binding!!
    private lateinit var adapter: TasksListAdapter

    private lateinit var activitySettingsInterface: ActivitySettingsInterface
    internal lateinit var actionActivity: ActionActivity

    private var serverStatus = ""
    internal var tableName: String = ""
    internal var currentItemId: String = ""

    private lateinit var noInternetString: String
    private lateinit var serverAccessibleString: String
    private lateinit var serverNotAccessibleString: String

    companion object {
        internal const val MAX_PENDING_TASKS = 10
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        setHasOptionsMenu(true)
        arguments?.getString("tableName")?.let { tableName = it }
        arguments?.getString("currentItemId")?.let { currentItemId = it }

        _binding = FragmentActionTasksBinding.inflate(inflater, container, false).apply {
            lifecycleOwner = viewLifecycleOwner
            viewModel = actionActivity.getTaskViewModel()
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initCellSwipe()
        initRecyclerView()
        initOnRefreshListener()
        TasksFragmentObserver(this).initObservers()
        activitySettingsInterface.checkNetwork(this)
    }

    /**
     * Initialize recyclerView
     */
    private fun initRecyclerView() {
        binding.fragmentTasksRecyclerView.addItemDecoration(
            DividerItemDecoration(
                activity,
                RecyclerView.VERTICAL
            )
        )
        binding.fragmentTasksRecyclerView.layoutManager = LinearLayoutManager(activity, RecyclerView.VERTICAL, false)
        binding.fragmentTasksRecyclerView.edgeEffectFactory = BounceEdgeEffectFactory()
    }

    /**
     * Initialize Swipe to delete
     */
    private fun initCellSwipe() {

        val swipeHandler = object : SwipeToDeleteCallback(requireContext()) {

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                if (adapter.isItemDeletable(viewHolder.bindingAdapterPosition)) {
                    super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
                }
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                if (adapter.isItemDeletable(viewHolder.bindingAdapterPosition)) {
                    adapter.getItemByPosition(viewHolder.absoluteAdapterPosition)?.let {
                        actionActivity.getTaskViewModel().deleteOne(it.id)
                    }
                    adapter.removeAt(viewHolder.absoluteAdapterPosition)
                }
            }
        }

        val itemTouchHelper = ItemTouchHelper(swipeHandler)
        itemTouchHelper.attachToRecyclerView(binding.fragmentTasksRecyclerView)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is ActivitySettingsInterface) {
            activitySettingsInterface = context
        }
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

    fun setupAdapter(pendingTasks: List<ActionTask?>, history: List<ActionTask?>) {
        val isFromSettings = tableName.isEmpty()
        // The 2 null items used as placeholders for sections titles Pending/History
        val newList = (mutableListOf(null) + pendingTasks + mutableListOf(null) + history) as MutableList<ActionTask?>
        adapter = TasksListAdapter(isFromSettings, requireContext(), newList, serverStatus) { position ->
            if (position == 0) {
                actionActivity.sendPendingTasks()
            } else {
                newList[position]?.let { selectedTask ->
                    val shouldNavigateToActionForm =
                        selectedTask.actionInfo.allParameters?.isNotEmpty() ?: false && !selectedTask.isSuccess()
                    if (shouldNavigateToActionForm) {
                        selectedTask.let { task ->
                            BaseApp.genericNavigationResolver.navigateToActionForm(
                                viewDataBinding = binding,
                                tableName = task.actionInfo.tableName,
                                itemId = "",
                                relationName = "",
                                parentItemId = "",
                                pendingTaskId = task.id,
                                actionUUID = task.actionInfo.actionUUID,
                                navbarTitle = task.actionInfo.preferredShortName
                            )
                        }
                    } else {
                        if (selectedTask.isErrorServer()) {
                            actionActivity.getTaskViewModel().deleteOne(selectedTask.id)
                            // As it's sent as a new action we have to update the date with the current date
                            selectedTask.date = Date()
                            // UUID.randomUUID() to send action as new fresh action otherwise will be ignored by the server (server don't tread same actions with same id)
                            actionActivity.sendAction(
                                actionContent = getActionContent(selectedTask.id, UUID.randomUUID().toString()),
                                actionTask = selectedTask,
                                tableName = tableName
                            ) {

                            }
                        }
                    }
                }

            }
        }
        binding.fragmentTasksRecyclerView.adapter = adapter
    }

    override fun onServerAccessible() {
        serverStatus = serverAccessibleString
    }

    override fun onServerInaccessible() {
        serverStatus = serverNotAccessibleString
    }

    override fun onNoInternet() {
        serverStatus = noInternetString
    }
}
