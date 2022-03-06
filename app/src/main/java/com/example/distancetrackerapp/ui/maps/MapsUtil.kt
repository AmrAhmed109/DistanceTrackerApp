package com.example.distancetrackerapp.ui.maps

import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.SphericalUtil
import java.text.DecimalFormat

object MapsUtil {

    fun setCameraPosition(location: LatLng): CameraPosition {
        return CameraPosition.builder()
            .target(location)
            .zoom(18f)
            .build()
    }

    fun calculateElapsedTime(startTime: Long, stopTime: Long): String {
        val elapsedTime = stopTime - startTime

        val hour = ((elapsedTime / (1000 * 60 * 60)) % 24)
        val min = ((elapsedTime / (1000 * 60)) % 60)
        val sec = (elapsedTime / 1000) % 60

        return "$hour : $min : $sec"
    }

    fun calculateDistance(locations: MutableList<LatLng>): String {
        if (locations.size > 1) {
            val meters = SphericalUtil.computeDistanceBetween(locations.first(), locations.last());
            val kilometers = meters / 1000
            return DecimalFormat("#.##").format(kilometers)
        }
        return "0.00"
    }
}