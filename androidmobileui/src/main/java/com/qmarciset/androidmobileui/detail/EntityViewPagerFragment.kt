package com.qmarciset.androidmobileui.detail

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager.widget.ViewPager
import com.qmarciset.androidmobileui.BaseFragment
import com.qmarciset.androidmobileui.FragmentCommunication
import com.qmarciset.androidmobileui.R
import com.qmarciset.androidmobileui.viewmodel.EntityListViewModel

class EntityViewPagerFragment : Fragment(), BaseFragment {

    private var position: Int = 0
    private var tableName: String = ""
    private var viewPager: ViewPager? = null

    // BaseFragment
    override lateinit var delegate: FragmentCommunication

    // ViewModel
    private lateinit var entityListViewModel: EntityListViewModel<*>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewPager = inflater.inflate(R.layout.fragment_pager, container, false) as ViewPager
        arguments?.getInt("position")?.let { position = it }
        arguments?.getString("tableName")?.let { tableName = it }
        return viewPager
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is FragmentCommunication) {
            delegate = context
        }
        // Access resources elements
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        getViewModel()
        setupObservers()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewPager = null
    }

    override fun getViewModel() {
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

    override fun setupObservers() {
        entityListViewModel.entityList.observe(viewLifecycleOwner, Observer { entities ->
            entities?.let {
                // When entity list data changed, refresh the displayed list
                viewPager?.adapter =
                    EntityViewPagerAdapter(
                        this,
                        tableName,
                        it
                    )
                viewPager?.currentItem = position
            }
        })
    }
}
