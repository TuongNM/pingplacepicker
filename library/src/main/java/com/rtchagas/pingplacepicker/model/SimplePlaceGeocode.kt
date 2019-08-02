package com.rtchagas.pingplacepicker.model

import android.os.Parcelable
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize

@JsonClass(generateAdapter = true)
@Parcelize
data class SimplePlaceGeocode (
        @Json(name = "place_id")
        val placeId: String,
        val types: List<String>,
        val geometry: Geometry,
        val formatted_address: String
): Parcelable