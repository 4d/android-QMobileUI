/*
 * Created by qmarciset on 20/10/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.utils

import android.content.Intent
import android.net.Uri
import android.util.SparseArray
import androidx.core.content.ContextCompat
import androidx.core.util.forEach
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.tabs.TabLayout
import com.google.android.material.transition.MaterialFadeThrough
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobileui.R

/**
 * Manages the various graphs needed for a [TabLayout].
 *
 * This sample is a workaround until the Navigation Component supports multiple back stacks.
 */
@Suppress("LongMethod")
fun TabLayout.setupWithNavController(
    fragmentManager: FragmentManager,
    intent: Intent,
    onTabSelected: () -> Unit
): LiveData<NavController> {
    val navGraphIds: List<Int> = BaseApp.navGraphIds
    val containerId: Int = R.id.nav_host_container
    // Map of tags
    val graphIndexToTagMap = SparseArray<String>()
    // Result. Mutable live data with the selected controlled
    val selectedNavController = MutableLiveData<NavController>()

    val firstFragmentIndex = 0

    val settingsNavLabel = this.resources.getString(R.string.nav_settings)
    val pendingTasksNavLabel = this.resources.getString(R.string.nav_pending_tasks)
    val barcodeScanNavLabel = this.resources.getString(R.string.nav_barcode_scan)
    val feedbackNavLabel = this.resources.getString(R.string.nav_feedback)

    val isWithIcons = isWithIcons(navGraphIds, fragmentManager, containerId)

    // First create a NavHostFragment for each NavGraph ID
    navGraphIds.forEachIndexed { index, navGraphId ->

        val fragmentTag = getFragmentTag(index)

        // Find or create the Navigation host fragment
        val navHostFragment =
            obtainNavHostFragment(
                fragmentManager,
                fragmentTag,
                navGraphId,
                containerId
            )

        this.addTab(
            this.newTab().apply {
                val label = navHostFragment.navController.graph.findStartDestination().label
                this.text = label
                if (isWithIcons) {
                    BaseApp.genericTableFragmentHelper.getNavIcon(label.toString())
                        ?.let { drawableRes ->
                            this.icon = ContextCompat.getDrawable(this@setupWithNavController.context, drawableRes)
                        }
                }
            }
        )
        if (this.selectedTabPosition == -1) {
            this.getTabAt(0)?.select()
        }

        // Save to the map
        graphIndexToTagMap.put(index, fragmentTag)

        // Attach or detach nav host fragment depending on whether it's the selected item.
        if (this.selectedTabPosition == index) {
            // Update livedata with the selected graph
            selectedNavController.value = navHostFragment.navController
            attachNavHostFragment(
                fragmentManager,
                navHostFragment,
                index == 0
            )
        } else {
            detachNavHostFragment(
                fragmentManager,
                navHostFragment
            )
        }
    }

    // Now connect selecting an item with swapping Fragments
    var selectedItemTag = graphIndexToTagMap[this.selectedTabPosition]
    val firstFragmentTag = graphIndexToTagMap[firstFragmentIndex]
    var isOnFirstFragment = selectedItemTag == firstFragmentTag

    this.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
        override fun onTabSelected(tab: TabLayout.Tab?) {
            if (tab == null) return
            // Don't do anything if the state is state has already been saved.
            if (!fragmentManager.isStateSaved) {
//                onItemSelected()
                onTabSelected()

                val newlySelectedItemTag = graphIndexToTagMap[tab.position]
                if (selectedItemTag != newlySelectedItemTag) {
                    // When clicking on an item, go back to first level // TODO : CAN'T REMEMBER WHY...
//                    val previousSelectedFragment = fragmentManager.findFragmentByTag(selectedItemTag) as NavHostFragment
//                    val previousNavController = previousSelectedFragment.navController
                    // Pop the back stack to the start destination of the current navController graph
//                    previousNavController.popBackStack(
//                        previousNavController.graph.startDestinationId,
//                        false
//                    )

                    // If SettingsFragment, TaskFragment or BarcodeScannerFragment, we want to pop it
                    val previousSelectedFragment = fragmentManager.findFragmentByTag(selectedItemTag) as NavHostFragment
                    when (previousSelectedFragment.navController.currentBackStackEntry?.destination?.label) {
                        settingsNavLabel, pendingTasksNavLabel, barcodeScanNavLabel, feedbackNavLabel ->
                            previousSelectedFragment.navController.popBackStack()
                    }
                    // If we were in Settings' pending tasks, we need to pop twice
                    when (previousSelectedFragment.navController.currentBackStackEntry?.destination?.label) {
                        settingsNavLabel -> previousSelectedFragment.navController.popBackStack()
                    }

                    // Pop everything above the first fragment (the "fixed start destination")
                    fragmentManager.popBackStack(
                        firstFragmentTag,
                        FragmentManager.POP_BACK_STACK_INCLUSIVE
                    )
                    val selectedFragment = fragmentManager.findFragmentByTag(newlySelectedItemTag)
                        as NavHostFragment

                    // Exclude the first fragment tag because it's always in the back stack.
                    if (firstFragmentTag != newlySelectedItemTag) {
                        // Commit a transaction that cleans the back stack and adds the first fragment
                        // to it, creating the fixed started destination.

                        selectedFragment.enterTransition = MaterialFadeThrough().apply {
                            duration = resources.getInteger(R.integer.motion_duration_large).toLong()
                        }

                        fragmentManager.beginTransaction()
//                        .setCustomAnimations(
//                            R.anim.nav_default_enter_anim,
//                            R.anim.nav_default_exit_anim,
//                            R.anim.nav_default_pop_enter_anim,
//                            R.anim.nav_default_pop_exit_anim
//                        )
                            .attach(selectedFragment)
                            .setPrimaryNavigationFragment(selectedFragment)
                            .apply {
                                // Detach all other Fragments
                                graphIndexToTagMap.forEach { _, fragmentTagIter ->
                                    if (fragmentTagIter != newlySelectedItemTag) {
                                        detach(fragmentManager.findFragmentByTag(firstFragmentTag)!!)
                                    }
                                }
                            }
                            .addToBackStack(firstFragmentTag)
                            .setReorderingAllowed(true)
                            .commit()
                    }
                    selectedItemTag = newlySelectedItemTag
                    isOnFirstFragment = selectedItemTag == firstFragmentTag
                    selectedNavController.value = selectedFragment.navController
                }
            }
        }

        override fun onTabUnselected(tab: TabLayout.Tab?) {}

        // Optional: on item reselected, pop back stack to the destination of the graph
        override fun onTabReselected(tab: TabLayout.Tab?) {
            if (tab == null) return
            val newlySelectedItemTag = graphIndexToTagMap[tab.position]
            val selectedFragment = fragmentManager.findFragmentByTag(newlySelectedItemTag)
                as NavHostFragment
            val navController = selectedFragment.navController
            // Pop the back stack to the start destination of the current navController graph
            navController.popBackStack(
                navController.graph.startDestinationId,
                false
            )
        }
    })

    // Handle deep link
    setupDeepLinks(navGraphIds, fragmentManager, containerId, intent)

    // Finally, ensure that we update our TabLayout when the back stack changes
    fragmentManager.addOnBackStackChangedListener {
        if (!isOnFirstFragment && !fragmentManager.isOnBackStack(firstFragmentTag)) {
            this.getTabAt(firstFragmentIndex)?.select()
        }

        // Reset the graph if the currentDestination is not valid (happens when the back
        // stack is popped after using the back button).
        selectedNavController.value?.let { controller ->
            if (controller.currentDestination == null) {
                controller.navigate(controller.graph.id)
            }
        }
    }
    return selectedNavController
}

