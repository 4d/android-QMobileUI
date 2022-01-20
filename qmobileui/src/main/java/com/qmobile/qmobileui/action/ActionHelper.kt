package com.qmobile.qmobileui.action

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import com.qmobile.qmobileapi.utils.getSafeArray
import com.qmobile.qmobileapi.utils.getSafeString
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.binding.ImageHelper
import com.qmobile.qmobileui.binding.ImageHelper.adjustActionDrawableMargins
import org.json.JSONArray
import org.json.JSONObject

class ActionHelper private constructor() {

    companion object {
        fun getActionContent(
            tableName: String,
            selectedActionId: String?,
            parameters: HashMap<String, Any>? = null,
            metaData: HashMap<String, String>? = null,
            relationName: String? = null,
            parentPrimaryKey: String? = null,
            parentTableName: String? = null,
            parentRelationName: String? = null
        ): MutableMap<String, Any> {
            val map: MutableMap<String, Any> = mutableMapOf()
            val actionContext = mutableMapOf<String, Any>(
                "dataClass" to
                    BaseApp.genericTableHelper.originalTableName(tableName)
            )

            // entity
            val entity = HashMap<String, Any>()
            if (selectedActionId != null) {
                entity["primaryKey"] = selectedActionId
            }
            if (!relationName.isNullOrEmpty()) {
                entity["relationName"] = relationName
            }
            if (entity.isNotEmpty()) {
                actionContext["entity"] = entity
            }

            // parent
            if ((parentPrimaryKey != null) && (parentPrimaryKey != "0")) {
                val parent = HashMap<String, Any>()

                parent["primaryKey"] = parentPrimaryKey
                if (!parentRelationName.isNullOrEmpty()) {
                    parent["relationName"] = parentRelationName
                }

                if (!parentTableName.isNullOrEmpty()) {
                    parent["dataClass"] = parentTableName
                }

                if (parent.isNotEmpty()) {
                    actionContext["parent"] = parent
                }
            }

            map["context"] = actionContext
            parameters?.let { map.put("parameters", parameters) }
            metaData?.let { map.put("metadata", ActionMetaData(metaData)) }
            return map
        }

        fun createActionFromJsonObject(jsonObject: JSONObject): Action {
            jsonObject.apply {
                return Action(
                    name = getSafeString("name") ?: "",
                    icon = getSafeString("icon"),
                    shortLabel = getSafeString("shortLabel"),
                    label = getSafeString("label"),
                    preset = getSafeString("preset"),
                    parameters = getSafeArray("parameters") ?: JSONArray()
                )
            }
        }

        fun getActionIconDrawable(context: Context, action: Action): Drawable {
            var drawable: Drawable? = null
            val iconDrawablePath = action.getIconDrawablePath()
            iconDrawablePath?.let { icon ->
                val resId = context.resources.getIdentifier(
                    icon,
                    "drawable",
                    context.packageName
                )
                if (resId != 0)
                    drawable = ContextCompat.getDrawable(context, resId)
            }

            if (drawable == null)
                drawable = ContextCompat.getDrawable(context, R.drawable.empty_action)

            return drawable.adjustActionDrawableMargins(context)
        }

        fun getActionDrawablePadding(context: Context): Int =
            (ImageHelper.ICON_MARGIN * context.resources.displayMetrics.density).toInt()
    }
}
