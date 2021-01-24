/*
 * Created by Quentin Marciset on 7/2/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.activity

import android.app.SearchManager
import android.view.Menu
import android.widget.SearchView
import androidx.appcompat.app.AppCompatActivity
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.ui.CustomSearchView

/**
 * Base AppCompatActivity for activities
 */
abstract class BaseActivity : AppCompatActivity() {
    private lateinit var searchView: SearchView

    companion object {
        // Constant used when returning to LoginActivity to display a toast message about logout
        const val LOGGED_OUT = "logged_out"
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        searchView = CustomSearchView(
            this.applicationContext,
            menu.findItem(R.id.search)
        ).addListener(
            (getSystemService(SEARCH_SERVICE) as SearchManager).getSearchableInfo(
                componentName
            )
        )
        return super.onCreateOptionsMenu(menu)
    }

    override fun onBackPressed() = if (!searchView.isIconified) {
        searchView.onActionViewCollapsed()
    } else {
        super.onBackPressed()
    }

}
