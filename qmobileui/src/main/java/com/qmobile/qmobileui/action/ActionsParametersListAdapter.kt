package com.qmobile.qmobileui.action

import android.content.Context
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.qmobile.qmobileapi.model.entity.EntityModel
import com.qmobile.qmobileapi.utils.getSafeString
import com.qmobile.qmobileui.action.viewholders.BaseViewHolder
import org.json.JSONArray
import org.json.JSONObject

class ActionsParametersListAdapter(
    private val context: Context,
    private val list: JSONArray,
    private val currentEntity: EntityModel?,
    private val hideKeyboardCallback: () -> Unit,
    private val onValueChanged: (String, Any?, String?, Boolean) -> Unit
) :
    RecyclerView.Adapter<BaseViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        return ActionParameterViewHolderFactory.createViewHolderFromViewType(
            viewType,
            parent,
            context,
            hideKeyboardCallback
        )
    }

    override fun getItemCount(): Int {
        return list.length()
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        holder.bind(
            list[position],
            currentEntity
        ) { name: String, value: Any?, metaData: String?, isValid: Boolean ->
            onValueChanged(name, value, metaData, isValid)
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
