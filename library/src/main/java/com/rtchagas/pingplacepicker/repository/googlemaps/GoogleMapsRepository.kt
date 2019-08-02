package com.rtchagas.pingplacepicker.repository.googlemaps

import android.annotation.SuppressLint
import android.graphics.Bitmap
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.PhotoMetadata
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.PlaceLikelihood
import com.google.android.libraries.places.api.net.FetchPhotoRequest
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.rtchagas.pingplacepicker.Config
import com.rtchagas.pingplacepicker.PingPlacePicker
import com.rtchagas.pingplacepicker.model.SearchResultGeocode
import com.rtchagas.pingplacepicker.model.SimplePlace
import com.rtchagas.pingplacepicker.repository.PlaceRepository
import io.reactivex.Single
import java.util.*


class GoogleMapsRepository constructor(
        private val googleClient: PlacesClient,
        private val googleMapsAPI: GoogleMapsAPI)
    : PlaceRepository {


    /**
     * Finds all nearby places ranked by likelihood of being the place where the device is.
     *
     * This call will be charged according to
     * [Places SDK for Android Usage and
       Billing](https://developers.google.com/places/android-sdk/usage-and-billing#find-current-place)
     */
    @SuppressLint("MissingPermission")
    override fun getNearbyPlaces(): Single<Pair<String?, List<Place>>> {

        // Create request
        val request = FindCurrentPlaceRequest.builder(getPlaceFields()).build()

        return Single.create { emitter ->
            googleClient.findCurrentPlace(request).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    task.result?.let {
                        val placeList = sortByLikelihood(it.placeLikelihoods)
                        val likelihoodPlaceList = placeList.map { likelihood -> likelihood.place }

                        emitter.onSuccess( Pair(null, likelihoodPlaceList) )
                    }
                    // Empty result
                    emitter.onSuccess( Pair(null, listOf()) )
                }
                else {
                    emitter.onError(task.exception ?: Exception("No places for you..."))
                }
            }
        }
    }

    /** Finds all nearby places ranked by distance from the requested location.
     *
     * This call will be charged according to
     * [Places SDK WEB API Usage and
    Billing](https://developers.google.com/maps/billing/understanding-cost-of-use#nearby-search)
     */
    override fun getNearbyPlaces(location: LatLng): Single<Pair<String?, List<SimplePlace>>> {

        val locationParam = "${location.latitude},${location.longitude}"

        return googleMapsAPI.searchNearby(locationParam, PingPlacePicker.mapsApiKey)
                .map { searchResult ->
                    Pair(searchResult.next_page_token, searchResult.results)
                }
    }

    override fun getNearbyPlacesPageToken(pageToken: String): Single<Pair<String?, List<SimplePlace>>>
    {
        return googleMapsAPI.searchNearbyNextPage(pageToken, PingPlacePicker.mapsApiKey)
                .map { searchResult ->
                    Pair(searchResult.next_page_token, searchResult.results)
                }
    }

    /**
     * Fetches a photo for the place.
     *
     * This call will be charged according to
     * [Places SDK for Android Usage and
       Billing](https://developers.google.com/places/android-sdk/usage-and-billing#places-photo)
     */
    override fun getPlacePhoto(photoMetadata: PhotoMetadata): Single<Bitmap> {

        // Create a FetchPhotoRequest.
        val photoRequest = FetchPhotoRequest.builder(photoMetadata)
                .setMaxWidth(Config.PLACE_IMG_WIDTH)
                .setMaxHeight(Config.PLACE_IMG_HEIGHT)
                .build()

        return Single.create { emitter ->
            googleClient.fetchPhoto(photoRequest).addOnSuccessListener {
                val bitmap = it.bitmap
                emitter.onSuccess(bitmap)
            }.addOnFailureListener {
                emitter.onError(it)
            }
        }
    }

    /**
     * Uses Google Maps GeoLocation API to retrieve a place by its latitude and longitude.
     * This call will be charged according to
     * [Places SDK for Android Usage and
       Billing](https://developers.google.com/maps/documentation/geocoding/usage-and-billing#pricing-for-the-geocoding-api)
     */
    override fun getPlaceByLocation(location: LatLng): Single<SimplePlace?> {

        val paramLocation = "${location.latitude},${location.longitude}"

        val languageCode = Locale.getDefault().language
        return googleMapsAPI.findByLocation(paramLocation, languageCode, PingPlacePicker.mapsApiKey)
                .flatMap { result: SearchResultGeocode ->
                    if (("OK" == result.status) && result.results.isNotEmpty()) {
                        val simplePlaceGeocode = result.results[0];

                        val simplePlace = SimplePlace(simplePlaceGeocode.placeId, simplePlaceGeocode.types, simplePlaceGeocode.geometry, simplePlaceGeocode.formatted_address, null)
                        return@flatMap Single.just(simplePlace)
                    }
                    return@flatMap Single.just(null)
                }
    }

    /**
     * This call to Google Places API is totally free :)
     */
    private fun getPlaceById(placeId: String): Single<Place> {

        // Create the request
        val request = FetchPlaceRequest.builder(placeId, getPlaceFields()).build()

        return Single.create { emitter ->
            googleClient.fetchPlace(request)
                    .addOnSuccessListener {
                        emitter.onSuccess(it.place)
                    }
                    .addOnFailureListener {
                        emitter.onError(it)
                    }
        }
    }

    /**
     * These fields are not charged by Google.
     * https://developers.google.com/places/android-sdk/usage-and-billing#basic-data
     */
    private fun getPlaceFields(): List<Place.Field> {

        return listOf(
                Place.Field.ID,
                Place.Field.NAME,
                Place.Field.ADDRESS,
                Place.Field.LAT_LNG,
                Place.Field.TYPES,
                Place.Field.PHOTO_METADATAS)
    }

    /**
     * Sorts the list by Likelihood. The best ranked places come first.
     */
    private fun sortByLikelihood(placeLikelihoods: List<PlaceLikelihood>): List<PlaceLikelihood> {

        val mutableList = placeLikelihoods.toMutableList()

        mutableList.sortByDescending { it.likelihood }

        return mutableList
    }

    // TODO: Remove if no longer needed.
    /*
    private fun handleNearbySearchResult(searchResult: SearchResult): Single<Pair<String?, List<Place>>>
    {
        val singles = mutableListOf<Single<Place>>()

        searchResult.results.forEach {
            singles.add(getPlaceById(it.placeId))
        }

        return Single.zip(singles) { listOfResults ->
            val places = mutableListOf<Place>()
            listOfResults.forEach {
                places.add(it as Place)
            }

            return@zip Pair(searchResult.next_page_token, places)
        }
    }
    */
}