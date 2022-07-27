/*
 * Created by qmarciset on 20/7/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.action.pendingtasks

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.qmobile.qmobiledatastore.dao.ActionTask
import com.qmobile.qmobileui.action.pendingtasks.viewholder.BaseTaskViewHolder
import com.qmobile.qmobileui.action.pendingtasks.viewholder.CompletedTaskViewHolder
import com.qmobile.qmobileui.action.pendingtasks.viewholder.PendingTaskViewHolder
import com.qmobile.qmobileui.databinding.ItemCompletedTaskBinding
import com.qmobile.qmobileui.databinding.ItemPendingTaskBinding

class TaskListAdapter(
    private val isFromSettings: Boolean,
    private val type: ActionTask.Status,
    private val onItemClick: (actionTask: ActionTask) -> Unit
) :
    RecyclerView.Adapter<BaseTaskViewHolder>() {

    private var tasks: MutableList<ActionTask> = mutableListOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseTaskViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (type == ActionTask.Status.PENDING) {
            val dataBinding = ItemPendingTaskBinding.inflate(inflater, parent, false)
            PendingTaskViewHolder(dataBinding, onItemClick)
        } else {
            val dataBinding = ItemCompletedTaskBinding.inflate(inflater, parent, false)
            CompletedTaskViewHolder(dataBinding, onItemClick)
        }
    }

    override fun onBindViewHolder(holder: BaseTaskViewHolder, position: Int) {
        holder.bind(tasks[position], isFromSettings)
    }

    override fun getItemCount(): Int = tasks.count()

    fun updateItems(items: List<ActionTask>?) {
        tasks = items?.toMutableList() ?: mutableListOf()
        notifyDataSetChanged()
    }

    fun getItemByPosition(position: Int): ActionTask? = tasks.getOrNull(position)
}
