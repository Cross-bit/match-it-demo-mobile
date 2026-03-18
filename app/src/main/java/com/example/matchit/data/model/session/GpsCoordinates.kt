package com.example.matchit.data.model.session

import android.os.Parcelable
import com.google.android.gms.maps.model.LatLng

@kotlinx.parcelize.Parcelize
data class GpsCoordinates(
    val long: Double,
    val lat: Double
) : Parcelable


fun GpsCoordinates.toLatLng(): LatLng {
    return LatLng(this.lat, this.long)
}