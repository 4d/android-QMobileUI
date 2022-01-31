package com.qmobile.qmobileui.action

import android.content.Context
import android.net.Uri
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import com.qmobile.qmobileapi.model.entity.EntityModel
import com.qmobile.qmobileapi.utils.getSafeObject
import com.qmobile.qmobileapi.utils.getSafeString
import com.qmobile.qmobileui.action.viewholders.BaseViewHolder
import org.json.JSONArray
import org.json.JSONObject

class ActionsParametersListAdapter(
    private val context: Context,
    private val parameters: JSONArray,
    private val currentEntity: EntityModel?,
    private val fragmentManager: FragmentManager?,
    private val hideKeyboardCallback: () -> Unit,
    private val intentChooserCallback: (position: Int) -> Unit,
    private val onValueChanged: (String, Any?, String?, Boolean) -> Unit
) :
    RecyclerView.Adapter<BaseViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        return ActionParameterViewHolderFactory.createViewHolderFromViewType(
            viewType,
            parent,
            context,
            fragmentManager,
            hideKeyboardCallback,
            intentChooserCallback
        )
    }

    override fun getItemCount(): Int {
        return parameters.length()
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        holder.bind(
            parameters[position],
            currentEntity
        ) { name: String, value: Any?, metaData: String?, isValid: Boolean ->
            onValueChanged(name, value, metaData, isValid)
        }
    }

    override fun getItemViewType(position: Int): Int {
        val itemJsonObject = parameters[position] as JSONObject
        val type = itemJsonObject.getSafeString("type")
        val format = itemJsonObject.getSafeString("format") ?: "default"
        return ActionParameterEnum.values().find { it.type == type && it.format == format }?.ordinal
            ?: 0
    }

    fun updateImageForPosition(position: Int, data: Uri) {
        parameters.getSafeObject(position)?.put("image_uri", data)
        notifyItemChanged(position)
    }

    fun getUpdatedImageParameterName(position: Int): String? {
        return parameters.getSafeObject(position)?.getSafeString("name")
    }
}
