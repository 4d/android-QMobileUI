/*
 * Created by qmarciset on 18/2/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.action.viewholders

import android.view.View
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.action.Action

class SignatureViewHolder(
    itemView: View,
    private val actionTypesCallback: (actionTypes: Action.Type, position: Int) -> Unit
) : BaseImageViewHolder(itemView) {

    override fun getPlaceholderRes(): Int = R.drawable.file_sign

    override fun onImageClick() {
        actionTypesCallback(Action.Type.SIGN, bindingAdapterPosition)
    }
}
