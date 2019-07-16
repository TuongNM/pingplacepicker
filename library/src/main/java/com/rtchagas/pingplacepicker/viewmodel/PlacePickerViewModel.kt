package com.rtchagas.pingplacepicker.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.Place
import com.rtchagas.pingplacepicker.PingPlacePicker
import com.rtchagas.pingplacepicker.repository.PlaceRepository
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers

class PlacePickerViewModel constructor(private var repository: PlaceRepository)
    : BaseViewModel() {

    // Keep the place list in this view model state
    private val placeList: MutableLiveData<Resource<List<Place>>> = MutableLiveData()

    private var lastLocation: LatLng = LatLng(0.0, 0.0)

    fun getNearbyPlaces(location: LatLng): LiveData<Resource<List<Place>>> {

        // If we already loaded the places for this location, return the same live data
        // instead of fetching (and charging) again.
        placeList.value?.run {
            if (lastLocation == location) return placeList
        }

        // Update the last fetched location
        lastLocation = location

        val placeQuery =
                if (PingPlacePicker.isNearbySearchEnabled || PingPlacePicker.useNearbySearchInsteadOfCurrentPlace)
                    repository.getNearbyPlaces(location)
                else
                    repository.getNearbyPlaces()

        var newPlaceList: List<Place> = listOf()
        val disposable: Disposable = placeQuery
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { placeList.value = Resource.loading() }
                .subscribe(
                        { result: Pair<String?, List<Place>> ->
                            val (nextPageToken, aPlaceList) = result
                            newPlaceList = aPlaceList

                            if (PingPlacePicker.isNearbySearchEnabled || PingPlacePicker.useNearbySearchInsteadOfCurrentPlace)
                            {
                                nextPageToken?.let { pageToken ->
                                    getNearbyPlacesWithPageToken(pageToken, newPlaceList)
                                }
                            }
                            else
                            {
                                placeList.value = Resource.success(aPlaceList)
                            }
                        },
                        { error: Throwable -> placeList.value = Resource.error(error) }
                )

        // Keep track of this disposable during the ViewModel lifecycle
        addDisposable(disposable)

        return placeList
    }

    fun getPlaceByLocation(location: LatLng): LiveData<Resource<Place?>> {

        val liveData = MutableLiveData<Resource<Place?>>()

        val disposable: Disposable = repository.getPlaceByLocation(location)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { liveData.value = Resource.loading() }
                .subscribe(
                        { result: Place? -> liveData.value = Resource.success(result) },
                        { error: Throwable -> liveData.value = Resource.error(error) }
                )

        // Keep track of this disposable during the ViewModel lifecycle
        addDisposable(disposable)

        return liveData
    }

    private fun getNearbyPlacesWithPageToken(pageToken: String, currentPlaceList: List<Place>)
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
                        { result: Pair<String?, List<Place>> ->
                            val (nextPageToken, nextPlaceList) = result
                            val newPlaceList = currentPlaceList + nextPlaceList

                            nextPageToken?.also { theNextPageToken ->
                                getNearbyPlacesWithPageToken(theNextPageToken, newPlaceList)
                            } ?: kotlin.run {
                                placeList.value = Resource.success(newPlaceList)
                            }
                        },
                        { error: Throwable -> placeList.value = Resource.error(error) }
                )

        addDisposable(nextDisposable)
    }
}