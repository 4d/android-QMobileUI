/*
 * Created by qmarciset on 3/6/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.action.pendingtasks.viewholder

import android.view.View
import android.widget.TextView
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.action.pendingtasks.TaskListViewHolder

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
