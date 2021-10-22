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
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import com.qmobile.qmobileapi.model.entity.EntityModel
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobileui.list.viewholder.BaseViewHolder
import com.qmobile.qmobileui.utils.ResourcesHelper

class EntityListAdapter internal constructor(
    private val tableName: String,
    private val lifecycleOwner: LifecycleOwner,
    private val actionDialogClickedCallBack: (String?) -> Unit
) :
    PagedListAdapter<EntityModel, BaseViewHolder>(DIFF_CALLBACK) {

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<EntityModel>() {
            // The ID property identifies when items are the same.
            override fun areItemsTheSame(oldItem: EntityModel, newItem: EntityModel) =
                oldItem.__KEY == newItem.__KEY

            // If you use the "==" operator, make sure that the object implements
            // .equals(). Alternatively, write custom data comparison logic here.
            override fun areContentsTheSame(
                oldItem: EntityModel,
                newItem: EntityModel
            ) = oldItem.__KEY == newItem.__KEY
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val dataBinding: ViewDataBinding =
            DataBindingUtil.inflate(
                inflater,
                ResourcesHelper.layoutFromTable(
                    parent.context,
                    "${ResourcesHelper.RV_ITEM_PREFIX}_$tableName".lowercase()
                ),
                parent,
                false
            )
        dataBinding.lifecycleOwner = this@EntityListAdapter.lifecycleOwner
        return BaseViewHolder(dataBinding, tableName)
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        getItem(position).let { entity ->
            holder.bind(entity, position)
            holder.itemView.setOnLongClickListener {
                actionDialogClickedCallBack(entity?.__KEY)
                true
            }
            // unbind because of issue : item at position 11 receives binding of at item 0,
            // item at position 12 receives binding of item at position 1, etc.
            holder.unbindRelations()
            entity?.let {
                setupObserver(entity, holder, position)
            }
        }
    }

    private fun setupObserver(entity: EntityModel, holder: BaseViewHolder, position: Int) {
        BaseApp.genericTableHelper.getManyToOneRelationsInfo(tableName, entity).let { relationMap ->
            if (relationMap.isNotEmpty()) {
                holder.observeRelations(relationMap, position)
            }
        }
    }

    fun getSelectedItem(position: Int): EntityModel? {
        return getItem(position)
    }
}
