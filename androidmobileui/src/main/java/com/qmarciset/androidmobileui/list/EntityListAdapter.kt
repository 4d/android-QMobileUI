/*
 * Created by Quentin Marciset on 7/2/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmarciset.androidmobileui.list

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView
import com.qmarciset.androidmobileapi.model.entity.EntityModel
import com.qmarciset.androidmobileui.list.viewholder.BaseViewHolder
import com.qmarciset.androidmobileui.utils.itemLayoutFromTable

class EntityListAdapter internal constructor(
    private val tableName: String
) :
    RecyclerView.Adapter<BaseViewHolder>() {

    private var entities = emptyList<EntityModel>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val dataBinding: ViewDataBinding =
            DataBindingUtil.inflate(
                inflater,
                itemLayoutFromTable(parent.context, tableName),
                parent,
                false
            )
        return BaseViewHolder(dataBinding, tableName)
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        val current = entities[position]
        holder.bind(current, position)
    }

    override fun getItemCount() = entities.size

    internal fun setEntities(entities: List<EntityModel>) {
        this.entities = entities
        notifyDataSetChanged()
    }

    fun getEntities(): List<EntityModel> {
        return entities
    }
}
