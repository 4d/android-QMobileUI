/*
 * Created by qmarciset on 7/12/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.maps

import android.location.Address
import android.location.Geocoder
import android.os.Build
import timber.log.Timber
import java.io.IOException

object MapsHelper {

    fun Geocoder.getAddressFromName(
        addressString: String,
        onResult: (Address?) -> Unit
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getFromLocationName(addressString, 1) {
                onResult(it.firstOrNull())
            }
        } else {
            try {
                @Suppress("DEPRECATION")
                onResult(getFromLocationName(addressString, 1)?.firstOrNull())
            } catch (e: IOException) {
                Timber.e(e.message.orEmpty())
                onResult(null)
            }
        }
    }

    fun Geocoder.getAddressFromLatLng(
        latitude: Double,
        longitude: Double,
        onResult: (Address?) -> Unit
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getFromLocation(latitude, longitude, 1) {
                onResult(it.firstOrNull())
            }
        } else {
            try {
                @Suppress("DEPRECATION")
                onResult(getFromLocation(latitude, longitude, 1)?.firstOrNull())
            } catch (e: IOException) {
                Timber.e(e.message.orEmpty())
                onResult(null)
            }
        }
    }
}
