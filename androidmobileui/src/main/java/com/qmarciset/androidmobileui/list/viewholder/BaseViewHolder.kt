/*
 * Created by Quentin Marciset on 7/2/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmarciset.androidmobileui.list.viewholder

import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView
import com.qmarciset.androidmobiledatasync.app.BaseApp
import com.qmarciset.androidmobileui.BR

class BaseViewHolder(
    private val dataBinding: ViewDataBinding,
    private val tableName: String
) :
    RecyclerView.ViewHolder(dataBinding.root) {

    // Applies DataBinding
    fun bind(entity: Any, position: Int) {
        dataBinding.setVariable(BR.entityData, entity)
        dataBinding.executePendingBindings()
        itemView.setOnClickListener {
            BaseApp.navigationInterface.navigateFromListToViewPager(
                dataBinding.root,
                position,
                tableName
            )
        }
    }
}
