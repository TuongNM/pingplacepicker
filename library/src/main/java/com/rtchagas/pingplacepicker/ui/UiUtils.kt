package com.rtchagas.pingplacepicker.ui

import android.content.Context
import com.rtchagas.pingplacepicker.R
import com.rtchagas.pingplacepicker.model.SimplePlace


object UiUtils {

    /**
     * Gets the place drawable resource according to its type
     */
    fun getPlaceDrawableRes(context: Context, place: SimplePlace): Int {

        val defType = "drawable"
        val defPackage = context.packageName

        place.types?.let {
            for (type: String in it) {
                val name = type.toLowerCase()
                val id: Int = context.resources
                        .getIdentifier("ic_places_$name", defType, defPackage)
                if (id > 0) return id
            }
        }

        // Default resource
        return R.drawable.ic_map_marker_black_24dp
    }
}