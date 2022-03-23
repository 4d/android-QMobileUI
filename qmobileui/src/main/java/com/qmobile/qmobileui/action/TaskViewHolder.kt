package com.qmobile.qmobileui.action

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.example.dotprogressbar.DotProgressBar
import com.qmobile.qmobiledatastore.dao.ActionTask
import com.qmobile.qmobiledatastore.dao.STATUS
import com.qmobile.qmobileui.R
import java.util.Date

const val MILLISECONDS_IN_SECOND = 1000
const val SECONDS_IN_MINUTE = 60
const val MINUTES_IN_HOUR = 60
const val HOURS_IN_DAY =24

class TaskViewHolder(itemView: View) : TaskListViewHolder(itemView) {
    var label: TextView = itemView.findViewById(R.id.label)
    var status: TextView = itemView.findViewById(R.id.status)
    var date: TextView = itemView.findViewById(R.id.date)
    var icon: ImageView = itemView.findViewById(R.id.icon_state)
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
            STATUS.SUCCESS -> {
                if (!item.message.isNullOrEmpty()) {
                    status.visibility = View.VISIBLE
                    status.text = item.message
                }
                icon.setImageResource(R.drawable.check_circle)
                dotProgressBar.visibility = View.INVISIBLE
                icon.visibility = View.VISIBLE
            }
            STATUS.ERROR_SERVER -> {
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
            days > 0 -> "$days days ago"
            hours > 0 -> "$hours hours ago"
            minutes > 0 -> "$minutes minutes ago"
            seconds > 0 -> "$seconds seconds ago"
            else -> ""
        }
    }
}



