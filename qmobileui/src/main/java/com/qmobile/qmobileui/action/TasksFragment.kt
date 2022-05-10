
package com.qmobile.qmobileui.action

import android.content.Context
import android.graphics.Canvas
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.qmobile.qmobileapi.model.entity.EntityModel
import com.qmobile.qmobiledatastore.dao.ActionTask
import com.qmobile.qmobiledatastore.dao.ActionTaskDao
import com.qmobile.qmobiledatastore.dao.STATUS
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobiledatasync.toast.MessageType
import com.qmobile.qmobiledatasync.viewmodel.EntityListViewModel
import com.qmobile.qmobiledatasync.viewmodel.TaskViewModel
import com.qmobile.qmobiledatasync.viewmodel.factory.getEntityListViewModel
import com.qmobile.qmobiledatasync.viewmodel.factory.getTaskViewModel
import com.qmobile.qmobileui.BaseFragment
import com.qmobile.qmobileui.FragmentCommunication
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.databinding.FragmentActionTasksBinding
import com.qmobile.qmobileui.network.NetworkChecker
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

class TasksFragment : Fragment(), BaseFragment {

    private var _binding: ViewDataBinding? = null
    internal lateinit var entityListViewModel: EntityListViewModel<EntityModel>
    val binding get() = _binding!!
    override lateinit var delegate: FragmentCommunication

    lateinit var adapter: TasksListAdapter
    private lateinit var actionTaskDao: ActionTaskDao
    lateinit var recyclerView: RecyclerView

    lateinit var taskViewModel: TaskViewModel
    var pendingTasks: List<ActionTask> = emptyList()
    var tableName: String = ""
    var currentItemId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        setHasOptionsMenu(true)
        arguments?.getString("tableName")?.let { tableName = it }
        arguments?.getString("currentItemId")?.let {
            currentItemId = it
        }

        entityListViewModel = getEntityListViewModel(
            activity,
            if (tableName.isEmpty()) {
                BaseApp.genericTableHelper.tableNames().first()
            } else {
                tableName
            },
            delegate.apiService
        )
        taskViewModel = getTaskViewModel(activity)
        actionTaskDao = taskViewModel.dao

