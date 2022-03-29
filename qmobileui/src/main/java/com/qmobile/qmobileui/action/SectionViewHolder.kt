package com.qmobile.qmobileui.action

import android.view.View
import android.widget.TextView
import com.qmobile.qmobileui.R

class SectionViewHolder(itemView: View) : TaskListViewHolder(itemView) {
    var label: TextView = itemView.findViewById(R.id.label)
    fun bind(
        typeEnum: TaskItemTypeEnum,
        serverStatus: String,
        onClick: (() -> Unit)? = null
    ) {
        if (typeEnum == TaskItemTypeEnum.HEADER_HISTORY) {
            label.text = itemView.context.getString(R.string.task_history_section_title)
        } else {
            label.text =
                "${itemView.context.getString(R.string.task_pending_section_title)} ($serverStatus)"
            label.setOnClickListener {
                onClick?.let { it1 -> it1() }
            }
        }
    }
}
