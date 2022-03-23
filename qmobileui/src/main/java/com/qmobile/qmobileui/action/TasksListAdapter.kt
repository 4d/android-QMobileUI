package com.qmobile.qmobileui.action

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.qmobile.qmobiledatastore.dao.ActionTask
import com.qmobile.qmobiledatastore.dao.STATUS
import com.qmobile.qmobileui.R

class TasksListAdapter(
    context: Context,
    var list: MutableList<ActionTask?>,
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
        if (holder is TaskViewHolder) {
            val item = list[position]
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
                (holder as SectionViewHolder).bind(TaskItemTypeEnum.HEADER_PENDING)
            } else {
                (holder as SectionViewHolder).bind(TaskItemTypeEnum.HEADER_HISTORY)
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