        if (_binding == null) {
            _binding = FragmentActionTasksBinding.inflate(
                inflater,
                container,
                false
            ).apply {
                lifecycleOwner = viewLifecycleOwner
                this@TasksFragment.recyclerView = recyclerView
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
                        val adapter = recyclerView.adapter as TasksListAdapter

                        if (adapter.isItemDeletable(viewHolder.adapterPosition)) {
                            super.onChildDraw(
                                c,
                                recyclerView,
                                viewHolder,
                                dX,
                                dY,
                                actionState,
                                isCurrentlyActive
                            )
                        }
                    }

                    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                        val adapter = recyclerView.adapter as TasksListAdapter

                        if (adapter.isItemDeletable(viewHolder.adapterPosition)) {
                            lifecycleScope.launch {
                                adapter.getItemByPosition(viewHolder.absoluteAdapterPosition)?.let {
                                    actionTaskDao.deleteById(
                                        it.id
                                    )
                                }
                            }
                            adapter.removeAt(viewHolder.absoluteAdapterPosition)
                        }
                    }
                }
                val itemTouchHelper = ItemTouchHelper(swipeHandler)
                itemTouchHelper.attachToRecyclerView(recyclerView)
                viewModel = taskViewModel
            }
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initOnRefreshListener()
        observeData()
    }

    private fun setupRecyclerView(list: MutableList<ActionTask?>, serverStatus: String) {
        adapter = TasksListAdapter(
            requireContext(), list, serverStatus
        ) { position ->

            if(position == 0){
                sendPendingTasks(pendingTasks)
            } else{
                list[position]?.let { it1 ->
                    it1.actionInfo.tableName?.let { tableName ->
                        BaseApp.genericNavigationResolver.navigateFromPendingTasksToActionForm(
                            binding,
                            it1.id,
                            tableName
                        )
                    }
                }
            }

        }
        val layoutManager = LinearLayoutManager(requireContext())
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = adapter

        val dividerItemDecoration = DividerItemDecoration(
            recyclerView.context,
            layoutManager.orientation
        )
        recyclerView.addItemDecoration(dividerItemDecoration)
    }

    private fun observeData() {
        actionTaskDao.getAll().observe(viewLifecycleOwner, { allTasks ->
            val filteredList = allTasks.filter {
                if (!tableName.isNullOrEmpty()) {
                    it.actionInfo.tableName == tableName
                } else {
                    // When we come from settings (no filter on table name)
                    true
                }
            }.filter {
                if (currentItemId != null) {
                    // From detail fragment (show only current entity tasks)
                    it.actionInfo.currentRecordId == currentItemId
                } else {
                    true // From EntityListFragment
                }
            }
            pendingTasks = filteredList.filter { actionTask -> actionTask.status == STATUS.PENDING }
                .sortedByDescending { actionTask -> actionTask.date }
            val history =
                filteredList.filter { actionTask ->
                    actionTask.status == STATUS.SUCCESS ||
                            actionTask.status == STATUS.ERROR_SERVER
                }.takeLast(10).sortedByDescending { actionTask -> actionTask.date }
            // The 2 null items used as placeholders for sections titles Pending/History

            delegate.checkNetwork(object : NetworkChecker {
                override fun onServerAccessible() {
                    setupRecyclerView(
                        (listOf(null) + pendingTasks + listOf(
                            null
                        ) + history
                                ) as MutableList<ActionTask?>, getString(R.string.server_accessible)
                    )

                }

                override fun onServerInaccessible() {
                    setupRecyclerView(
                        (listOf(null) + pendingTasks + listOf(
                            null
                        ) + history
                                ) as MutableList<ActionTask?>,
                        getString(R.string.server_not_accessible)
                    )
                }

                override fun onNoInternet() {
                    setupRecyclerView(
                        (
                                listOf(null) + pendingTasks.sortedByDescending { actionTask -> actionTask.date } + listOf(
                                    null
                                ) + history.sortedByDescending { actionTask -> actionTask.date }
                                ) as MutableList<ActionTask?>,
                        getString(R.string.no_internet)
                    )
                }
            })
        })
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is FragmentCommunication) {
            delegate = context
        }
    }

    /**
     * Initialize Pull to refresh
     */
    private fun initOnRefreshListener() {
        (binding as FragmentActionTasksBinding).apply {
            fragmentListSwipeToRefresh.setOnRefreshListener {
                pendingTasks?.let { sendPendingTasks(it) }
                fragmentListSwipeToRefresh.isRefreshing = false
            }
        }
    }

    private fun uploadImages(actionTask: ActionTask) {
        val bodies = actionTask.actionInfo.imagesToUpload?.mapValues {
            val fileUri = Uri.parse(it.value)
            val stream = activity?.contentResolver?.openInputStream(fileUri)
            val body = stream?.readBytes()?.let { it1 ->
                it1
                    .toRequestBody(
                        "application/octet".toMediaTypeOrNull(),
                        0, it1.size
                    )
            }
            body
        }

        delegate.checkNetwork(object : NetworkChecker {
            override fun onServerAccessible() {
                bodies?.let {
                    entityListViewModel.uploadImage(it, { parameterName, receivedId ->
                        actionTask.actionInfo.paramsToSubmit?.set(parameterName, receivedId)
                        actionTask.actionInfo.metaDataToSubmit?.set(parameterName, "uploaded")
                    }) {
                        sendTask(actionTask)
                    }
                }
            }

            override fun onServerInaccessible() {
                entityListViewModel.toastMessage.showMessage(
                    context?.getString(R.string.action_send_server_not_accessible),
                    tableName,
                    MessageType.ERROR
                )
            }

            override fun onNoInternet() {
                entityListViewModel.toastMessage.showMessage(
                    context?.getString(R.string.action_send_no_internet),
                    tableName,
                    MessageType.ERROR
                )
            }
        })
    }

    private fun sendTask(task: ActionTask) {
        delegate.checkNetwork(object : NetworkChecker {
            override fun onServerAccessible() {
                entityListViewModel.sendAction(
                    task.actionInfo.actionName,
                    ActionHelper.getActionContent(
                        tableName,
                        task.relatedItemId,
                        task.actionInfo.paramsToSubmit,
                        task.actionInfo.metaDataToSubmit, actionUUID = task.actionInfo.actionUUID
                    )
                ) { actionResponse ->
                    actionResponse?.let {
                        lifecycleScope.launch {
                            val status = if (actionResponse.success) {
                                STATUS.SUCCESS
                            } else {
                                STATUS.ERROR_SERVER
                            }

                            task.status = status
                            actionTaskDao.insert(
                                task
                            )
                        }
                    }
                }
            }

            override fun onServerInaccessible() {
                if (shouldShowActionError()) {
                    entityListViewModel.toastMessage.showMessage(
                        context?.getString(R.string.action_send_server_not_accessible),
                        tableName,
                        MessageType.NEUTRAL
                    )
                }
            }

            override fun onNoInternet() {
                if (shouldShowActionError()) {
                    entityListViewModel.toastMessage.showMessage(
                        context?.getString(R.string.action_send_no_internet),
                        tableName,
                        MessageType.NEUTRAL
                    )
                }
            }
        })
    }

    private fun sendPendingTasks(pendingTasks: List<ActionTask>) {
        taskViewModel.sendPendingTasks(
            pendingTasks,
            { task ->
                sendTask(task)
            }
        ) { task ->
            uploadImages(task)
        }
    }
}
