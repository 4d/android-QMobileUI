package com.qmobile.qmobileui.action

import android.content.Context
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.qmobile.qmobileapi.model.entity.EntityModel
import com.qmobile.qmobileapi.utils.getSafeObject
import com.qmobile.qmobileapi.utils.getSafeString
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.action.viewholders.BaseViewHolder
import org.json.JSONObject
import timber.log.Timber

class ActionsParametersListAdapter(
    private val context: Context,
    private val action: Action,
    private val currentEntity: EntityModel?,
    private val fragmentManager: FragmentManager?,
    private val hideKeyboardCallback: () -> Unit,
    private val focusNextCallback: (position: Int) -> Unit,
    private val actionTypesCallback: (actionTypes: Action.Type, position: Int) -> Unit,
    private val onValueChanged: (String, Any?, String?, Boolean) -> Unit
) :
    RecyclerView.Adapter<BaseViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        return ActionParameterViewHolderFactory.createViewHolderFromViewType(
            viewType,
            parent,
            context,
            fragmentManager,
            actionTypesCallback
        )
    }

    override fun getItemCount(): Int {
        return action.parameters.length()
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        holder.bind(
            action.parameters[position],
            currentEntity,
            action.preset,
            position == action.parameters.length() - 1
        ) { name: String, value: Any?, metaData: String?, isValid: Boolean ->
            onValueChanged(name, value, metaData, isValid)
        }

        // When clicking outside an EditText we want to hide the keyboard
        holder.itemView.setOnClickListener {
            hideKeyboardCallback()
        }

        // Handle IME_ACTION_NEXT as issue will occur due to not rendered views
        holder.itemView.findViewById<TextInputEditText>(R.id.input)?.let { input ->
            input.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_NEXT) {
                    focusNextCallback(holder.bindingAdapterPosition)
                    true
                } else {
                    // Make sure to return false to let default behavior for other event such as IME_ACTION_DONE
                    // or KeyEvent.KEYCODE_ENTER
                    false
                }
            }
        }

        // Adding another OnFocusChangeListener because we can lose the focus with ime 'actionNext' not finding the next
        // focusable EditText as the view might be recycled. In this case, it's the view that get the focus.
        holder.itemView.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                holder.itemView.findViewById<TextInputEditText>(R.id.input)
                    ?.requestFocus()
                    ?: kotlin.run {
                        Timber.d("itemView ${holder.bindingAdapterPosition} has focus but no input, hiding keyboard")
                        hideKeyboardCallback()
                    }
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        val itemJsonObject = action.parameters[position] as JSONObject
        val type = itemJsonObject.getSafeString("type")
        val format = itemJsonObject.getSafeString("format") ?: "default"
        return ActionParameterEnum.values().find { it.type == type && it.format == format }?.ordinal
            ?: 0
    }

    fun updateImageForPosition(position: Int, data: Uri) {
        action.parameters.getSafeObject(position)?.put("image_uri", data)
        notifyItemChanged(position)
    }

    fun updateBarcodeForPosition(position: Int, value: String) {
        action.parameters.getSafeObject(position)?.put("barcode_value", value)
        notifyItemChanged(position)
    }
}
