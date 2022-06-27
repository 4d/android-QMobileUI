/*
 * Created by qmarciset on 3/6/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.action.pendingtasks

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.qmobile.qmobiledatastore.dao.ActionTask
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.action.pendingtasks.viewholder.SectionViewHolder
import com.qmobile.qmobileui.action.pendingtasks.viewholder.TaskItemTypeEnum
import com.qmobile.qmobileui.action.pendingtasks.viewholder.TaskViewHolder

class TasksListAdapter(
    private val isFromSettings: Boolean,
    private val context: Context,
    var list: MutableList<ActionTask?>,
    var serverStatus: String? = null,
    val onCLick: (position: Int) -> Unit
) :
    RecyclerView.Adapter<TaskListViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskListViewHolder {
        val inflater = LayoutInflater.from(context)

        return when (TaskItemTypeEnum.values()[viewType]) {
            TaskItemTypeEnum.HEADER_PENDING, TaskItemTypeEnum.HEADER_HISTORY -> SectionViewHolder(
                inflater.inflate(R.layout.item_section, parent, false)
            )
            TaskItemTypeEnum.TASK -> TaskViewHolder(inflater.inflate(R.layout.item_task, parent, false))
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: TaskListViewHolder, position: Int) {
        val item = list[position]

        when (holder) {
            is TaskViewHolder -> {
                if (item != null)
                    holder.bind(isFromSettings, item) {
                        onCLick(position)
                    }
            }
            is SectionViewHolder -> {
                if (position == 0)
                    holder.bind(TaskItemTypeEnum.HEADER_PENDING, serverStatus.orEmpty()) {
                        onCLick(position)
                    }
                else
                    holder.bind(TaskItemTypeEnum.HEADER_HISTORY, serverStatus.orEmpty())
            }
        }
    }

    override fun getItemViewType(position: Int): Int {

        if (list[position] == null) {
            return if (position == 0) // header pending
                TaskItemTypeEnum.HEADER_PENDING.ordinal
            else // header history
                TaskItemTypeEnum.HEADER_HISTORY.ordinal
        }
        return TaskItemTypeEnum.TASK.ordinal
    }

    fun removeAt(position: Int) {
        list.removeAt(position)
        notifyItemRemoved(position)
    }

    fun getItemByPosition(position: Int): ActionTask? {
        return list[position]
    }

    fun isItemDeletable(position: Int): Boolean {
        return (position > -1) && list[position]?.status == ActionTask.Status.PENDING
    }

    fun setStatus(status: String){
        serverStatus = status
        notifyItemChanged(0)
    }
}
