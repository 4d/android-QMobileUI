/*
 * Created by qmarciset on 3/6/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.action.actionparameters

import android.content.Context
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.qmobile.qmobileapi.utils.getSafeString
import com.qmobile.qmobiledatastore.data.RoomEntity
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.action.actionparameters.ActionParametersFragment.Companion.INPUT_CONTROL_DISPLAY_TEXT_INJECT_KEY
import com.qmobile.qmobileui.action.actionparameters.ActionParametersFragment.Companion.INPUT_CONTROL_FIELD_VALUE_INJECT_KEY
import com.qmobile.qmobileui.action.actionparameters.viewholders.ActionParameterViewHolderFactory
import com.qmobile.qmobileui.action.actionparameters.viewholders.ActionParameterViewHolderFactory.CUSTOM_BOOLEAN_VH
import com.qmobile.qmobileui.action.actionparameters.viewholders.ActionParameterViewHolderFactory.CUSTOM_TEXT_VH
import com.qmobile.qmobileui.action.actionparameters.viewholders.ActionParameterViewHolderFactory.INPUT_CONTROL_MENU_VH
import com.qmobile.qmobileui.action.actionparameters.viewholders.ActionParameterViewHolderFactory.INPUT_CONTROL_PICKER_VH
import com.qmobile.qmobileui.action.actionparameters.viewholders.ActionParameterViewHolderFactory.INPUT_CONTROL_POPOVER_VH
import com.qmobile.qmobileui.action.actionparameters.viewholders.ActionParameterViewHolderFactory.INPUT_CONTROL_PUSH_VH
import com.qmobile.qmobileui.action.actionparameters.viewholders.ActionParameterViewHolderFactory.INPUT_CONTROL_SEGMENTED_VH
import com.qmobile.qmobileui.action.actionparameters.viewholders.BaseViewHolder
import com.qmobile.qmobileui.action.actionparameters.viewholders.ImageViewHolder
import com.qmobile.qmobileui.action.actionparameters.viewholders.SignatureViewHolder
import com.qmobile.qmobileui.action.inputcontrols.InputControl
import com.qmobile.qmobileui.action.inputcontrols.InputControlFormatHolder
import com.qmobile.qmobileui.action.model.Action
import com.qmobile.qmobileui.ui.setOnSingleClickListener
import com.qmobile.qmobileui.utils.hideKeyboard
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber

class ActionsParametersListAdapter(
    private val context: Context,
    val allParameters: JSONArray,
    // contains data of the failed action, this data will be used for pre-fill form to edit pending task
    private val paramsToSubmit: HashMap<String, Any?>,
    private val imagesToUpload: HashMap<String, Uri>,
    private val paramsError: HashMap<String, String>,
    private val inputControlFormatHolders: Map<Int, InputControlFormatHolder>,
    private val currentEntity: RoomEntity?,
    private val fragmentManager: FragmentManager?,
    private val hideKeyboardCallback: () -> Unit,
    private val focusNextCallback: (position: Int, onlyScroll: Boolean) -> Unit,
    private val actionTypesCallback: (actionTypes: Action.Type, position: Int) -> Unit,
    private val goToPushFragment: (position: Int) -> Unit,
    private val formatHolderCallback: (holder: InputControlFormatHolder, position: Int) -> Unit,
    private val onDataLoadedCallback: () -> Unit,
    private val onValueChanged: (String, Any?, String?, Boolean) -> Unit
) :
    RecyclerView.Adapter<BaseViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        return ActionParameterViewHolderFactory.createViewHolderFromViewType(
            viewType,
            parent,
            context,
            fragmentManager,
            actionTypesCallback,
            goToPushFragment,
            formatHolderCallback,
            onDataLoadedCallback
        )
    }

    override fun getItemCount(): Int {
        return allParameters.length()
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        val item = allParameters[position] as? JSONObject ?: return
        val paramName = item.getSafeString("name")

        // Value used to pre-fill offline action edit form
        val alreadyFilledValue = when (holder) {
            is ImageViewHolder, is SignatureViewHolder ->
                // if image or signature we take the uri to pre-fill image/signature preview
                imagesToUpload[paramName] ?: paramsToSubmit[paramName]
            else ->
                // for other field we take the value to prefill editText
                paramsToSubmit[paramName]
        }

        // Error returned from server for this specific param
        val errorText = paramsError[paramName]

        item.put(INPUT_CONTROL_DISPLAY_TEXT_INJECT_KEY + "_$position", inputControlFormatHolders[position]?.displayText)
        item.put(INPUT_CONTROL_FIELD_VALUE_INJECT_KEY + "_$position", inputControlFormatHolders[position]?.fieldValue)

        holder.bind(
            item = item,
            currentEntity = currentEntity,
            isLastParameter = position == allParameters.length(),
            alreadyFilledValue = alreadyFilledValue,
            serverError = errorText,
            onValueChanged = onValueChanged
        )

        // When clicking outside an EditText we want to hide the keyboard
        holder.itemView.setOnSingleClickListener {
            hideKeyboardCallback()
        }

        // Handle IME_ACTION_NEXT as issue will occur due to not rendered views
        holder.itemView.findViewById<TextInputEditText>(R.id.input)?.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                focusNextCallback(holder.bindingAdapterPosition, false)
                true
            } else {
                // Make sure to return false to let default behavior for other event such as IME_ACTION_DONE
                // or KeyEvent.KEYCODE_ENTER
                focusNextCallback(holder.bindingAdapterPosition, true)
                false
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
                        // Warning :
                        // do not call hideKeyboardCallback() because it will clearFocus and generate an infinite loop
                        (context as? FragmentActivity)?.let { hideKeyboard(it) }
                    }
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        val itemJsonObject = allParameters[position] as? JSONObject ?: return 0
        val type = itemJsonObject.getSafeString("type")
        val format = itemJsonObject.getSafeString("format")
        val source = itemJsonObject.getSafeString("source")

        return if (format?.startsWith("/") == true) {
            getInputControlViewType(format, type, source)
        } else {
            val actionParameter =
                ActionParameter.values().firstOrNull { it.type == type && it.format == format }
                    ?: ActionParameter.values().find { it.type == type && it.format == "default" }
            actionParameter?.ordinal ?: 0
        }
    }

    private fun getInputControlViewType(format: String, type: String?, source: String?): Int {
        return if (format in InputControl.Types.values().map { it.format }) {
            getDefaultInputControlViewType(format, source)
        } else {
            getKotlinInputControlViewType(type)
        }
    }

    private fun getKotlinInputControlViewType(type: String?): Int {
        return when (type) {
            "text" -> ActionParameter.values().size + CUSTOM_TEXT_VH
            "bool" -> ActionParameter.values().size + CUSTOM_BOOLEAN_VH
            else -> ActionParameter.values().size + CUSTOM_TEXT_VH
        }
    }

    private fun getDefaultInputControlViewType(format: String, source: String?): Int {
        return when {
            source == null -> 0
            format == InputControl.Types.PUSH.format -> ActionParameter.values().size + INPUT_CONTROL_PUSH_VH
            format == InputControl.Types.SEGMENTED.format -> ActionParameter.values().size + INPUT_CONTROL_SEGMENTED_VH
            format == InputControl.Types.POPOVER.format -> ActionParameter.values().size + INPUT_CONTROL_POPOVER_VH
            format == InputControl.Types.MENU.format -> ActionParameter.values().size + INPUT_CONTROL_MENU_VH
            format == InputControl.Types.PICKER.format -> ActionParameter.values().size + INPUT_CONTROL_PICKER_VH
            else -> 0
        }
    }
}
