/*
 * Created by qmarciset on 3/6/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.action.pendingtasks.viewholder

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.qmobile.qmobiledatastore.dao.ActionTask
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.action.pendingtasks.TaskListViewHolder
import com.qmobile.qmobileui.action.utils.DotProgressBar
import com.qmobile.qmobileui.action.utils.getDayWord
import com.qmobile.qmobileui.action.utils.getHourWord
import com.qmobile.qmobileui.action.utils.getMinuteWord
import com.qmobile.qmobileui.action.utils.getSecondWord
import java.util.Date

const val MILLISECONDS_IN_SECOND = 1000
const val SECONDS_IN_MINUTE = 60
const val MINUTES_IN_HOUR = 60
const val HOURS_IN_DAY = 24

class TaskViewHolder(itemView: View) : TaskListViewHolder(itemView) {
    private var label: TextView = itemView.findViewById(R.id.label)
    private var status: TextView = itemView.findViewById(R.id.status)
    private var date: TextView = itemView.findViewById(R.id.date)
    private var icon: ImageView = itemView.findViewById(R.id.icon_state)
    private var dotProgressBar: DotProgressBar = itemView.findViewById(R.id.dot_progress_bar)

    fun bind(
        item: ActionTask,
        onClick: () -> Unit
    ) {
        itemView.setOnClickListener {
            onClick()
        }
        label.text = item.label
        when (item.status) {
            ActionTask.Status.SUCCESS -> {
                if (!item.message.isNullOrEmpty()) {
                    status.visibility = View.VISIBLE
                    status.text = item.message
                }
                icon.setImageResource(R.drawable.check_circle)
                dotProgressBar.visibility = View.INVISIBLE
                icon.visibility = View.VISIBLE
            }
            ActionTask.Status.ERROR_SERVER -> {
                icon.setImageResource(R.drawable.alert_circle)
                dotProgressBar.visibility = View.INVISIBLE
                icon.visibility = View.VISIBLE
                if (!item.message.isNullOrEmpty()) {
                    status.visibility = View.VISIBLE
                    status.text = item.message
                }
            }
            else -> {
                status.visibility = View.GONE
                icon.visibility = View.INVISIBLE
                dotProgressBar.visibility = View.VISIBLE
            }
        }
        date.text = getRelatedDate(item.date)
    }

    private fun getRelatedDate(date: Date): String {
        val diff: Long = Date().time - date.time
        val seconds = diff / MILLISECONDS_IN_SECOND
        val minutes = seconds / SECONDS_IN_MINUTE
        val hours = minutes / MINUTES_IN_HOUR
        val days = hours / HOURS_IN_DAY

        return when {
            days > 0 -> "$days ${getDayWord(days)} ago"
            hours > 0 -> "$hours ${getHourWord(hours)} ago"
            minutes > 0 -> "$minutes ${getMinuteWord(minutes)} ago"
            seconds > 0 -> "$seconds ${getSecondWord(seconds)} ago"
            else -> ""
        }
    }
}
