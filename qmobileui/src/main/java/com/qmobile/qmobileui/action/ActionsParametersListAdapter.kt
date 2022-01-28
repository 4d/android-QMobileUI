package com.qmobile.qmobileui.action

import android.content.Context
import android.net.Uri
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.qmobile.qmobileapi.model.entity.EntityModel
import com.qmobile.qmobileapi.utils.getSafeString
import org.json.JSONArray
import org.json.JSONObject

class ActionsParametersListAdapter(
    context: Context,
    val list: JSONArray,
    private val currentEntity: EntityModel?,
    val onValueChanged: (String, Any?, String?, Boolean) -> Unit,
    val goToScanner: (Int) -> Unit
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
        holder.bind(
            list[position],
            currentEntity,
            { name: String, value: Any?, metaData: String?, isValid: Boolean ->
                onValueChanged(name, value, metaData, isValid)
            }, {
            goToScanner(it)
        }
        )
    }

    override fun getItemViewType(position: Int): Int {
        val itemJsonObject = list[position] as JSONObject
        val type = itemJsonObject.getSafeString("type")
        val format = itemJsonObject.getSafeString("format") ?: "default"
        return ActionParameterEnum.values().find { it.type == type && it.format == format }?.ordinal
            ?: 0
    }

    fun updateImageForPosition(position: Int, data: Any) {
        if (data is Uri) {
            (list[position] as JSONObject).put("uri", data)
        } else {
            (list[position] as JSONObject).put("bitmap", data)
        }
        notifyItemChanged(position)
    }

    fun getUpdatedImageParameterName(position: Int): String? {
        return (list[position] as JSONObject).getSafeString("name")
    }

    fun updateBarcodeForPosition(position: Int, value: String) {
        (list[position] as JSONObject).put("scanned", value)
    }
}
