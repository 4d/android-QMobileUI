/*
 * Created by qmarciset on 18/2/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.action.viewholders

import android.view.View
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.action.ActionTypes

class SignatureViewHolder(
    itemView: View,
    hideKeyboardCallback: () -> Unit,
    private val actionTypesCallback: (actionTypes: ActionTypes, position: Int) -> Unit
) : BaseImageViewHolder(itemView, hideKeyboardCallback) {

    override fun getPlaceholderRes(): Int = R.drawable.file_sign

    override fun onImageClick() {
        actionTypesCallback(ActionTypes.SIGN, bindingAdapterPosition)
    }
}
