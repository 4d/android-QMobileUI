/*
 * Created by qmarciset on 21/2/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.action.viewholders

import android.net.Uri
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.qmobile.qmobileapi.model.entity.EntityModel
import com.qmobile.qmobileapi.model.entity.Photo
import com.qmobile.qmobileapi.utils.getSafeAny
import com.qmobile.qmobileapi.utils.getSafeString
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.binding.ImageHelper
import com.qmobile.qmobileui.binding.Transformations
import com.qmobile.qmobileui.binding.getColorFromAttr

abstract class BaseImageViewHolder(itemView: View) : BaseViewHolder(itemView) {

    private val label: TextView = itemView.findViewById(R.id.label)
    private val error: TextView = itemView.findViewById(R.id.error)
    private var imageView: ImageView = itemView.findViewById(R.id.image_view)
    private var removeItem: ImageView = itemView.findViewById(R.id.item_remove)

    private var currentUri: String? = null

    companion object {
        const val DEFAULT_PLACEHOLDER_ICON_ALPHA = 0.6F
    }

    override fun bind(
        item: Any,
        currentEntity: EntityModel?,
        preset: String?,
        isLastParameter: Boolean,
        onValueChanged: (String, Any?, String?, Boolean) -> Unit
    ) {
        super.bind(item, currentEntity, preset, isLastParameter, onValueChanged)

        itemJsonObject.getSafeString("label")?.let { parameterLabel ->
            label.text = if (isMandatory()) {
                "$parameterLabel *"
            } else {
                parameterLabel
            }
        }

        setDefaultPlaceholder()

        imageView.setOnClickListener {
            onImageClick()
        }

        removeItem.setOnClickListener {
            setDefaultPlaceholder()
            removeItem.visibility = View.INVISIBLE
            currentUri = null
            onValueChanged(parameterName, null, null, validate(false))
        }

        newImageChosen()?.let { uri ->
            displayImage(uri)
        } ?: kotlin.run {
            getDefaultFieldValue(currentEntity, itemJsonObject) { defaultPhoto ->
                (defaultPhoto as Photo?)?.__deferred?.uri?.let {
                    displayImage(it)
                }
            }
        }

        onValueChanged(parameterName, currentUri, null, validate(false))
    }

    abstract fun getPlaceholderRes(): Int

    abstract fun onImageClick()

    override fun validate(displayError: Boolean): Boolean {

        if (isMandatory() && currentUri != null) {
            if (displayError)
                showError(itemView.context.getString(R.string.action_parameter_mandatory_error))
            return false
        }
        return true
    }

    override fun getInputType(format: String): Int {
        return -1
    }

    private fun setDefaultPlaceholder() {
        ContextCompat.getDrawable(imageView.context, getPlaceholderRes())?.let {
            imageView.setImageDrawable(it)
            imageView.alpha = DEFAULT_PLACEHOLDER_ICON_ALPHA
        }
    }

    private fun newImageChosen(): Uri? =
        itemJsonObject.getSafeAny("image_uri") as Uri?

    private fun displayImage(data: Any) {
        currentUri = data.toString()
        error.visibility = View.INVISIBLE

        val glideRequest = ImageHelper.getGlideRequest(imageView, data)

        Transformations.getTransformation(
            "CropCircleWithBorder",
            imageView.context.getColorFromAttr(android.R.attr.colorPrimary)
        )?.let { transformation ->
            glideRequest.transform(transformation)
        }

        glideRequest.into(imageView)
        imageView.alpha = 1F
        removeItem.visibility = View.VISIBLE
    }

    private fun showError(text: String) {
        error.text = text
        error.visibility = View.VISIBLE
    }
}
