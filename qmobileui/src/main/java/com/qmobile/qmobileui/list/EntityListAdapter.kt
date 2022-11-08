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
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobileui.utils.ReflectionUtils
import com.qmobile.qmobileui.utils.ResourcesHelper

class EntityListAdapter internal constructor(
    private val tableName: String,
    private val lifecycleOwner: LifecycleOwner,
    private val onItemClick: (ViewDataBinding, String, Int) -> Unit,
    private val onItemLongClick: (RoomEntity) -> Unit
) :
    PagingDataAdapter<RoomEntity, ListItemViewHolder>(DIFF_CALLBACK) {

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<RoomEntity>() {
            // The ID property identifies when items are the same.
            override fun areItemsTheSame(oldItem: RoomEntity, newItem: RoomEntity) =
                (oldItem.__entity as? EntityModel)?.__KEY == (newItem.__entity as? EntityModel)?.__KEY

            // If you use the "==" operator, make sure that the object implements
            // .equals(). Alternatively, write custom data comparison logic here.
            override fun areContentsTheSame(
                oldItem: RoomEntity,
                newItem: RoomEntity
            ) = (oldItem.__entity as? EntityModel)?.__STAMP == (newItem.__entity as? EntityModel)?.__STAMP &&
                BaseApp.genericRelationHelper.relationsEquals(oldItem, newItem)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListItemViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val dataBinding: ViewDataBinding =
            DataBindingUtil.inflate(
                inflater,
                ResourcesHelper.itemLayoutFromTable(parent.context, tableName),
                parent,
                false
            )
        dataBinding.lifecycleOwner = this@EntityListAdapter.lifecycleOwner
        return ListItemViewHolder(dataBinding, tableName, onItemClick, onItemLongClick)
    }

    override fun onBindViewHolder(holder: ListItemViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    fun getSelectedItem(position: Int): RoomEntity? {
        return getItem(position)
    }

    fun getSectionCallback(sectionField: String): RecyclerSectionItemDecoration.SectionCallback {
        return object : RecyclerSectionItemDecoration.SectionCallback {
            override fun isSection(position: Int): Boolean {
                if (position > 0) {
                    val section1 =
                        getItem(position - 1)?.let { ReflectionUtils.getInstanceProperty(it, sectionField) }
                    val section2 =
                        getItem(position)?.let { ReflectionUtils.getInstanceProperty(it, sectionField) }
                    return section1 != section2
                }
                return (position == 0 && itemCount > 0)
            }

            override fun getSectionHeader(position: Int): CharSequence {
                val section1 =
                    getItem(position)?.let { ReflectionUtils.getInstanceProperty(it, sectionField) }
                        ?: ""
                return section1.toString()
            }
        }
    }
}
