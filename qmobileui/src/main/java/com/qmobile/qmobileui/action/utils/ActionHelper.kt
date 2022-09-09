/*
 * Created by htemanni on 1/6/2022.
 * 4D SAS
 * Copyright (c) 2022 htemanni. All rights reserved.
 */

package com.qmobile.qmobileui.action.utils

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.Base64
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.core.widget.TextViewCompat
import com.fasterxml.jackson.databind.ObjectMapper
import com.qmobile.qmobileapi.utils.getSafeArray
import com.qmobile.qmobileapi.utils.getSafeString
import com.qmobile.qmobileapi.utils.parseToString
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobiledatasync.relation.Relation
import com.qmobile.qmobiledatasync.relation.RelationHelper.inverseAliasPath
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.action.model.Action
import com.qmobile.qmobileui.action.model.ActionMetaData
import com.qmobile.qmobileui.binding.ImageHelper.DRAWABLE_24
import com.qmobile.qmobileui.binding.ImageHelper.DRAWABLE_32
import com.qmobile.qmobileui.binding.ImageHelper.ICON_MARGIN
import com.qmobile.qmobileui.binding.ImageHelper.NO_ICON_PADDING
import com.qmobile.qmobileui.binding.ImageHelper.adjustActionDrawableMargins
import com.qmobile.qmobileui.binding.ImageHelper.getDrawableFromString
import com.qmobile.qmobileui.binding.getColorFromAttr
import com.qmobile.qmobileui.binding.px
import org.json.JSONArray
import org.json.JSONObject

object ActionHelper {

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
                uuid = getSafeString("uuid") ?: "",
                description = getSafeString("description") ?: ""
            )
        }
    }

    fun getActionIconDrawable(context: Context, action: Action): Drawable? {
        var drawable: Drawable? = getDrawableFromString(context, action.icon, DRAWABLE_32.px, DRAWABLE_32.px)

        if (drawable == null) {
            drawable = ContextCompat.getDrawable(context, R.drawable.empty_action)
        }

        return drawable?.adjustActionDrawableMargins(context)
    }

    fun Drawable.paramMenuActionDrawable(context: Context) {
        this.setMenuItemColorFilter(context)
        this.setBounds(0, 0, DRAWABLE_24.px, DRAWABLE_24.px)
    }

    private fun Drawable.setMenuItemColorFilter(context: Context) {
        this.colorFilter = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
            context.getColorFromAttr(R.attr.colorOnSurface),
            BlendModeCompat.SRC_ATOP
        )
    }

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
                    val padding = ICON_MARGIN.px
                    textView.compoundDrawablePadding = padding
                    textView.setPadding(padding, 0, 0, 0)
                } else {
                    textView.setPadding(NO_ICON_PADDING.px, 0, 0, 0)
                }

                textView.text = action.getPreferredName()
                return itemView
            }
        }
    }

    fun getActionButtonColor(context: Context, index: Int): Int {
        return when (index) {
            0 -> context.getColorFromAttr(R.attr.colorPrimary)
            1 -> context.getColorFromAttr(R.attr.colorSecondary)
            else -> context.getColorFromAttr(R.attr.colorTertiary)
        }
    }

    // Get base 64 encoded context from actionContent map
    fun getBase64EncodedContext(actionContent: Map<String, Any>): String {
        val context =BaseApp.mapper.parseToString(actionContent["context"])
        return Base64.encodeToString(context.toByteArray(), Base64.NO_WRAP)
    }
}
