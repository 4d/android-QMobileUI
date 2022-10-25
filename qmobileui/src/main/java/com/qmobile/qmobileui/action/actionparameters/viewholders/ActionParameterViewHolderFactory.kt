/*
 * Created by qmarciset on 3/6/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.action.actionparameters.viewholders

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.action.actionparameters.ActionParameter
import com.qmobile.qmobileui.action.actionparameters.viewholders.inputcontrols.KotlinInputControlBooleanViewHolder
import com.qmobile.qmobileui.action.actionparameters.viewholders.inputcontrols.KotlinInputControlViewHolder
import com.qmobile.qmobileui.action.actionparameters.viewholders.inputcontrols.MenuViewHolder
import com.qmobile.qmobileui.action.actionparameters.viewholders.inputcontrols.PickerViewHolder
import com.qmobile.qmobileui.action.actionparameters.viewholders.inputcontrols.PopoverViewHolder
import com.qmobile.qmobileui.action.actionparameters.viewholders.inputcontrols.PushViewHolder
import com.qmobile.qmobileui.action.actionparameters.viewholders.inputcontrols.SegmentedViewHolder
import com.qmobile.qmobileui.action.inputcontrols.InputControlFormatHolder
import com.qmobile.qmobileui.action.model.Action

@Suppress("LongMethod")
object ActionParameterViewHolderFactory {

    const val CUSTOM_TEXT_VH = 1
    const val CUSTOM_BOOLEAN_VH = 2
    const val INPUT_CONTROL_PUSH_VH = 3
    const val INPUT_CONTROL_SEGMENTED_VH = 4
    const val INPUT_CONTROL_POPOVER_VH = 5
    const val INPUT_CONTROL_MENU_VH = 6
    const val INPUT_CONTROL_PICKER_VH = 7

    fun createViewHolderFromViewType(
        viewType: Int,
        parent: ViewGroup,
        context: Context,
        fragmentManager: FragmentManager?,
        actionTypesCallback: (actionTypes: Action.Type, position: Int) -> Unit,
        goToPushFragment: (position: Int) -> Unit,
        formatHolderCallback: (holder: InputControlFormatHolder, position: Int) -> Unit,
        onDataLoadedCallback: () -> Unit
    ): BaseViewHolder {
        return when (val itemType = ActionParameter.values().getOrNull(viewType)) {
            // Input control
            null -> getInputControlViewHolder(
                viewType,
                parent,
                context,
                fragmentManager,
                goToPushFragment,
                formatHolderCallback,
                onDataLoadedCallback
            )

            // Text
            ActionParameter.TEXT_DEFAULT,
            ActionParameter.TEXT_EMAIL,
            ActionParameter.TEXT_PASSWORD,
            ActionParameter.TEXT_URL,
            ActionParameter.TEXT_ZIP,
            ActionParameter.TEXT_PHONE,
            ActionParameter.TEXT_ACCOUNT,
            ActionParameter.TEXT_AREA ->
                TextViewHolder(
                    LayoutInflater.from(context)
                        .inflate(R.layout.item_parameter_text, parent, false),
                    itemType.format
                )

            // Boolean
            ActionParameter.BOOLEAN_DEFAULT ->
                BooleanViewHolder(
                    LayoutInflater.from(context)
                        .inflate(R.layout.item_parameter_boolean_switch, parent, false),
                    itemType.format
                )
            ActionParameter.BOOLEAN_CHECK ->
                BooleanViewHolder(
                    LayoutInflater.from(context)
                        .inflate(R.layout.item_parameter_boolean_check_mark, parent, false),
                    itemType.format
                )

            // Number
            ActionParameter.NUMBER_DEFAULT1,
            ActionParameter.NUMBER_DEFAULT2,
            ActionParameter.NUMBER_SCIENTIFIC,
            ActionParameter.NUMBER_PERCENTAGE,
            ActionParameter.NUMBER_INTEGER,
            ActionParameter.NUMBER_SPELL_OUT ->
                NumberViewHolder(
                    LayoutInflater.from(context)
                        .inflate(R.layout.item_parameter_text, parent, false),
                    itemType.format
                )

            // Date
            ActionParameter.DATE_DEFAULT1,
            ActionParameter.DATE_DEFAULT2,
            ActionParameter.DATE_SHORT,
            ActionParameter.DATE_LONG,
            ActionParameter.DATE_FULL ->
                DateViewHolder(
                    LayoutInflater.from(context)
                        .inflate(R.layout.item_parameter_text, parent, false),
                    itemType.format,
                    fragmentManager
                )

            // Time
            ActionParameter.TIME_DEFAULT,
            ActionParameter.TIME_DURATION ->
                TimeViewHolder(
                    LayoutInflater.from(context)
                        .inflate(R.layout.item_parameter_text, parent, false),
                    itemType.format,
                    fragmentManager
                )

            // Image
            ActionParameter.IMAGE ->
                ImageViewHolder(
                    LayoutInflater.from(context)
                        .inflate(R.layout.item_parameter_image, parent, false),
                    itemType.format,
                    actionTypesCallback
                )
            ActionParameter.SIGNATURE ->
                SignatureViewHolder(
                    LayoutInflater.from(context)
                        .inflate(R.layout.item_parameter_image, parent, false),
                    itemType.format,
                    actionTypesCallback
                )
            ActionParameter.BARCODE ->
                BarcodeViewHolder(
                    LayoutInflater.from(context)
                        .inflate(R.layout.item_parameter_text, parent, false),
                    itemType.format,
                    actionTypesCallback
                )
        }
    }

    private fun getInputControlViewHolder(
        viewType: Int,
        parent: ViewGroup,
        context: Context,
        fragmentManager: FragmentManager?,
        goToPushFragment: (position: Int) -> Unit,
        formatHolderCallback: (holder: InputControlFormatHolder, position: Int) -> Unit,
        onDataLoadedCallback: () -> Unit
    ): BaseViewHolder {
        return when (viewType) {
            // Kotlin input control
            ActionParameter.values().size + CUSTOM_TEXT_VH -> KotlinInputControlViewHolder(
                LayoutInflater.from(context)
                    .inflate(R.layout.item_parameter_text, parent, false)
            )
            ActionParameter.values().size + CUSTOM_BOOLEAN_VH -> KotlinInputControlBooleanViewHolder(
                LayoutInflater.from(context)
                    .inflate(R.layout.item_parameter_boolean_custom, parent, false)
            )

            // Default input control
            ActionParameter.values().size + INPUT_CONTROL_PUSH_VH -> PushViewHolder(
                LayoutInflater.from(context)
                    .inflate(R.layout.item_parameter_text, parent, false),
                fragmentManager,
                goToPushFragment
            )
            ActionParameter.values().size + INPUT_CONTROL_SEGMENTED_VH -> SegmentedViewHolder(
                LayoutInflater.from(context)
                    .inflate(R.layout.input_control_segmented, parent, false),
                fragmentManager,
                formatHolderCallback,
                onDataLoadedCallback
            )
            ActionParameter.values().size + INPUT_CONTROL_POPOVER_VH -> PopoverViewHolder(
                LayoutInflater.from(context)
                    .inflate(R.layout.item_parameter_text, parent, false),
                fragmentManager,
                formatHolderCallback
            )
            ActionParameter.values().size + INPUT_CONTROL_MENU_VH -> MenuViewHolder(
                LayoutInflater.from(context)
                    .inflate(R.layout.input_control_menu, parent, false),
                fragmentManager,
                formatHolderCallback
            )
            ActionParameter.values().size + INPUT_CONTROL_PICKER_VH -> PickerViewHolder(
                LayoutInflater.from(context)
                    .inflate(R.layout.input_control_picker, parent, false),
                fragmentManager,
                formatHolderCallback,
                onDataLoadedCallback
            )
            else -> KotlinInputControlViewHolder(
                LayoutInflater.from(context)
                    .inflate(R.layout.item_parameter_text, parent, false)
            )
        }
    }
}
