/*
 * Created by qmarciset on 11/7/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.action.actionparameters.viewholders

import android.view.View
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.action.model.Action

class SignatureViewHolder(
    itemView: View,
    format: String,
    private val actionTypesCallback: (actionTypes: Action.Type, position: Int) -> Unit
) : BaseImageViewHolder(itemView, format) {

    override fun getPlaceholderRes(): Int = R.drawable.file_sign

    override fun onImageClick() {
        actionTypesCallback(Action.Type.SIGN, bindingAdapterPosition)
    }
}
