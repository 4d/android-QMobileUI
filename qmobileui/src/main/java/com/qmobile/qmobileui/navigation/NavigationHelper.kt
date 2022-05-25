/*
 * Created by qmarciset on 25/5/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.navigation

import androidx.databinding.ViewDataBinding
import androidx.fragment.app.FragmentActivity
import androidx.navigation.Navigation
import androidx.navigation.findNavController
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.action.ActionParametersFragmentDirections
import com.qmobile.qmobileui.settings.SettingsFragmentDirections

/**
 * Navigates from list or detail form to action form
 */
fun ViewDataBinding.navigateToActionForm(
    tableName: String,
    itemId: String,
    relationName: String,
    parentItemId: String,
    pendingTaskId: Long,
    navbarTitle: String
) {
    this.root.findNavController().navigate(
        SettingsFragmentDirections.toActionForm(
            tableName = tableName,
            itemId = itemId,
            relationName = relationName,
            parentItemId = parentItemId,
            taskId = pendingTaskId,
            navbarTitle = navbarTitle
        )
    )
}

/**
 * Navigates from action form to barcode scanner fragment
 */
fun ViewDataBinding.navigateToActionScanner(position: Int) {
    this.root.findNavController().navigate(
        ActionParametersFragmentDirections.actionParametersToScanner(position = position)
    )
}

/**
 * Navigates to TasksFragment
 */
fun FragmentActivity.navigateToPendingTasks(tableName: String, currentItemId: String) {
    Navigation.findNavController(this, R.id.nav_host_container).navigate(
        SettingsFragmentDirections.toPendingTasks(tableName = tableName, currentItemId = currentItemId)
    )
}
