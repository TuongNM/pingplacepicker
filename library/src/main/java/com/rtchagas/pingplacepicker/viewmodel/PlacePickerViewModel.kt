package com.rtchagas.pingplacepicker.viewmodel

import android.location.Location
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.maps.model.LatLng
import com.rtchagas.pingplacepicker.PingPlacePicker
import com.rtchagas.pingplacepicker.model.SimplePlace
import com.rtchagas.pingplacepicker.repository.PlaceRepository
import com.rtchagas.pingplacepicker.repository.googlemaps.LastNearbySearchSingleton
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers

class PlacePickerViewModel constructor(private var repository: PlaceRepository)
    : BaseViewModel() {

    private val kMinimumDistanceBeforeNearbySearch = 20

    // Keep the place list in this view model state
    private val placeList: MutableLiveData<Resource<List<SimplePlace>>> = MutableLiveData()

    private var lastLocation: LatLng = LatLng(0.0, 0.0)

    fun getNearbyPlaces(location: LatLng, filterType: String): LiveData<Resource<List<SimplePlace>>> {

        // If we already loaded the places for this location, return the same live data
        // instead of fetching (and charging) again.
        placeList.value?.run {
            if (lastLocation == location)
            {
                return placeList
            }
        }

        val lastLocationForNearbySearch = LastNearbySearchSingleton.lastLocationForNearbySearch
        val lastPlacesList = LastNearbySearchSingleton.lastPlacesList

        if (lastLocationForNearbySearch != null && lastPlacesList != null)
        {
            val resultArray = FloatArray(1)
            Location.distanceBetween(lastLocationForNearbySearch.latitude, lastLocationForNearbySearch.longitude, location.latitude, location.longitude, resultArray)

            if (resultArray[0] < this.kMinimumDistanceBeforeNearbySearch)
            {
                placeList.value = Resource.success(lastPlacesList)
                return placeList
            }
        }

        // Update the last fetched location
        lastLocation = location

        // TODO: Find a way to combine the Places of getNearbyPlaces() with th SimplePlaces of getNearbyPlaces(location. Possible need a third ProxyPlace class.
        /*
                val placeQuery =
                if (PingPlacePicker.isNearbySearchEnabled || PingPlacePicker.useNearbySearchInsteadOfCurrentPlace)
                    repository.getNearbyPlaces(location)
                else
                    repository.getNearbyPlaces()
         */

        val placeQuery = repository.getNearbyPlaces(location, filterType)

        var newPlaceList: List<SimplePlace>
        val disposable: Disposable = placeQuery
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { placeList.value = Resource.loading() }
                .subscribe(
                        { result: Pair<String?, List<SimplePlace>> ->
                            val (nextPageToken, aPlaceList) = result
                            newPlaceList = aPlaceList

                            if ( (PingPlacePicker.isNearbySearchEnabled || PingPlacePicker.useNearbySearchInsteadOfCurrentPlace) && PingPlacePicker.resolveNearbySearchPaging )
                            {
                                nextPageToken?.let { pageToken ->
                                    getNearbyPlacesWithPageToken(pageToken, newPlaceList, location)
                                }
                            }
                            else
                            {
                                val sortedPlaceList = this.sortPlaceListByDistanceAndFilterPOIs(aPlaceList)
                                placeList.value = Resource.success(sortedPlaceList)

                                LastNearbySearchSingleton.updateLastNearbySearch(location, sortedPlaceList)
                            }
                        },
                        { error: Throwable -> placeList.value = Resource.error(error) }
                )

        // Keep track of this disposable during the ViewModel lifecycle
        addDisposable(disposable)

        return placeList
    }

    fun getPlaceByLocation(location: LatLng): LiveData<Resource<SimplePlace?>> {

        val liveData = MutableLiveData<Resource<SimplePlace?>>()

        val disposable: Disposable = repository.getPlaceByLocation(location)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { liveData.value = Resource.loading() }
                .subscribe(
                        { result: SimplePlace? -> liveData.value = Resource.success(result) },
                        { error: Throwable -> liveData.value = Resource.error(error) }
                )

        // Keep track of this disposable during the ViewModel lifecycle
        addDisposable(disposable)

        return liveData
    }

    private fun getNearbyPlacesWithPageToken(pageToken: String, currentPlaceList: List<SimplePlace>, lastLocation: LatLng)
    {
        try
        {
            // Google issues the next page token before it becomes valid.
            // Therefore, need to wait to ensure that enough time passes so that the token does become valid.
            Thread.sleep(1500)
        }
        catch (e: Exception)
        {
            e.printStackTrace()
        }

        val nextDisposable: Disposable = repository.getNearbyPlacesPageToken(pageToken)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { result: Pair<String?, List<SimplePlace>> ->
                            val (nextPageToken, nextPlaceList) = result
                            val newPlaceList = currentPlaceList + nextPlaceList

                            nextPageToken?.also { theNextPageToken ->
                                getNearbyPlacesWithPageToken(theNextPageToken, newPlaceList, lastLocation)
                            } ?: kotlin.run {
                                val sortedPlaceList = this.sortPlaceListByDistanceAndFilterPOIs(newPlaceList)
                                placeList.value = Resource.success(sortedPlaceList)

                                LastNearbySearchSingleton.updateLastNearbySearch(lastLocation, sortedPlaceList)
                            }
                        },
                        { error: Throwable -> placeList.value = Resource.error(error) }
                )

        addDisposable(nextDisposable)
    }

    /**
     * Sorts the given placeList by distance to the last location and also filters out only the POIs.
     */
    private fun sortPlaceListByDistanceAndFilterPOIs(placeList: List<SimplePlace>): List<SimplePlace>
    {
        val filteredList = placeList.filter { place ->
            val typeList = place.types ?: listOf()
            typeList.contains("point_of_interest")
        }

        return filteredList.sortedWith(Comparator<SimplePlace>{ firstPlace, secondPlace ->
            // Sort alphabetically.
            val firstName = firstPlace.name
            val secondName = secondPlace.name

            if (firstName != null && secondName == null)
            {
                -1
            }
            else if (firstName == null && secondName != null)
            {
                1
            }
            else if (firstName != null && secondName != null)
            {
                firstName.compareTo(secondName, ignoreCase = true)
            }
            else
            {
                0
            }
        })
    }
}