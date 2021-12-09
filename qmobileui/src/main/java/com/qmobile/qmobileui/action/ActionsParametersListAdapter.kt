package com.qmobile.qmobileui.action

import android.content.Context
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.qmobile.qmobileapi.utils.getSafeString
import org.json.JSONArray
import org.json.JSONObject

class ActionsParametersListAdapter(
    context: Context,
    val list: JSONArray,
    val onValueChanged: (String, Any, String?) -> Unit
) :
    RecyclerView.Adapter<ActionParameterViewHolder>() {

    private val context: Context = context

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActionParameterViewHolder {
        return ActionParameterViewHolderFactory.createViewHolderFromViewType(
            viewType,
            parent,
            context
        )
    }

    override fun getItemCount(): Int {
        return list.length()
    }

    override fun onBindViewHolder(holder: ActionParameterViewHolder, position: Int) {
        holder.bind(list[position]) { name: String, value: Any, metaData: String? ->
            onValueChanged(name, value, metaData)
        }
    }

    override fun getItemViewType(position: Int): Int {
        val itemJsonObject = list[position] as JSONObject
        val type = itemJsonObject.getSafeString("type")
        val format = itemJsonObject.getSafeString("format") ?: "default"
        return ActionParameterEnum.values().find { it.type == type && it.format == format }?.ordinal
            ?: 0
    }
}
