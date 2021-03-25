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
import androidx.lifecycle.LiveData
import androidx.recyclerview.widget.RecyclerView
import com.qmobile.qmobileapi.model.entity.EntityModel
import com.qmobile.qmobiledatastore.data.RoomRelation
import com.qmobile.qmobileui.list.viewholder.BaseViewHolder
import com.qmobile.qmobileui.model.QMobileUiConstants
import com.qmobile.qmobileui.utils.layoutFromTable
import java.util.Locale

class EntityListAdapter internal constructor(
    private val tableName: String,
    private val lifecycleOwner: LifecycleOwner
) :
    RecyclerView.Adapter<BaseViewHolder>() {

    private var entities = emptyList<EntityModel>()

    // Map<entityKey, Map<relationName, LiveData<RoomRelation>>>
    private var relationMap = mutableMapOf<String, MutableMap<String, LiveData<RoomRelation>>>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val dataBinding: ViewDataBinding =
            DataBindingUtil.inflate(
                inflater,
                layoutFromTable(
                    parent.context,
                    "${QMobileUiConstants.Prefix.RECYCLER_PREFIX}$tableName".toLowerCase(Locale.getDefault())
                ),
                parent,
                false
            )
        dataBinding.lifecycleOwner = this@EntityListAdapter.lifecycleOwner
        return BaseViewHolder(dataBinding, tableName)
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        val current = entities[position]
        holder.bind(current, position)
        relationMap[current.__KEY]?.let { relations ->
            holder.observeRelations(relations, position)
        }
    }

    override fun getItemCount() = entities.size

    internal fun setEntities(
        entities: List<EntityModel>,
        relationMap: MutableMap<String, MutableMap<String, LiveData<RoomRelation>>>
    ) {
        this.entities = entities
        this.relationMap = relationMap
        notifyDataSetChanged()
    }

    fun getEntities(): List<EntityModel> {
        return entities
    }
}
