/*
 * Created by qmarciset on 11/7/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.action.actionparameters.viewholders

import android.net.Uri
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.qmobile.qmobileapi.model.entity.EntityModel
import com.qmobile.qmobileapi.model.entity.Photo
import com.qmobile.qmobileapi.utils.getSafeAny
import com.qmobile.qmobileapi.utils.getSafeString
import com.qmobile.qmobileapi.utils.parseToType
import com.qmobile.qmobiledatastore.data.RoomEntity
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.binding.ImageHelper
import com.qmobile.qmobileui.binding.px
import com.qmobile.qmobileui.ui.setOnSingleClickListener
import com.qmobile.qmobileui.utils.ReflectionUtils

abstract class BaseImageViewHolder(itemView: View, format: String) : BaseViewHolder(itemView, format) {

    private val label: TextView = itemView.findViewById(R.id.label)
    private val error: TextView = itemView.findViewById(R.id.error)
    private var imageView: ImageView = itemView.findViewById(R.id.image_view)
    private var removeItem: ImageView = itemView.findViewById(R.id.item_remove)

    private var currentUri: String? = null

    companion object {
        const val DEFAULT_PLACEHOLDER_ICON_ALPHA = 0.6F
        const val ROUNDING_RADIUS = 12
    }

    override fun bind(
        item: Any,
        currentEntity: RoomEntity?,
        isLastParameter: Boolean,
        alreadyFilledValue: Any?,
        serverError: String?,
        onValueChanged: (String, Any?, String?, Boolean) -> Unit
    ) {
        super.bind(item, currentEntity, isLastParameter, alreadyFilledValue, serverError, onValueChanged)

        itemJsonObject.getSafeString("label")?.let { parameterLabel ->
            label.text = if (isMandatory()) {
                "$parameterLabel *"
            } else {
                parameterLabel
            }
        }

        setDefaultPlaceholder()

        imageView.setOnSingleClickListener {
            onImageClick()
        }

        removeItem.setOnSingleClickListener {
            setDefaultPlaceholder()
            removeItem.visibility = View.INVISIBLE
            currentUri = null
            onValueChanged(parameterName, null, null, validate(false))
        }

        newImageChosen()?.let { uri ->
            displayImage(uri.toString())
        } ?: kotlin.run {
            if (alreadyFilledValue is Uri) {
                displayImage(alreadyFilledValue.toString())
            } else {
                getDefaultFieldValue(currentEntity, itemJsonObject) { defaultPhoto ->
                    BaseApp.mapper.parseToType<Photo>(defaultPhoto.toString())?.__deferred?.uri?.let {
                        displayImage(it)
                    }
                }
            }
        }

        onValueChanged(parameterName, currentUri, null, validate(false))

        showServerError()
    }

    abstract fun getPlaceholderRes(): Int

    abstract fun onImageClick()

    override fun validate(displayError: Boolean): Boolean {
        if (isMandatory() && currentUri == null) {
            if (displayError) {
                showError(itemView.context.getString(R.string.action_parameter_mandatory_error))
            }
            return false
        }
        return true
    }

    override fun getInputType(format: String): Int {
        return -1
    }

    override fun fill(value: Any) {}

    private fun setDefaultPlaceholder() {
        ContextCompat.getDrawable(imageView.context, getPlaceholderRes())?.let {
            imageView.background = null
            imageView.setImageDrawable(it)
            imageView.alpha = DEFAULT_PLACEHOLDER_ICON_ALPHA
        }
    }

    private fun newImageChosen(): Uri? {
        val uri: Uri? = itemJsonObject.getSafeAny("image_uri") as Uri?
        itemJsonObject.remove("image_uri")
        return uri
    }

    private fun displayImage(uriString: String, currentEntity: RoomEntity? = null, defaultField: String? = null) {
        currentUri = uriString
        error.visibility = View.INVISIBLE

        var key: String? = null
        currentEntity?.let {
            key = if (defaultField?.contains(".") == true) { // alias
                (
                    ReflectionUtils.getInstanceProperty(
                        currentEntity,
                        defaultField.substringBeforeLast("?.")
                    ) as EntityModel?
                    )?.__KEY
            } else { // not alias
                (currentEntity.__entity as EntityModel?)?.__KEY
            }
        }

        val image: Any = ImageHelper.getImage(
            uriString,
            itemJsonObject.getSafeString("fieldName"),
            key,
            itemJsonObject.getSafeString("tableName")
        )

        val glideRequest = ImageHelper.getGlideRequest(imageView, image)
        val options = RequestOptions().centerCrop().transform(RoundedCorners(ROUNDING_RADIUS.px))
        glideRequest.apply(options).into(imageView)

        imageView.alpha = 1F
        removeItem.visibility = View.VISIBLE
    }

    override fun showError(text: String) {
        error.text = text
        error.visibility = View.VISIBLE
    }
}
