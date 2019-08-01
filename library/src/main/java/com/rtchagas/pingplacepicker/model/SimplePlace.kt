package com.rtchagas.pingplacepicker.model

import android.os.Parcelable
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize

@JsonClass(generateAdapter = true)
@Parcelize
data class SimplePlace(
        @Json(name = "place_id")
        val placeId: String,
        val types: List<String>,
        val geometry: Geometry,
        val name: String?,
        val vicinity: String?
): Parcelable

@Parcelize
data class Geometry(
        val location: Location
): Parcelable

@Parcelize
data class Location (
        val lat: Double,
        val lng: Double
): Parcelable