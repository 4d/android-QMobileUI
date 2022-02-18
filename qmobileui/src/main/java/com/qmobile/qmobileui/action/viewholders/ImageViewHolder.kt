/*
 * Created by qmarciset on 11/1/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.action.viewholders

import android.Manifest
import android.net.Uri
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.qmobile.qmobileapi.model.entity.EntityModel
import com.qmobile.qmobileapi.model.entity.Photo
import com.qmobile.qmobileapi.utils.getSafeAny
import com.qmobile.qmobileapi.utils.getSafeString
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.action.ActionTypes
import com.qmobile.qmobileui.binding.ImageHelper
import com.qmobile.qmobileui.binding.Transformations
import com.qmobile.qmobileui.binding.getColorFromAttr
import com.qmobile.qmobileui.utils.PermissionChecker

class ImageViewHolder(
    itemView: View,
    hideKeyboardCallback: () -> Unit,
    private val actionTypesCallback: (actionTypes: ActionTypes, position: Int) -> Unit
) :
    BaseViewHolder(itemView, hideKeyboardCallback) {

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
        onValueChanged: (String, Any?, String?, Boolean) -> Unit
    ) {
        super.bind(item, currentEntity, preset, onValueChanged)

        itemJsonObject.getSafeString("label")?.let { parameterLabel ->
            label.text = if (isMandatory()) {
                "$parameterLabel *"
            } else {
                parameterLabel
            }
        }

        imageView.setOnClickListener {
            MaterialAlertDialogBuilder(itemView.context)
                .setTitle(itemView.context.getString(R.string.action_parameter_image_dialog_title))
                .setItems(
                    arrayOf(
                        itemView.context.getString(R.string.action_parameter_image_dialog_photo),
                        itemView.context.getString(R.string.action_parameter_image_dialog_gallery)
                    )
                ) { _, which ->
                    when (which) {
                        0 -> askCameraPermissionAndProceed()
                        1 -> actionTypesCallback(ActionTypes.PICK_PHOTO_GALLERY, bindingAdapterPosition)
                    }
                }
                .show()
        }

        removeItem.setOnClickListener {
            ContextCompat.getDrawable(imageView.context, R.drawable.image_plus)?.let {
                imageView.setImageDrawable(it)
                removeItem.visibility = View.INVISIBLE
                imageView.alpha = DEFAULT_PLACEHOLDER_ICON_ALPHA
                currentUri = null
                onValueChanged(parameterName, null, null, validate(false))
            }
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

    private fun askCameraPermissionAndProceed() {
        (itemView.context as PermissionChecker?)?.askPermission(
            context = itemView.context,
            permission = Manifest.permission.CAMERA,
            rationale = itemView.context.getString(R.string.permission_rationale_camera)
        ) { isGranted ->
            if (isGranted) {
                actionTypesCallback(ActionTypes.TAKE_PICTURE_CAMERA, bindingAdapterPosition)
            }
        }
    }

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
