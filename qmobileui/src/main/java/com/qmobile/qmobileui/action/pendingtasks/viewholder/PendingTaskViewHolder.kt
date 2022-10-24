/*
 * Created by qmarciset on 3/6/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.action.pendingtasks.viewholder

import com.qmobile.qmobileapi.utils.getJSONObjectList
import com.qmobile.qmobileapi.utils.getSafeString
import com.qmobile.qmobiledatastore.dao.ActionTask
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobileui.action.inputcontrols.InputControl
import com.qmobile.qmobileui.action.inputcontrols.InputControl.Format.getInputControlFormatHolders
import com.qmobile.qmobileui.action.inputcontrols.InputControlFormatHolder
import com.qmobile.qmobileui.action.model.Action
import com.qmobile.qmobileui.action.utils.ActionHelper
import com.qmobile.qmobileui.databinding.ItemPendingTaskBinding
import com.qmobile.qmobileui.formatters.FormatterUtils
import org.json.JSONArray
import org.json.JSONObject

class PendingTaskViewHolder(
    private val binding: ItemPendingTaskBinding,
    onItemClick: (actionTask: ActionTask) -> Unit
) : BaseTaskViewHolder(binding.root, onItemClick) {

    override fun bind(actionTask: ActionTask, isFromSettings: Boolean) {
        super.bind(actionTask, isFromSettings)
        bindItemDetails(actionTask)
    }

    private fun bindItemDetails(actionTask: ActionTask) {
        var sb = StringBuilder()
        // get all parameters for related action of this task to check the type/format of each paramToSubmit
        val relatedActionParameters: List<JSONObject> = retrieveAction(actionTask).parameters.getJSONObjectList()
        relatedActionParameters.forEachIndexed { index, actionParameter ->
            actionTask.actionInfo.paramsToSubmit?.entries?.find { it.key == actionParameter.getSafeString("name") }
                ?.let { entry ->
                    val type = actionParameter.getSafeString("type")
                    val format = actionParameter.getSafeString("format")
                    sb = getFieldOverview(actionTask, index, format, type, entry.value, sb)
                }
        }
        binding.overview.text = sb.removeSuffix(" , ")
    }

    private fun getFieldOverview(
        actionTask: ActionTask,
        index: Int,
        format: String?,
        type: String?,
        value: Any,
        sb: StringBuilder
    ): StringBuilder {
        val stringToAppend = when {
            format == "password" -> "" // We don't display password fields
            type == "date" -> FormatterUtils.applyFormat("shortDate", value.toString())
            format == "duration" -> FormatterUtils.applyFormat("duration", value.toString())
            type == "time" -> FormatterUtils.applyFormat("shortTime", value.toString())
            format in InputControl.Types.values().map { it.format } -> getInputControlValue(actionTask, index)
            else -> value.toString()
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

    private fun getInputControlValue(actionTask: ActionTask, index: Int): String {
        actionTask.actionInfo.allParameters?.let { allParameters ->
            val parameters = JSONArray(allParameters)
            (parameters.get(index) as? JSONObject)?.let { actionParameter ->
                val inputControlName = actionParameter.getSafeString("source")?.removePrefix("/")
                val fieldMapping = BaseApp.runtimeDataHolder.inputControls.find { it.name == inputControlName }
                if (fieldMapping?.binding == "imageNamed") {
                    return ""
                }
            }
        }
        val map: Map<Int, InputControlFormatHolder> = actionTask.getInputControlFormatHolders()
        return map[index]?.displayText ?: ""
    }
}
