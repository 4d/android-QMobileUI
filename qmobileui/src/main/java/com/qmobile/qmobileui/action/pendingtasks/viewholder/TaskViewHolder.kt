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
import com.qmobile.qmobileui.action.model.Action
import com.qmobile.qmobileui.action.pendingtasks.TaskListViewHolder
import com.qmobile.qmobileui.action.utils.ActionHelper
import com.qmobile.qmobileui.action.utils.DateTimeHelper
import com.qmobile.qmobileui.action.utils.DotProgressBar
import com.qmobile.qmobileui.formatters.FormatterUtils
import org.json.JSONObject

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

        if (isFromSettings) {
            tableName.text = item.actionInfo.tableName
        } else {
            tableName.visibility = View.GONE
        }

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
        date.text = DateTimeHelper.getFormattedDate(item.date)
    }

    private fun showItemDetails(item: ActionTask) {
        status.visibility = View.VISIBLE
        var sb = StringBuilder()
        // get all parameters for related action of this task to check the type/format of each paramToSubmit
        val relatedActionParameters: List<JSONObject> = retrieveAction(item).parameters.getJSONObjectList()
        item.actionInfo.paramsToSubmit?.entries?.forEach { entry ->
            relatedActionParameters.find { it.getSafeString("name") == entry.key }?.let { relatedParam ->
                val type = relatedParam.getSafeString("type")
                val format = relatedParam.getSafeString("format")
                // We don't display password fields
                if (format != ActionParameterEnum.TEXT_PASSWORD.format) {
                    sb = getFieldOverview(format, type, entry.value, sb)
                }
            }

            if (sb.toString().isNotEmpty()) {
                status.text = sb.removeSuffix(" , ")
            }
        }
    }

    private fun getFieldOverview(
        format: String?,
        type: String?,
        value: Any,
        sb: StringBuilder
    ): StringBuilder {
        val stringToAppend = when (type) {
            "date" -> {
                FormatterUtils.applyFormat("shortDate", value)
            }
            "time" -> {
                value.toString().toDoubleOrNull()?.let { numberOfMilliSeconds ->
                    DateTimeHelper.getFormattedTime(numberOfMilliSeconds, format)
                } ?: ""
            }
            else -> {
                value.toString()
            }
        }

        if (stringToAppend.isNotEmpty()) {
            sb.append("$stringToAppend , ")
        }

        return sb
    }

    private fun retrieveAction(task: ActionTask): Action {
        val tableName = task.actionInfo.tableName
        val actionUUID = task.actionInfo.actionUUID
        ActionHelper.getActionObjectList(BaseApp.runtimeDataHolder.tableActions, tableName)
            .plus(ActionHelper.getActionObjectList(BaseApp.runtimeDataHolder.currentRecordActions, tableName))
            .forEach { action ->
                // create id with pattern: $actionName$tableName
                val actionId = action.getSafeString("name") + tableName
                if (actionUUID == actionId) {
                    return ActionHelper.createActionFromJsonObject(action)
                }
            }
        throw Action.ActionException("Couldn't find action from table [$tableName], with uuid [$actionUUID]")
    }
}
