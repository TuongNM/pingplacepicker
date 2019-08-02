package com.rtchagas.pingplacepicker.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SearchResultGeocode (
        val results: List<SimplePlaceGeocode>,
        val status: String
)