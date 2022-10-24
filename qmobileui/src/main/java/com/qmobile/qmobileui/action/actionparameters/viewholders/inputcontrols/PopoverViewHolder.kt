/*
 * Created by qmarciset on 22/9/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.action.actionparameters.viewholders.inputcontrols

import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.qmobile.qmobiledatastore.data.RoomEntity
import com.qmobile.qmobiledatasync.utils.FieldMapping
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.action.actionparameters.viewholders.BaseInputLessViewHolder
import com.qmobile.qmobileui.action.inputcontrols.InputControl
import com.qmobile.qmobileui.action.inputcontrols.InputControlAdapter
import com.qmobile.qmobileui.action.inputcontrols.InputControlFormatHolder
import com.qmobile.qmobileui.ui.getStatusBarHeight
import org.json.JSONObject
import java.util.LinkedList

class PopoverViewHolder(
    itemView: View,
    fragmentManager: FragmentManager?,
    private val formatHolderCallback: (holder: InputControlFormatHolder, position: Int) -> Unit,
    format: String = ""
) : BaseInputLessViewHolder(itemView, format), BaseInputControlViewHolder {

    override val fragMng: FragmentManager? = fragmentManager
    override var fieldMapping: FieldMapping? = null
    override val placeHolder = itemView.context.getString(R.string.input_control_popover_baseline)
    override val circularProgressBar: CircularProgressIndicator? = null

    private lateinit var bottomSheetDialog: BottomSheetDialog
    private lateinit var adapter: InputControlAdapter
    private var recyclerView: RecyclerView? = null

    override fun bind(
        item: JSONObject,
        currentEntity: RoomEntity?,
        isLastParameter: Boolean,
        alreadyFilledValue: Any?,
        serverError: String?,
        onValueChanged: (String, Any?, String?, Boolean) -> Unit
    ) {
        super.bind(item, currentEntity, isLastParameter, alreadyFilledValue, serverError, onValueChanged)

        fieldMapping = getFieldMapping(itemJsonObject)

        container.isExpandedHintEnabled = false
        container.endIconDrawable = ContextCompat.getDrawable(itemView.context, R.drawable.chevron_right)

        val displayText = getDisplayText(itemJsonObject, bindingAdapterPosition, input.text.toString(), placeHolder)
        setTextOrIcon(container, input, displayText)

        val fieldValue: Any? = getFieldValue(itemJsonObject, bindingAdapterPosition, input.text.toString(), placeHolder)
        val typedValue = InputControl.getTypedValue(itemJsonObject, fieldValue)
        onValueChanged(parameterName, typedValue, null, validate(false))

        initBottomSheetDialog()
        initRecyclerView()

        setupInputControlData(isMandatory())

        setOnSingleClickListener {
            bottomSheetDialog.show()
        }

        showServerError()
    }

    override fun setupValues(items: LinkedList<Any>, field: String?, entityFormat: String?) {
        adapter = InputControlAdapter(
            context = itemView.context,
            items = items,
            fieldMapping = fieldMapping,
            isMandatory = isMandatory(),
            field = field,
            entityFormat = entityFormat,
            onItemClick = { displayText, fieldValue ->
                onItemSelected(displayText, fieldValue)
            }
        )
        recyclerView?.adapter = adapter
    }

    private fun initRecyclerView() {
        recyclerView = bottomSheetDialog.findViewById(R.id.input_control_list_recycler_view)
        recyclerView?.layoutManager = LinearLayoutManager(itemView.context, RecyclerView.VERTICAL, false)
        val divider = DividerItemDecoration(itemView.context, LinearLayoutManager.VERTICAL)
        recyclerView?.addItemDecoration(divider)
    }

    private fun initBottomSheetDialog() {
        bottomSheetDialog = BottomSheetDialog(itemView.context)
        bottomSheetDialog.setContentView(R.layout.input_control_list_fragment)

        bottomSheetDialog.findViewById<View>(R.id.design_bottom_sheet)?.let {
            val marginHeight =
                itemView.context.resources.displayMetrics.heightPixels - getStatusBarHeight(itemView.context)
            BottomSheetBehavior.from(it).peekHeight = marginHeight
        }
    }

    private fun onItemSelected(displayText: String, fieldValue: Any?) {
        container.error = null
        setTextOrIcon(container, input, displayText)
        val typedValue = InputControl.getTypedValue(itemJsonObject, fieldValue)
        onValueChanged(parameterName, typedValue, null, validate(false))
        val inputControlFormatHolder = InputControlFormatHolder(displayText, typedValue)
        formatHolderCallback(inputControlFormatHolder, bindingAdapterPosition)
        bottomSheetDialog.dismiss()
    }

    override fun formatToDisplay(input: String): String {
        return input
    }

    override fun validate(displayError: Boolean): Boolean {
        if (isMandatory() && isInputValid(input.text.toString())) {
            if (displayError) {
                showError(itemView.context.getString(R.string.action_parameter_mandatory_error))
            }
            return false
        }
        return true
    }
}
