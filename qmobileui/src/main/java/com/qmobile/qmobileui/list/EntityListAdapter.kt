/*
 * Created by Quentin Marciset on 7/2/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.list

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView
import com.qmobile.qmobileapi.model.entity.EntityModel
import com.qmobile.qmobileui.list.viewholder.BaseViewHolder
import com.qmobile.qmobileui.model.QMobileUiConstants
import com.qmobile.qmobileui.utils.layoutFromTable
import java.util.Locale

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
                layoutFromTable(
                    parent.context,
                    "${QMobileUiConstants.RECYCLER_PREFIX}$tableName".toLowerCase(Locale.getDefault())
                ),
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
        Log.w("TEST", ">>> ${entities[0].__TIMESTAMP}")
        Log.w("TEST", ">>> ${entities[1].__KEY}")
        notifyDataSetChanged()
    }

    fun getEntities(): List<EntityModel> {
        return entities
    }
}
