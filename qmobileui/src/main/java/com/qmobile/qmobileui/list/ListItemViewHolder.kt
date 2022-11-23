/*
 * Created by Quentin Marciset on 7/2/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.list

import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView
import com.qmobile.qmobiledatastore.data.RoomEntity
import com.qmobile.qmobiledatasync.relation.RelationHelper
import com.qmobile.qmobileui.BR
import com.qmobile.qmobileui.ui.setOnSingleClickListener

class ListItemViewHolder(
    private val dataBinding: ViewDataBinding,
    private val tableName: String,
    private val onItemClick: (ViewDataBinding, Int) -> Unit,
    private val onItemLongClick: (RoomEntity) -> Unit
) :
    RecyclerView.ViewHolder(dataBinding.root) {

    // Applies DataBinding
    fun bind(roomEntity: RoomEntity?) {
        roomEntity?.let {
            dataBinding.setVariable(BR.entityData, roomEntity)
            dataBinding.executePendingBindings()

            setupClickListeners(roomEntity)
            RelationHelper.setupRelationNavigation(tableName, dataBinding, roomEntity)
        }
    }

    private fun setupClickListeners(roomEntity: RoomEntity) {
        itemView.setOnSingleClickListener {
            onItemClick(dataBinding, bindingAdapterPosition)
        }
        itemView.setOnLongClickListener {
            onItemLongClick(roomEntity)
            true
        }
    }
}
