/*
 * Created by Quentin Marciset on 24/1/2021.
 * 4D SAS
 * Copyright (c) 2021 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.ui

import android.app.SearchableInfo
import android.content.Context
import android.view.MenuItem
import android.widget.SearchView

class CustomSearchView(var context: Context?, menuItem: MenuItem, searchListener: SearchListener) {
    private val searchView = menuItem.actionView as SearchView
    val addListener = { searchableInfo: SearchableInfo? ->
        searchView.setOnCloseListener {
            searchView.onActionViewCollapsed()
            true }
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                searchListener.dataToSearch(query)
                return false
            }

            override fun onQueryTextChange(queryText: String): Boolean {
                searchListener.dataToSearch(queryText)
                return false
            }
        })
        searchView.setSearchableInfo(searchableInfo)
        searchView
    }
}

// Holds the search listener
interface SearchListener {
    fun dataToSearch(data: String)
}
