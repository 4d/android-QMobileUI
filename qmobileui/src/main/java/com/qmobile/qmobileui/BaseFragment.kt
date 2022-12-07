/*
 * Created by Quentin Marciset on 7/2/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuInflater
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import com.google.android.material.appbar.AppBarLayout
import com.qmobile.qmobileui.action.actionparameters.ActionParametersFragment
import com.qmobile.qmobileui.action.inputcontrols.PushInputControlFragment
import com.qmobile.qmobileui.action.pendingtasks.TasksFragment
import com.qmobile.qmobileui.detail.EntityDetailFragment
import com.qmobile.qmobileui.list.EntityListFragment
import com.qmobile.qmobileui.ui.hasNavIcon
import com.qmobile.qmobileui.ui.setSharedAxisZExitTransition
import com.qmobile.qmobileui.utils.hideKeyboard

abstract class BaseFragment : Fragment() {

    internal lateinit var delegate: FragmentCommunication

    internal var navbarTitle: String? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is FragmentCommunication) {
            delegate = context
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupAppBar()
    }

    protected fun getPaddingBottom(): Int = if (hasNavIcon()) {
        resources.getDimension(R.dimen.nav_view_height).toInt()
    } else {
        resources.getDimension(R.dimen.nav_view_height_no_icon).toInt()
    }

    protected fun MenuProvider.initMenuProvider() {
        val menuHost: MenuHost = requireActivity() as MenuHost
        menuHost.addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun setupAppBar() {
        // Mandatory step as the SwipeRefreshLayout will cause issues with AppBarLayout coloring while scrolling
        activity?.findViewById<AppBarLayout>(R.id.appbar)?.let { appBarLayout ->
            when (this) {
                is EntityListFragment -> appBarLayout.liftOnScrollTargetViewId = R.id.fragment_list_recycler_view
                is EntityDetailFragment -> appBarLayout.liftOnScrollTargetViewId = R.id.base_detail_scroll_view
                is ActionParametersFragment -> appBarLayout.liftOnScrollTargetViewId = R.id.parameters_recycler_view
                is TasksFragment -> appBarLayout.liftOnScrollTargetViewId = R.id.task_nested_scroll_view
                is PushInputControlFragment ->
                    appBarLayout.liftOnScrollTargetViewId = R.id.input_control_list_recycler_view
            }
        }
    }

    protected lateinit var searchView: SearchView
    protected lateinit var searchPlate: EditText

    @SuppressLint("ClickableViewAccessibility")
    protected fun setupSearchView(
        menu: Menu,
        inflater: MenuInflater,
        searchableWithBarcode: Boolean = false,
        navigateToScanner: () -> Unit = {}
    ) {
        inflater.inflate(R.menu.menu_search, menu)
        searchView = menu.findItem(R.id.search).actionView as SearchView
        searchPlate =
            searchView.findViewById(androidx.appcompat.R.id.search_src_text) as EditText
        searchPlate.hint = ""

        if (searchableWithBarcode) {
            val scanIcon = ContextCompat.getDrawable(requireContext(), R.drawable.qr_code_scanner)
            searchPlate.setCompoundDrawablesWithIntrinsicBounds(null, null, scanIcon, null)

            searchPlate.setOnTouchListener { view, motionEvent ->
                if (motionEvent.action == MotionEvent.ACTION_UP) {
                    if (motionEvent.x >= view.width - searchPlate.totalPaddingEnd) {
                        hideKeyboard(activity)
                        setSharedAxisZExitTransition()
                        navigateToScanner()
                    }
                }
                true
            }
        }

        searchPlate.setOnEditorActionListener { textView, actionId, keyEvent ->
            if ((keyEvent != null && (keyEvent.keyCode == KeyEvent.KEYCODE_ENTER)) ||
                (actionId == EditorInfo.IME_ACTION_DONE)
            ) {
                hideKeyboard(activity)
                textView.clearFocus()
                if (textView.text.isEmpty()) {
                    searchView.onActionViewCollapsed()
                }
            }
            true
        }

        searchView.setOnCloseListener {
            searchView.onActionViewCollapsed()
            true
        }
    }
}
