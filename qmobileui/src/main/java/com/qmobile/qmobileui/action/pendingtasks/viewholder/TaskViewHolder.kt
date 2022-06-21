/*
 * Created by qmarciset on 3/6/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.action.pendingtasks.viewholder

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.qmobile.qmobileapi.utils.getJSONObjectList
import com.qmobile.qmobileapi.utils.getSafeString
import com.qmobile.qmobiledatastore.dao.ActionTask
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.action.actionparameters.ActionParameterEnum
import com.qmobile.qmobileui.action.actionparameters.viewholder.AM_KEY
import com.qmobile.qmobileui.action.actionparameters.viewholder.PM_KEY
import com.qmobile.qmobileui.action.model.Action
import com.qmobile.qmobileui.action.pendingtasks.TaskListViewHolder
import com.qmobile.qmobileui.action.utils.ActionHelper
import com.qmobile.qmobileui.action.utils.DotProgressBar
import com.qmobile.qmobileui.action.utils.getDayWord
import com.qmobile.qmobileui.action.utils.getHourWord
import com.qmobile.qmobileui.action.utils.getMinuteWord
import com.qmobile.qmobileui.action.utils.getSecondWord
import com.qmobile.qmobileui.formatters.FormatterUtils
import java.util.Date

const val MILLISECONDS_IN_SECOND = 1000
const val SECONDS_IN_MINUTE = 60
const val SECONDS_IN_HOUR = 3600
const val MINUTES_IN_HOUR = 60
const val HOURS_IN_DAY = 24
const val HOURS_IN_MID_DAY_FORMAT = 12

class TaskViewHolder(itemView: View) : TaskListViewHolder(itemView) {

    private val label: TextView = itemView.findViewById(R.id.label)
    private val status: TextView = itemView.findViewById(R.id.status)
    private val tableName: TextView = itemView.findViewById(R.id.tableName)
    private val date: TextView = itemView.findViewById(R.id.date)
    private val icon: ImageView = itemView.findViewById(R.id.icon_state)
    private val dotProgressBar: DotProgressBar = itemView.findViewById(R.id.dot_progress_bar)

    fun bind(
        isFromSettings: Boolean,
        item: ActionTask,
        onClick: () -> Unit
    ) {
        itemView.setOnClickListener { onClick() }

        label.text = item.label

        if (isFromSettings)
            tableName.text = item.actionInfo.tableName
        else
            tableName.visibility = View.GONE

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
                showItemDetails(item)
                icon.visibility = View.INVISIBLE
                dotProgressBar.visibility = View.VISIBLE
            }
        }
        date.text = getRelatedDate(item.date)
    }

    @Suppress("NestedBlockDepth")
    private fun showItemDetails(item: ActionTask) {
        status.visibility = View.VISIBLE
        item.actionInfo.paramsToSubmit?.let { paramsToSubmit ->
            val sb = StringBuilder()
            // get all parameters for related action of this task to check the type/format of each paramToSubmit
            val relatedActionParameters = retrieveAction(item).parameters.getJSONObjectList()
            paramsToSubmit.entries.forEach { entry ->
                val relatedParam = relatedActionParameters.find {
                    it.getSafeString("name") == entry.key
                }
                val type = relatedParam?.getSafeString("type")
                val format = relatedParam?.getSafeString("format")
                // We don't display password fields
                if (format != ActionParameterEnum.TEXT_PASSWORD.format) {
                    val stringToAppend = when (type) {
                        "date" -> {
                            FormatterUtils.applyFormat("shortDate", entry.value)
                        }
                        "time" -> {
                            entry.value.toString().toDoubleOrNull()?.let { numberOfSeconds ->
                                val hours: Int = (numberOfSeconds / SECONDS_IN_HOUR).toInt()
                                val minutes: Int = (numberOfSeconds % SECONDS_IN_HOUR / SECONDS_IN_MINUTE).toInt()
                                if (format == "duration") {
                                    "$hours hours $minutes minutes"
                                } else {
                                    if (hours >= HOURS_IN_MID_DAY_FORMAT)
                                        "${hours - HOURS_IN_MID_DAY_FORMAT}:$minutes $PM_KEY"
                                    else
                                        "$hours:$minutes $AM_KEY"
                                }
                            }
                        }
                        else -> {
                            entry.value.toString()
                        }
                    }
                    stringToAppend?.let {
                        if (it.isNotEmpty())
                            sb.append("$stringToAppend , ")
                    }
                }
            }

            if (sb.toString().isNotEmpty())
                status.text = sb.removeSuffix(" , ")
        }
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
            seconds == 0L -> "1 ${getSecondWord(1)} ago" // case of click on error server task without
            // parameters , show always one second ago(instead of 0)
            else -> ""
        }
    }

    private fun retrieveAction(task: ActionTask): Action {
        val tableName = task.actionInfo.tableName
        val actionUUID = task.actionInfo.actionUUID
        ActionHelper.getActionObjectList(BaseApp.runtimeDataHolder.tableActions, tableName)
            .plus(ActionHelper.getActionObjectList(BaseApp.runtimeDataHolder.currentRecordActions, tableName))
            .forEach { action ->
                // create id with pattern: $actionName$tableName
                val actionId = action.getSafeString("name") + tableName
                if (actionUUID == actionId)
                    return ActionHelper.createActionFromJsonObject(action)
            }
        throw Action.ActionException("Couldn't find action from table [$tableName], with uuid [$actionUUID]")
    }
}
