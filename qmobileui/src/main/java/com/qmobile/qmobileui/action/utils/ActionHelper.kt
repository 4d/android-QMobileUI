/*
 * Created by htemanni on 1/6/2022.
 * 4D SAS
 * Copyright (c) 2022 htemanni. All rights reserved.
 */

package com.qmobile.qmobileui.action.utils

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.widget.TextViewCompat
import com.qmobile.qmobileapi.utils.getSafeArray
import com.qmobile.qmobileapi.utils.getSafeString
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobiledatasync.relation.Relation
import com.qmobile.qmobiledatasync.relation.RelationHelper.inverseAliasPath
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.action.model.Action
import com.qmobile.qmobileui.action.model.ActionMetaData
import com.qmobile.qmobileui.binding.ImageHelper
import com.qmobile.qmobileui.binding.ImageHelper.adjustActionDrawableMargins
import org.json.JSONArray
import org.json.JSONObject
import kotlin.collections.HashMap

class ActionHelper private constructor() {

    companion object {
        fun getActionContent(
            tableName: String,
            actionUUID: String,
            itemId: String = "",
            parameters: HashMap<String, Any>? = null,
            metaData: HashMap<String, String>? = null,
            parentItemId: String = "",
            relation: Relation? = null
        ): MutableMap<String, Any> {
            val actionContext = mutableMapOf<String, Any>(
                "dataClass" to
                    (BaseApp.runtimeDataHolder.tableInfo[tableName]?.originalName ?: "")
            )
            // entity
            val entity = HashMap<String, Any>()

            if (itemId.isNotEmpty()) {
                entity["primaryKey"] = itemId
            }

            if (relation != null) {
                entity["relationName"] = relation.name
            }

            if (entity.isNotEmpty()) {
                actionContext["entity"] = entity
            }

            // parent
            if (relation != null) {
                val parent = HashMap<String, Any>()

                parent["primaryKey"] = parentItemId

                parent["relationName"] = relation.inverseAliasPath()
                parent["dataClass"] = relation.source

                if (parent.isNotEmpty()) {
                    actionContext["parent"] = parent
                }
            }

            val map: MutableMap<String, Any> = mutableMapOf()
            map["context"] = actionContext
            map["id"] = actionUUID
            parameters?.let { map.put("parameters", parameters) }
            metaData?.let { map.put("metadata", ActionMetaData(metaData)) }
            return map
        }

        fun createActionFromJsonObject(jsonObject: JSONObject): Action {
            jsonObject.apply {
                return Action(
                    name = getSafeString("name") ?: "",
                    shortLabel = getSafeString("shortLabel"),
                    label = getSafeString("label"),
                    icon = getSafeString("icon"),
                    preset = getSafeString("preset"),
                    scope = if (getSafeString("scope") == "table") Action.Scope.TABLE else Action.Scope.CURRENT_RECORD,
                    parameters = getSafeArray("parameters") ?: JSONArray(),
                    uuid = getSafeString("uuid") ?: ""
                )
            }
        }

        fun getActionIconDrawable(context: Context, action: Action): Drawable {
            var drawable: Drawable? = null
            val iconDrawablePath = action.getIconDrawablePath()
            iconDrawablePath?.let { icon ->
                val resId = context.resources.getIdentifier(icon, "drawable", context.packageName)
                if (resId != 0) {
                    drawable = ContextCompat.getDrawable(context, resId)
                }
            }

            if (drawable == null) {
                drawable = ContextCompat.getDrawable(context, R.drawable.empty_action)
            }

            return drawable.adjustActionDrawableMargins(context)
        }

        private fun getActionDrawablePadding(context: Context): Int =
            (ImageHelper.ICON_MARGIN * context.resources.displayMetrics.density).toInt()

        fun fillActionList(json: JSONObject, tableName: String, actionList: MutableList<Action>) {
            getActionObjectList(json, tableName).forEach {
                val action = createActionFromJsonObject(it)
                // Action having preset "sort" without any parameter should be discarded
                if (!(action.isSortAction() && action.parameters.length() == 0)) {
                    actionList.add(action)
                }
            }
        }

        fun getActionObjectList(json: JSONObject, tableName: String): List<JSONObject> {
            val objectList = mutableListOf<JSONObject>()
            json.getSafeArray(tableName)?.let { actionsArray ->
                for (i in 0 until actionsArray.length()) {
                    actionsArray.getJSONObject(i)?.let {
                        objectList.add(it)
                    }
                }
            }
            return objectList
        }

        fun getActionArrayAdapter(context: Context, actionList: List<Action>): ArrayAdapter<Action> {
            val withIcons = actionList.firstOrNull { it.getIconDrawablePath() != null } != null

            return object :
                ArrayAdapter<Action>(context, R.layout.material_select_dialog_item, android.R.id.text1, actionList) {
                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val itemView = super.getView(position, convertView, parent)
                    val textView = itemView.findViewById<View>(android.R.id.text1) as TextView
                    val action = actionList[position]

                    if (withIcons) {
                        val drawable = getActionIconDrawable(context, action)
                        textView.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null)
                        TextViewCompat.setCompoundDrawableTintList(textView, textView.textColors)
                        // Add margin between image and text (support various screen densities)
                        textView.compoundDrawablePadding =
                            getActionDrawablePadding(context)
                    }

                    textView.text = action.getPreferredName()
                    return itemView
                }
            }
        }
    }
}
