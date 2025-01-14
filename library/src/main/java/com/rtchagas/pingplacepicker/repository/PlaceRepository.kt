package com.rtchagas.pingplacepicker.repository

import android.graphics.Bitmap
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.PhotoMetadata
import com.google.android.libraries.places.api.model.Place
import com.rtchagas.pingplacepicker.model.SimplePlace
import io.reactivex.Single

/**
 * We decided to interface the Places repository as there's a lot of
 * room to improve the place search and retrieval.
 * We could have different repositories to fetch places locally from
 * a cached database or from other providers than Google.
 */
interface PlaceRepository {

    fun getNearbyPlaces(): Single<Pair<String?, List<Place>>>

    fun getNearbyPlaces(location: LatLng, filterType: String): Single<Pair<String?, List<SimplePlace>>>
    fun getNearbyPlacesPageToken(pageToken: String): Single<Pair<String?, List<SimplePlace>>>

    fun getPlacePhoto(photoMetadata: PhotoMetadata): Single<Bitmap>

    fun getPlaceByLocation(location: LatLng): Single<SimplePlace?>
}