private fun isWithIcons(
    navGraphIds: List<Int>,
    fragmentManager: FragmentManager,
    containerId: Int
): Boolean {
    navGraphIds.forEachIndexed { index, navGraphId ->

        val fragmentTag = getFragmentTag(index)

        // Find or create the Navigation host fragment
        val navHostFragment =
            obtainNavHostFragment(
                fragmentManager,
                fragmentTag,
                navGraphId,
                containerId
            )
        val label = navHostFragment.navController.graph.findStartDestination().label
        val icon = BaseApp.genericTableFragmentHelper.getNavIcon(label.toString())
        if (icon != null) {
            return true
        }
    }
    return false
}

private fun TabLayout.setupDeepLinks(
    navGraphIds: List<Int>,
    fragmentManager: FragmentManager,
    containerId: Int,
    intent: Intent
) {
    var dataClass = ""

    val data: Uri? = intent.data
    if (data != null && data.isHierarchical) {
        val uri = Uri.parse(intent.dataString)
        dataClass = uri.getQueryParameter("dataClass") ?: ""
    }
    intent.getStringExtra(DeepLinkUtil.PN_DEEPLINK_DATACLASS)?.let {
        dataClass = it
    }

    BaseApp.runtimeDataHolder.tableInfo[dataClass]?.label?.let { targetDataClassLabel ->
        navGraphIds.forEachIndexed { index, navGraphId ->
            val fragmentTag =
                getFragmentTag(index)

            // Find or create the Navigation host fragment
            val navHostFragment =
                obtainNavHostFragment(
                    fragmentManager,
                    fragmentTag,
                    navGraphId,
                    containerId
                )

            val currentGraphLabel = navHostFragment.navController.graph.findStartDestination().label

            // Handle Intent
            if (currentGraphLabel == targetDataClassLabel && index != this.selectedTabPosition) {
                this.getTabAt(index)?.select()
            }
        }
    }
}

private fun detachNavHostFragment(
    fragmentManager: FragmentManager,
    navHostFragment: NavHostFragment
) {
    fragmentManager.beginTransaction()
        .detach(navHostFragment)
        .commitNow()
}

private fun attachNavHostFragment(
    fragmentManager: FragmentManager,
    navHostFragment: NavHostFragment,
    isPrimaryNavFragment: Boolean
) {
    fragmentManager.beginTransaction()
        .attach(navHostFragment)
        .apply {
            if (isPrimaryNavFragment) {
                setPrimaryNavigationFragment(navHostFragment)
            }
        }
        .commitNow()
}

private fun obtainNavHostFragment(
    fragmentManager: FragmentManager,
    fragmentTag: String,
    navGraphId: Int,
    containerId: Int
): NavHostFragment {
    // If the Nav Host fragment exists, return it
    val existingFragment = fragmentManager.findFragmentByTag(fragmentTag) as NavHostFragment?
    existingFragment?.let { return it }

    // Otherwise, create it and return it.
    val navHostFragment = NavHostFragment.create(navGraphId)
    fragmentManager.beginTransaction()
        .add(containerId, navHostFragment, fragmentTag)
        .commitNow()
    return navHostFragment
}

private fun FragmentManager.isOnBackStack(backStackName: String): Boolean {
    val backStackCount = backStackEntryCount
    for (index in 0 until backStackCount) {
        if (getBackStackEntryAt(index).name == backStackName) {
            return true
        }
    }
    return false
}

private fun getFragmentTag(index: Int) = "tabLayout#$index"
