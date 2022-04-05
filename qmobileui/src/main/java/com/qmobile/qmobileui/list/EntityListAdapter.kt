/*
 * Created by Quentin Marciset on 7/2/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.list

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.LifecycleOwner
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import com.qmobile.qmobileapi.model.entity.EntityModel
import com.qmobile.qmobiledatastore.data.RoomEntity
import com.qmobile.qmobileui.list.viewholder.BaseViewHolder
import com.qmobile.qmobileui.utils.ResourcesHelper

class EntityListAdapter internal constructor(
    private val tableName: String,
    private val lifecycleOwner: LifecycleOwner,
    private val onItemClick: (ViewDataBinding, String) -> Unit,
    private val onItemLongClick: (RoomEntity) -> Unit
) :
    PagingDataAdapter<RoomEntity, BaseViewHolder>(DIFF_CALLBACK) {

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<RoomEntity>() {
            // The ID property identifies when items are the same.
            override fun areItemsTheSame(oldItem: RoomEntity, newItem: RoomEntity) =
                (oldItem.__entity as EntityModel?)?.__KEY == (newItem.__entity as EntityModel?)?.__KEY

            // If you use the "==" operator, make sure that the object implements
            // .equals(). Alternatively, write custom data comparison logic here.
            override fun areContentsTheSame(
                oldItem: RoomEntity,
                newItem: RoomEntity
            ) = (oldItem.__entity as EntityModel?)?.__STAMP == (newItem.__entity as EntityModel?)?.__STAMP
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val dataBinding: ViewDataBinding =
            DataBindingUtil.inflate(
                inflater,
                ResourcesHelper.itemLayoutFromTable(parent.context, tableName),
                parent,
                false
            )
        dataBinding.lifecycleOwner = this@EntityListAdapter.lifecycleOwner
        return BaseViewHolder(dataBinding, tableName, onItemClick, onItemLongClick)
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    fun getSelectedItem(position: Int): RoomEntity? {
        return getItem(position)
    }
}
