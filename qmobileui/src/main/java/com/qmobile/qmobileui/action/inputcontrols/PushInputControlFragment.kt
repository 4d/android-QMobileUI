/*
 * Created by qmarciset on 15/9/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.action.inputcontrols

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.core.view.MenuProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobiledatasync.utils.FieldMapping
import com.qmobile.qmobileui.BaseFragment
import com.qmobile.qmobileui.action.actionparameters.ActionParametersFragment.Companion.INPUT_CONTROL_PUSH_DISPLAY_TEXT_KEY
import com.qmobile.qmobileui.action.actionparameters.ActionParametersFragment.Companion.INPUT_CONTROL_PUSH_FIELD_VALUE_KEY
import com.qmobile.qmobileui.action.actionparameters.ActionParametersFragment.Companion.INPUT_CONTROL_PUSH_FRAGMENT_REQUEST_KEY
import com.qmobile.qmobileui.activity.mainactivity.MainActivity
import com.qmobile.qmobileui.databinding.InputControlPushFragmentBinding
import com.qmobile.qmobileui.ui.BounceEdgeEffectFactory
import com.qmobile.qmobileui.ui.noTabLayoutUI
import com.qmobile.qmobileui.ui.setSharedAxisXEnterTransition
import java.util.LinkedList
import java.util.concurrent.atomic.AtomicBoolean

class PushInputControlFragment : BaseFragment(), MenuProvider, InputControlDataHandler {

    private var _binding: InputControlPushFragmentBinding? = null
    private val binding get() = _binding!!

    override var fieldMapping: FieldMapping? = null

    private var isMandatory = false
    private var searchPattern = ""

    private var hasSearch = false
    private lateinit var adapter: InputControlAdapter
    private var dataSourceHandler: InputControlDataSourceHandler? = null
    private val menuInitialized = AtomicBoolean(false)

    companion object {
        private const val SEARCHABLE_THRESHOLD = 10
    }

    private val searchListener: SearchView.OnQueryTextListener =
        object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                newText?.let {
                    if (searchPattern != it) {
                        searchPattern = it
                        dataSourceHandler?.setSearchQuery(searchPattern)
                    }
                }
                return true
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.getString("name")?.let { inputControlName ->
            fieldMapping = BaseApp.runtimeDataHolder.inputControls.find { it.name == inputControlName }
        }
        arguments?.getBoolean("mandatory")?.let { isMandatory = it }
        setSharedAxisXEnterTransition()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = InputControlPushFragmentBinding.inflate(inflater, container, false).apply {
            lifecycleOwner = viewLifecycleOwner
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupInputControlData(isMandatory)

        initRecyclerView()
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        if (hasSearch) {
            setupSearchView(menu, menuInflater)
            searchView.setOnQueryTextListener(searchListener)
        }
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return false
    }

    override fun FieldMapping.prepareStaticData(isMandatory: Boolean) {
        handleMandatory(this.getChoiceList(), isMandatory) { items ->
            initAdapter(items)
        }
    }

    override fun FieldMapping.prepareCurrentEntity(isMandatory: Boolean, choiceList: Map<String, Any>?) {
        var wantedChoiceList: Any? = choiceList
        choiceList?.let {
            if (it.containsKey("choiceList")) {
                wantedChoiceList = it["choiceList"]
            }
        }
        handleMandatory(this.getChoiceList(wantedChoiceList), isMandatory) { items ->
            initAdapter(items)
        }
    }

    override fun FieldMapping.prepareDataSource(isMandatory: Boolean) {
        this@PushInputControlFragment.binding.circularProgress.visibility = View.VISIBLE
        dataSourceHandler = super.handleDataSource(
            fragment = this@PushInputControlFragment,
            fieldMapping = this,
            callback = { entities, field, entityFormat, hasSearch ->
                this@PushInputControlFragment.binding.circularProgress.visibility = View.GONE
                this@PushInputControlFragment.hasSearch = hasSearch
                handleMandatory(entities, isMandatory) { items ->
                    initAdapter(items, field, entityFormat)
                }
            }
        )
    }

    private fun initRecyclerView() {
        if (!noTabLayoutUI) {
            binding.pushConstraintLayout.setPadding(0, 0, 0, getPaddingBottom())
        }
        binding.inputControlListRecyclerView.layoutManager =
            LinearLayoutManager(activity, RecyclerView.VERTICAL, false)
        val divider = DividerItemDecoration(activity, LinearLayoutManager.VERTICAL)
        binding.inputControlListRecyclerView.addItemDecoration(divider)
        binding.inputControlListRecyclerView.edgeEffectFactory = BounceEdgeEffectFactory()
    }

    private fun initAdapter(items: LinkedList<Any>, field: String? = null, entityFormat: String? = null) {
        val threshold = if (isMandatory) {
            SEARCHABLE_THRESHOLD
        } else {
            SEARCHABLE_THRESHOLD + 1
        }
        if (items.size >= threshold && !menuInitialized.getAndSet(true)) {
            initMenuProvider()
        }
        adapter = InputControlAdapter(
            context = requireContext(),
            items = items,
            fieldMapping = fieldMapping,
            isMandatory = isMandatory,
            field = field,
            entityFormat = entityFormat,
            onItemClick = { displayText, fieldValue, _ ->
                onItemClick(displayText, fieldValue)
            }
        )
        binding.inputControlListRecyclerView.adapter = adapter
    }

    private fun onItemClick(displayText: String, fieldValue: Any?) {
        val result = Bundle().apply {
            putString(INPUT_CONTROL_PUSH_DISPLAY_TEXT_KEY, displayText)
            fieldValue?.let {
                putString(INPUT_CONTROL_PUSH_FIELD_VALUE_KEY, fieldValue.toString())
            }
        }
        if (isAdded) {
            parentFragmentManager.setFragmentResult(INPUT_CONTROL_PUSH_FRAGMENT_REQUEST_KEY, result)
        }
        (activity as? MainActivity?)?.navController?.navigateUp()
    }
}
