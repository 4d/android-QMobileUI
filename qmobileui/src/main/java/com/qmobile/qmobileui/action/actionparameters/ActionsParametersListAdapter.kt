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
import com.qmobile.qmobileui.action.actionparameters.ActionParametersFragment.Companion.BARCODE_VALUE_INJECT_KEY
import com.qmobile.qmobileui.action.actionparameters.ActionParametersFragment.Companion.IMAGE_URI_INJECT_KEY
import com.qmobile.qmobileui.action.actionparameters.viewholders.ActionParameterViewHolderFactory
import com.qmobile.qmobileui.action.actionparameters.viewholders.BaseViewHolder
import com.qmobile.qmobileui.action.actionparameters.viewholders.ImageViewHolder
import com.qmobile.qmobileui.action.actionparameters.viewholders.SignatureViewHolder
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
    private val paramsToSubmit: HashMap<String, Any>,
    private val imagesToUpload: HashMap<String, Uri>,
    private val paramsError: HashMap<String, String>,
    private val currentEntity: RoomEntity?,
    private val fragmentManager: FragmentManager?,
    private val hideKeyboardCallback: () -> Unit,
    private val focusNextCallback: (position: Int, onlyScroll: Boolean) -> Unit,
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
        return allParameters.length()
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        val item = allParameters[position]
        val paramName = (item as JSONObject).getSafeString("name")

        // Value used to pre-fill offline action edit form
        val alreadyFilledValue = when (holder) {
            is ImageViewHolder, is SignatureViewHolder ->
                // if image or signature we take the uri to pre-fill image/signature preview
                imagesToUpload[paramName]
            else ->
                // for other field we take the value to prefill editText
                paramsToSubmit[paramName]
        }

        // Error returned from server for this specific param
        val errorText = paramsError[paramName]

        holder.bind(
            item = item,
            currentEntity = currentEntity,
            isLastParameter = position == allParameters.length(),
            alreadyFilledValue = alreadyFilledValue,
            serverError = errorText
        ) { name: String, value: Any?, metaData: String?, isValid: Boolean ->
            onValueChanged(name, value, metaData, isValid)
        }

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
                        (context as FragmentActivity?)?.let { hideKeyboard(it) }
                    }
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        val itemJsonObject = allParameters[position] as JSONObject
        val type = itemJsonObject.getSafeString("type")
        val format = itemJsonObject.getSafeString("format")

        if (format?.startsWith("/") == true) { // Kotlin Input Control
            return when (type) {
                "bool" -> ActionParameter.values().size + 2
                else -> ActionParameter.values().size + 1
            }
        }

        val actionParameter =
            ActionParameter.values().firstOrNull { it.type == type && it.format == format }
                ?: ActionParameter.values().find { it.type == type && it.format == "default" }
        return actionParameter?.ordinal ?: 0
    }

    fun updateImageForPosition(position: Int, data: Uri) {
        (allParameters[position] as JSONObject).put(IMAGE_URI_INJECT_KEY, data)
        notifyItemChanged(position)
    }

    fun updateBarcodeForPosition(position: Int, value: String) {
        (allParameters[position] as JSONObject).put(BARCODE_VALUE_INJECT_KEY, value)
        notifyItemChanged(position)
    }
}
