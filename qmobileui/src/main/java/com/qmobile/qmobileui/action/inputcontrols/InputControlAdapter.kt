/*
 * Created by qmarciset on 16/9/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.action.inputcontrols

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.qmobile.qmobiledatastore.data.RoomEntity
import com.qmobile.qmobiledatasync.utils.FieldMapping
import com.qmobile.qmobiledatasync.utils.fieldAdjustment
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.action.actionparameters.viewholders.inputcontrols.BaseInputControlViewHolder.Companion.NO_VALUE_PLACEHOLDER
import com.qmobile.qmobileui.action.actionparameters.viewholders.inputcontrols.BaseInputControlViewHolder.Companion.NO_VALUE_PLACEHOLDER_KEY
import com.qmobile.qmobileui.action.inputcontrols.InputControl.getImageName
import com.qmobile.qmobileui.formatters.ImageNamed
import com.qmobile.qmobileui.ui.setOnSingleClickListener
import com.qmobile.qmobileui.utils.ReflectionUtils
import java.util.LinkedList

class InputControlAdapter(
    private val context: Context,
    private val items: LinkedList<Any>,
    private val fieldMapping: FieldMapping?,
    private val isMandatory: Boolean,
    private val field: String? = null,
    private val entityFormat: String? = null,
    private val onItemClick: (displayText: String, fieldValue: Any?, position: Int) -> Unit
) :
    RecyclerView.Adapter<InputControlAdapter.TextLineViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TextLineViewHolder {
        return TextLineViewHolder(
            LayoutInflater.from(context).inflate(R.layout.input_control_list_item, parent, false)
        )
    }

    override fun onBindViewHolder(holder: TextLineViewHolder, position: Int) {
        when (val item = items[position]) {
            is RoomEntity -> handleRoomEntity(holder, item, position)
            is String -> handleString(holder, item, position)
            is Pair<*, *> -> handlePair(holder, item, position)
        }
    }

    private fun handleRoomEntity(holder: TextLineViewHolder, item: RoomEntity, position: Int) {
        field?.let {
            val fieldValue = ReflectionUtils.getInstanceProperty(item, field.fieldAdjustment())
            val displayText: String = InputControl.applyEntityFormat(item, entityFormat).ifEmpty {
                fieldValue?.toString() ?: ""
            }
            holder.textView.text = displayText
            holder.itemView.setOnSingleClickListener {
                onItemClick(displayText, fieldValue, position)
            }
        }
    }

    private fun handleString(holder: TextLineViewHolder, item: String, position: Int) {
        if (item == NO_VALUE_PLACEHOLDER_KEY) {
            handleNoValuePlaceholder(holder)
        } else {
            bind(holder, item)
            holder.itemView.setOnSingleClickListener {
                if (isMandatory) {
                    onItemClick(item, position.toString(), position)
                } else {
                    // the null placeholder get the position 0
                    onItemClick(item, (position - 1).toString(), position)
                }
            }
        }
    }

    private fun handlePair(holder: TextLineViewHolder, item: Pair<*, *>, position: Int) {
        if (item.first == NO_VALUE_PLACEHOLDER_KEY) {
            handleNoValuePlaceholder(holder)
        } else {
            item.second?.let {
                bind(holder, it.toString())
                holder.itemView.setOnSingleClickListener {
                    onItemClick(it.toString(), item.first, position)
                }
            }
        }
    }

    private fun bind(holder: TextLineViewHolder, item: String) {
        if (fieldMapping?.binding == "imageNamed") {
            fieldMapping.getImageName(item)?.let { imageName ->
                ImageNamed.setDrawable(holder.textView, imageName, fieldMapping.imageWidth, fieldMapping.imageHeight)
            }
        } else {
            holder.textView.text = item
        }
    }

    private fun handleNoValuePlaceholder(holder: TextLineViewHolder) {
        holder.textView.text = NO_VALUE_PLACEHOLDER
        holder.itemView.setOnSingleClickListener {
            onItemClick(NO_VALUE_PLACEHOLDER, null, 0)
        }
    }

    override fun getItemCount(): Int = items.size

    inner class TextLineViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(R.id.input_control_line)
    }
}
