package com.qmobile.qmobileui.action

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.action.viewholders.BarcodeViewHolder
import com.qmobile.qmobileui.action.viewholders.BaseViewHolder
import com.qmobile.qmobileui.action.viewholders.BooleanViewHolder
import com.qmobile.qmobileui.action.viewholders.DateViewHolder
import com.qmobile.qmobileui.action.viewholders.ImageViewHolder
import com.qmobile.qmobileui.action.viewholders.NumberViewHolder
import com.qmobile.qmobileui.action.viewholders.SignatureViewHolder
import com.qmobile.qmobileui.action.viewholders.TextViewHolder
import com.qmobile.qmobileui.action.viewholders.TimeViewHolder

@Suppress("LongMethod")
class ActionParameterViewHolderFactory private constructor() {
    companion object {
        fun createViewHolderFromViewType(
            viewType: Int,
            parent: ViewGroup,
            context: Context,
            fragmentManager: FragmentManager?,
            hideKeyboardCallback: () -> Unit,
            actionTypesCallback: (actionTypes: ActionTypes, position: Int) -> Unit
        ): BaseViewHolder {

            return when (val itemType = ActionParameterEnum.values()[viewType]) {
                // Text
                ActionParameterEnum.TEXT_DEFAULT,
                ActionParameterEnum.TEXT_EMAIL,
                ActionParameterEnum.TEXT_PASSWORD,
                ActionParameterEnum.TEXT_ZIP,
                ActionParameterEnum.TEXT_PHONE,
                ActionParameterEnum.TEXT_ACCOUNT,
                ActionParameterEnum.TEXT_URL,
                ActionParameterEnum.TEXT_AREA ->
                    TextViewHolder(
                        LayoutInflater.from(context)
                            .inflate(R.layout.item_parameter_text, parent, false),
                        itemType.format,
                        hideKeyboardCallback
                    )
                // Number
                ActionParameterEnum.NUMBER_DEFAULT1,
                ActionParameterEnum.NUMBER_DEFAULT2,
                ActionParameterEnum.NUMBER_INTEGER,
                ActionParameterEnum.NUMBER_PERCENTAGE,
                ActionParameterEnum.NUMBER_SCIENTIFIC,
                ActionParameterEnum.NUMBER_SPELL_OUT ->
                    NumberViewHolder(
                        LayoutInflater.from(context)
                            .inflate(R.layout.item_parameter_text, parent, false),
                        itemType.format,
                        hideKeyboardCallback
                    )
                // Date
                ActionParameterEnum.DATE_DEFAULT1,
                ActionParameterEnum.DATE_DEFAULT2,
                ActionParameterEnum.DATE_SHORT,
                ActionParameterEnum.DATE_FULL,
                ActionParameterEnum.DATE_LONG ->
                    DateViewHolder(
                        LayoutInflater.from(context)
                            .inflate(R.layout.item_parameter_text, parent, false),
                        itemType.format,
                        fragmentManager,
                        hideKeyboardCallback
                    )
                // Time
                ActionParameterEnum.TIME_DEFAULT,
                ActionParameterEnum.TIME_DURATION ->
                    TimeViewHolder(
                        LayoutInflater.from(context)
                            .inflate(R.layout.item_parameter_text, parent, false),
                        itemType.format,
                        fragmentManager,
                        hideKeyboardCallback
                    )
                // Boolean
                ActionParameterEnum.BOOLEAN_DEFAULT ->
                    BooleanViewHolder(
                        LayoutInflater.from(context)
                            .inflate(R.layout.item_parameter_boolean_switch, parent, false),
                        hideKeyboardCallback
                    )
                ActionParameterEnum.BOOLEAN_CHECK ->
                    BooleanViewHolder(
                        LayoutInflater.from(context)
                            .inflate(R.layout.item_parameter_boolean_check_mark, parent, false),
                        hideKeyboardCallback
                    )
                // Image
                ActionParameterEnum.IMAGE ->
                    ImageViewHolder(
                        LayoutInflater.from(context)
                            .inflate(R.layout.item_parameter_image, parent, false),
                        hideKeyboardCallback,
                        actionTypesCallback
                    )
                // Signature
                ActionParameterEnum.SIGNATURE ->
                    SignatureViewHolder(
                        LayoutInflater.from(context)
                            .inflate(R.layout.item_parameter_image, parent, false),
                        hideKeyboardCallback,
                        actionTypesCallback
                    )
                // Barcode
                ActionParameterEnum.BARCODE ->
                    BarcodeViewHolder(
                        LayoutInflater.from(context)
                            .inflate(R.layout.item_parameter_text, parent, false),
                        hideKeyboardCallback,
                        actionTypesCallback
                    )
            }
        }
    }
}
