/*
 * Created by qmarciset on 7/12/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.list.maps

import android.location.Geocoder
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SnapHelper
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.maps.android.ktx.awaitMap
import com.google.maps.android.ktx.awaitMapLoad
import com.qmobile.qmobileapi.model.entity.EntityModel
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.databinding.FragmentMapsBinding
import com.qmobile.qmobileui.list.ListFormFragment
import com.qmobile.qmobileui.maps.MapsHelper.getAddressFromName
import com.qmobile.qmobileui.ui.BounceEdgeEffectFactory
import com.qmobile.qmobileui.ui.SnackbarHelper
import com.qmobile.qmobileui.ui.noTabLayoutUI
import java.util.*

class MapsFragment : ListFormFragment() {

    companion object {
        private const val mapZoom = 15f
    }

    private val addressMap: MutableMap<String, String> = mutableMapOf()
    private var currentPosition = 0
    private var currentAddress = ""
    private var googleMap: GoogleMap? = null

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): ViewDataBinding {
        return FragmentMapsBinding.inflate(inflater, container, false).apply {
            viewModel = entityListViewModel
            lifecycleOwner = viewLifecycleOwner
        }
    }

    override fun initRecyclerView() {
        val layoutManager = LinearLayoutManager(activity, RecyclerView.HORIZONTAL, false)
        recyclerView.layoutManager = layoutManager

        if (!noTabLayoutUI) {
            recyclerView.setPadding(0, 0, 0, getPaddingBottom())
        }

        recyclerView.adapter = adapter
        recyclerView.edgeEffectFactory = BounceEdgeEffectFactory()

        val snapHelper: SnapHelper = LinearSnapHelper()
        snapHelper.attachToRecyclerView(recyclerView)

        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    snapHelper.findSnapView(layoutManager)?.let { centerView ->
                        val newPos = layoutManager.getPosition(centerView)
                        if (newPos != currentPosition) {
                            currentPosition = newPos
                            refreshMap()
                        }
                    }
                }
            }
        })

        initMap()
    }

    private fun initMap() {
        val mapFragment = childFragmentManager.findFragmentById(R.id.map_fragment) as? SupportMapFragment
        lifecycleScope.launchWhenCreated {
            val googleMap = mapFragment?.awaitMap()
            googleMap?.awaitMapLoad()
            this@MapsFragment.googleMap = googleMap
            refreshMap()
        }
    }

    fun refreshAddressMap(address: String, key: String) {
        addressMap[key] = address
        refreshMap()
    }

    private fun refreshMap() {
        (adapter.getSelectedItem(currentPosition)?.__entity as? EntityModel)?.__KEY?.let { entityKey ->
            addressMap[entityKey]?.let { address ->
                if (currentAddress != address) {
                    currentAddress = address
                    computeMap(address)
                }
            }
        }
    }

    private fun computeMap(address: String) {
        getLocationFromAddress(address) { latLng ->
            lifecycleScope.launchWhenCreated {
                googleMap?.clear()
                googleMap?.addMarker(
                    MarkerOptions()
                        .title(address)
                        .position(latLng)
                )
                googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, mapZoom))
            }
        }
    }

    private fun getLocationFromAddress(addressString: String, onResult: (LatLng) -> Unit) {
        val geocoder = Geocoder(requireContext(), Locale.getDefault())
        geocoder.getAddressFromName(addressString) { address ->
            if (address != null) {
                onResult(LatLng(address.latitude, address.longitude))
            } else {
                SnackbarHelper.show(activity, "Could not get current address")
            }
        }
    }

    override fun initOnRefreshListener() {}
}
