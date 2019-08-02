package com.rtchagas.pingplacepicker.repository.googlemaps

import com.google.android.gms.maps.model.LatLng
import com.rtchagas.pingplacepicker.model.SimplePlace

object LastNearbySearchSingleton
{
    var lastLocationForNearbySearch: LatLng? = null
    var lastPlacesList: List<SimplePlace>? = null

    fun updateLastNearbySearch(location: LatLng, placesList: List<SimplePlace>)
    {
        this.lastLocationForNearbySearch = location
        this.lastPlacesList = placesList
    }
}