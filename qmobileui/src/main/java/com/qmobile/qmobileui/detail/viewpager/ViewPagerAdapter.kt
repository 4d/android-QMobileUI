/*
 * Created by qmarciset on 27/10/2021.
 * 4D SAS
 * Copyright (c) 2021 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.detail.viewpager

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.qmobile.qmobileapi.model.entity.EntityModel
import com.qmobile.qmobiledatastore.data.RoomEntity
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobileui.BR
import com.qmobile.qmobileui.utils.ResourcesHelper

class ViewPagerAdapter(fragment: Fragment, private val tableName: String) :
    PagedListPagerAdapter<RoomEntity>(fragment) {

    override var isSmoothScroll = true

    override fun createItem(position: Int): Fragment {
        var itemId = "0"
        getValue(position)?.let { roomEntity ->
            (roomEntity.__entity as EntityModel?)?.__KEY?.let { itemId = it }
        }
        return BaseApp.genericTableFragmentHelper.getDetailFragment(tableName).apply {
            arguments = Bundle().apply {
                putString("itemId", itemId)
                putString("tableName", tableName)
            }
        }
    }
}

class ViewPagerAdapter2 internal constructor(
    private val tableName: String,
    private val lifecycleOwner: LifecycleOwner
) :
    PagingDataAdapter<RoomEntity, ViewPagerAdapter2.BaseViewHolder2>(DIFF_CALLBACK) {

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
            ) = (oldItem.__entity as EntityModel?)?.__STAMP == (newItem.__entity as EntityModel?)?.__STAMP &&
                BaseApp.genericRelationHelper.relationsEquals(oldItem, newItem)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder2 {
        val inflater = LayoutInflater.from(parent.context)
        val dataBinding: ViewDataBinding =
            DataBindingUtil.inflate(
                inflater,
                ResourcesHelper.detailLayoutFromTable(parent.context, tableName),
                parent,
                false
            )
        dataBinding.lifecycleOwner = this@ViewPagerAdapter2.lifecycleOwner
        return BaseViewHolder2(dataBinding)
    }

    override fun onBindViewHolder(holder: BaseViewHolder2, position: Int) {
        holder.bind(getItem(position))
    }

    inner class BaseViewHolder2(
        private val dataBinding: ViewDataBinding
    ) :
        RecyclerView.ViewHolder(dataBinding.root) {

        // Applies DataBinding
        fun bind(entity: RoomEntity?) {
            entity?.let {
                dataBinding.setVariable(BR.entityData, entity)
                dataBinding.executePendingBindings()
            }
        }
    }
}
