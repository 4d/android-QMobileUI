package com.qmarciset.androidmobileui.list

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.qmarciset.androidmobileui.FragmentCommunication
import com.qmarciset.androidmobileui.R
import com.qmarciset.androidmobileui.utils.fetchResourceString
import com.qmarciset.androidmobileui.viewmodel.EntityListViewModel
import kotlinx.android.synthetic.main.fragment_list_stub.*

class EntityListFragment : Fragment() {

    private var tableName: String = ""

    private lateinit var delegate: FragmentCommunication
    private lateinit var adapter: EntityListAdapter
    private lateinit var entityListViewModel: EntityListViewModel<*>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        arguments?.getString("tableName")?.let { tableName = it }

        getViewModel()

        val dataBinding: ViewDataBinding = DataBindingUtil.inflate<ViewDataBinding>(
            inflater,
            delegate.fromTableInterface.listLayoutFromTable(tableName),
            container,
            false
        ).apply {
            delegate.viewDataBindingInterface.setEntityListViewModel(this, entityListViewModel)
            lifecycleOwner = viewLifecycleOwner
        }

        return dataBinding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        initView()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is FragmentCommunication) {
            delegate = context
        }
        // access resources elements
    }

    private fun getViewModel() {
        val kClazz = delegate.fromTableInterface.entityListViewModelClassFromTable(tableName)
        entityListViewModel = activity?.run {
            ViewModelProvider(
                this,
                EntityListViewModel.EntityListViewModelFactory(
                    delegate.appInstance,
                    delegate.appDatabaseInterface,
                    delegate.apiService,
                    tableName,
                    delegate.fromTableInterface
                )
            )[kClazz.java]
        } ?: throw IllegalStateException("Invalid Activity")
    }

    private fun initView() {
        setupObservers()
        initRecyclerView()
        initOnRefreshListener()
        initSwipeToDeleteAndUndo()
    }

    private fun setupObservers() {
        entityListViewModel.entityList.observe(viewLifecycleOwner, Observer { entities ->
            entities?.let {
                adapter.setEntities(it)
            }
        })

        entityListViewModel.toastMessage.observe(viewLifecycleOwner, Observer { message ->
            val toastMessage = context?.fetchResourceString(message) ?: ""
            if (toastMessage.isNotEmpty()) {
                delegate.toast(toastMessage)
                // To avoid the error toast to be displayed without performing a refresh again
                entityListViewModel.toastMessage.postValue("")
            }
        })
    }

    private fun initRecyclerView() {
        adapter = EntityListAdapter(
            tableName,
            delegate.fromTableInterface,
            delegate.navigationInterface
        )

        fragment_list_recycler_view.layoutManager =
            LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
        fragment_list_recycler_view.addItemDecoration(
            DividerItemDecoration(
                activity,
                RecyclerView.VERTICAL
            )
        )

        fragment_list_recycler_view.adapter = adapter
    }

    private fun initOnRefreshListener() {
        fragment_list_swipe_to_refresh.setProgressBackgroundColorSchemeColor(
            ContextCompat.getColor(
                requireContext(), com.qmarciset.androidmobileui.R.color.list_swipe_to_refresh
            )
        )
        fragment_list_swipe_to_refresh.setColorSchemeColors(Color.WHITE)
        fragment_list_swipe_to_refresh.setOnRefreshListener {
            entityListViewModel.getAllFromApi()
            fragment_list_recycler_view.adapter = adapter
            fragment_list_swipe_to_refresh.isRefreshing = false
        }
    }

    private fun initSwipeToDeleteAndUndo() {
        val swipeToDeleteCallback: SwipeToDeleteCallback =
            object : SwipeToDeleteCallback(requireContext()) {
                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                    val position = viewHolder.adapterPosition
                    val item = adapter.getEntities()[position]
                    entityListViewModel.delete(item)
                    activity?.let {
                        val snackBar = Snackbar
                            .make(
                                it.findViewById<View>(android.R.id.content),
                                it.resources.getString(R.string.snackbar_remove),
                                Snackbar.LENGTH_LONG
                            )
                        snackBar.setAction(it.resources.getString(com.qmarciset.androidmobileui.R.string.snackbar_undo)) {
                            entityListViewModel.insert(item)
                            //                        rv_main.scrollToPosition(position)
                        }
                        snackBar.setActionTextColor(Color.YELLOW)
                        snackBar.show()
                    }
                }
            }

        val itemTouchHelper = ItemTouchHelper(swipeToDeleteCallback)
        itemTouchHelper.attachToRecyclerView(fragment_list_recycler_view)
    }

    override fun onDestroyView() {
        fragment_list_recycler_view.adapter = null
        super.onDestroyView()
    }
}
