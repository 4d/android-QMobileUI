/*
 * Created by qmarciset on 12/10/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.ui

import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.databinding.ViewDataBinding
import androidx.navigation.NavDirections
import androidx.navigation.findNavController
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.activity.mainactivity.MainActivity

fun View.setOnNavigationClickListener(viewDataBinding: ViewDataBinding, action: NavDirections) {
    this.enableLink()
    this.setOnSingleClickListener {
        (viewDataBinding.root.context as? MainActivity)?.currentNavigationFragment?.setSharedAxisZExitTransition()
        viewDataBinding.root.findNavController().navigate(action)
    }
}

fun View.disableLink() {
    when (this) { // Button first has a button is also a TextView
        is Button -> this.isEnabled = false
        is TextView -> this.setTextColor(
            ContextCompat.getColor(
                this.context,
                android.R.color.darker_gray
            )
        )
    }
}

fun View.enableLink() {
    when (this) { // Button first has a button is also a TextView
        is Button -> this.isEnabled = true
        is TextView -> this.setTextColor(
            ContextCompat.getColor(
                this.context,
                R.color.relation_link
            )
        )
    }
}
