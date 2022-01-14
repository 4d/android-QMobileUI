/*
 * Created by qmarciset on 11/1/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.action.viewholders

import android.view.View

class ImageViewHolder(itemView: View, hideKeyboardCallback: () -> Unit) :
    BaseViewHolder(itemView, hideKeyboardCallback) {
    override fun validate(displayError: Boolean): Boolean {
        return true
    }

    override fun getInputType(format: String): Int {
        return -1
    }
}
