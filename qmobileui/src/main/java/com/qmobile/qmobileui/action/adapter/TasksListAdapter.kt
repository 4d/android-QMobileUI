/*
 * Created by htemanni on 1/6/2022.
 * 4D SAS
 * Copyright (c) 2022 htemanni. All rights reserved.
 */

package com.qmobile.qmobileui.action.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.qmobile.qmobiledatastore.dao.ActionTask
import com.qmobile.qmobiledatastore.dao.STATUS
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.action.model.TaskItemTypeEnum
import com.qmobile.qmobileui.action.viewholder.SectionViewHolder
import com.qmobile.qmobileui.action.viewholder.TaskListViewHolder
import com.qmobile.qmobileui.action.viewholder.TaskViewHolder

class TasksListAdapter(
    context: Context,
    var list: MutableList<ActionTask?>,
    var serverStatus: String? = null,
    val onCLick: (position: Int) -> Unit
) :
    RecyclerView.Adapter<TaskListViewHolder>() {

    private val context: Context = context

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskListViewHolder {
        val inflater = LayoutInflater.from(context)

        return when (TaskItemTypeEnum.values()[viewType]) {
            TaskItemTypeEnum.HEADER_PENDING, TaskItemTypeEnum.HEADER_HISTORY -> SectionViewHolder(
                inflater.inflate(R.layout.item_section, parent, false)
            )
            TaskItemTypeEnum.TASK -> TaskViewHolder(
                inflater.inflate(
                    R.layout.item_task,
                    parent,
                    false
                )
            )
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: TaskListViewHolder, position: Int) {
        val item = list[position]

        if (holder is TaskViewHolder) {
            if (item != null) {
                holder.bind(
                    item
                ) {
                    if (!item.actionInfo.allParameters.isNullOrEmpty() && item.status == STATUS.PENDING) {
                        onCLick(position)
                    }
                }
            }
        } else {
            if (position == 0) {
                (holder as SectionViewHolder).bind(TaskItemTypeEnum.HEADER_PENDING, serverStatus.orEmpty()) {
                    onCLick(position)
                }
            } else {
                (holder as SectionViewHolder).bind(TaskItemTypeEnum.HEADER_HISTORY, serverStatus.orEmpty())
            }
        }
    }

    override fun getItemViewType(position: Int): Int {

        if (list[position] == null) {
            return if (position == 0) { // header pending
                TaskItemTypeEnum.HEADER_PENDING.ordinal
            } else { // header history
                TaskItemTypeEnum.HEADER_HISTORY.ordinal
            }
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
        return (position > -1) && list[position]?.status == STATUS.PENDING
    }
}
