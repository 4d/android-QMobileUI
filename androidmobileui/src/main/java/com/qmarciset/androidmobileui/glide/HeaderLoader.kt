/*
 * Created by Quentin Marciset on 11/2/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmarciset.androidmobileui.glide

import android.content.Context
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.Headers
import com.bumptech.glide.load.model.LazyHeaders
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.model.stream.BaseGlideUrlLoader
import com.qmarciset.androidmobileapi.auth.AuthInfoHelper
import com.qmarciset.androidmobileapi.network.ApiClient
import java.io.InputStream

class HeaderLoader(context: Context, concreteLoader: ModelLoader<GlideUrl, InputStream>) :
    BaseGlideUrlLoader<String>(concreteLoader) {

    private val authInfoHelper = AuthInfoHelper(context)

    /**
     * Adjusts image url depending on what is given in remoteUrl
     */
    private fun buildImageUrl(imageUrl: String): String =
        authInfoHelper.remoteUrl.removeSuffix("/") + imageUrl

    override fun getUrl(model: String?, width: Int, height: Int, options: Options?): String {
        if (model.isNullOrEmpty()) {
            return ""
        }
        return buildImageUrl(model)
    }

    override fun getHeaders(model: String?, width: Int, height: Int, options: Options?): Headers? {

        val lazyHeadersBuilder = LazyHeaders.Builder()

        // If a token is stored in sharedPreferences, we add it in header
        if (authInfoHelper.sessionToken.isNotEmpty()) {
            lazyHeadersBuilder
                .setHeader(
                    ApiClient.AUTHORIZATION_HEADER_KEY,
                    "${ApiClient.AUTHORIZATION_HEADER_VALUE_PREFIX} ${authInfoHelper.sessionToken}"
                )
        }

        // Adding default headers
        lazyHeadersBuilder.addHeader(
            ApiClient.CONTENT_TYPE_HEADER_KEY,
            ApiClient.CONTENT_TYPE_HEADER_VALUE
        )
            .addHeader(
                ApiClient.X_QMOBILE_HEADER_KEY,
                ApiClient.X_QMOBILE_HEADER_VALUE
            )

        return lazyHeadersBuilder.build()
    }

    override fun handles(model: String): Boolean {
        return true
    }
}
