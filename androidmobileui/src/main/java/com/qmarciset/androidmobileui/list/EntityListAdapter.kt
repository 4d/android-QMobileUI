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
import com.qmarciset.androidmobileui.utils.FromTableInterface
import com.qmarciset.androidmobileui.utils.NavigationInterface

class EntityListAdapter internal constructor(
    private val tableName: String,
    private val fromTableInterface: FromTableInterface,
    private val navigationInterface: NavigationInterface
) :
    RecyclerView.Adapter<BaseViewHolder>() {

    private var entities = emptyList<EntityModel>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val dataBinding: ViewDataBinding =
            DataBindingUtil.inflate(
                inflater,
                fromTableInterface.itemLayoutFromTable(tableName),
                parent,
                false
            )
        return BaseViewHolder(dataBinding, tableName, navigationInterface)
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        val current = entities[position]
        holder.bind(current, position)
    }

    override fun getItemCount() = entities.size

    @Suppress("UNCHECKED_CAST")
    internal fun setEntities(entities: List<*>) {
        this.entities = entities as List<EntityModel>
        notifyDataSetChanged()
    }

    fun getEntities(): List<EntityModel> {
        return entities
    }
}
