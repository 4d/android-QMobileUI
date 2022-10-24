/*
 * Created by qmarciset on 22/7/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.action.pendingtasks.viewholder

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.qmobile.qmobiledatastore.dao.ActionTask
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.formatters.TimeFormat
import com.qmobile.qmobileui.ui.setOnSingleClickListener

abstract class BaseTaskViewHolder(
    view: View,
    private val onItemClick: (actionTask: ActionTask) -> Unit
) : RecyclerView.ViewHolder(view) {

    private val label: TextView = itemView.findViewById(R.id.label)
    private val tableName: TextView = itemView.findViewById(R.id.tableName)
    private val date: TextView = itemView.findViewById(R.id.date)

    open fun bind(actionTask: ActionTask, isFromSettings: Boolean) {
        itemView.setOnSingleClickListener { onItemClick(actionTask) }

        label.text = actionTask.label

        if (isFromSettings) {
            tableName.text = actionTask.actionInfo.tableName
        } else {
            tableName.visibility = View.GONE
        }

        date.text = TimeFormat.getElapsedTime(actionTask.date)
    }
}
