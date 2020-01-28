package com.qmarciset.androidmobileui.detail

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.qmarciset.androidmobileui.BaseFragment
import com.qmarciset.androidmobileui.FragmentCommunication
import com.qmarciset.androidmobileui.viewmodel.EntityViewModel

class EntityDetailFragment : Fragment(), BaseFragment {

    private var itemId: String = "0"
    private var tableName: String = ""
    private lateinit var delegate: FragmentCommunication
    private lateinit var entityViewModel: EntityViewModel<*>

    companion object {
        fun newInstance(itemId: String, tableName: String) = EntityDetailFragment().apply {
            arguments = Bundle().apply {
                putString("itemId", itemId)
                putString("tableName", tableName)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        arguments?.getString("itemId")?.let { itemId = it }
        arguments?.getString("tableName")?.let { tableName = it }

        getViewModel()

        val dataBinding: ViewDataBinding = DataBindingUtil.inflate<ViewDataBinding>(
            inflater,
            delegate.fromTableInterface.detailLayoutFromTable(tableName),
            container,
            false
        ).apply {
            delegate.viewDataBindingInterface.setEntityViewModel(this, entityViewModel)
            lifecycleOwner = viewLifecycleOwner
        }
        return dataBinding.root
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is FragmentCommunication) {
            delegate = context
        }
        // access resources elements
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setupObservers()
    }

    override fun getViewModel() {
        entityViewModel = ViewModelProvider(
            this,
            EntityViewModel.EntityViewModelFactory(
                delegate.appInstance,
                delegate.appDatabaseInterface,
                delegate.apiService,
                itemId,
                tableName
            )
        )[EntityViewModel::class.java]
    }

    override fun setupObservers() {
        entityViewModel.entity.observe(viewLifecycleOwner, Observer {
        })
    }
}
