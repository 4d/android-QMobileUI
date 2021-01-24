/*
 * Created by Quentin Marciset on 24/1/2021.
 * 4D SAS
 * Copyright (c) 2021 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.ui

import android.app.SearchableInfo
import android.content.Context
import android.util.Log
import android.view.MenuItem
import android.widget.SearchView
import android.widget.Toast
import androidx.core.view.MenuItemCompat
import timber.log.Timber

class CustomSearchView(var context: Context,  menuItem: MenuItem) {
    private val searchView = MenuItemCompat.getActionView(menuItem) as SearchView

    val addListener = { searchableInfo: SearchableInfo? ->
        searchView.setOnCloseListener { true }
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                Toast.makeText(context, "Submit :: $query", Toast.LENGTH_LONG).show()
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                Timber.d("onTextchange :: $newText")
               return false
            }

        })
        searchView.setSearchableInfo(searchableInfo)
        searchView
    }

}