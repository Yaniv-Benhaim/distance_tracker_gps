package co.il.dronedistancetracker.utils

import com.google.android.gms.maps.model.LatLng
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin

object ShortestDistanceFormula {

    //Haversine Formula for calculating distance between two points
    //Link to formula: https://community.esri.com/t5/coordinate-reference-systems/distance-on-a-sphere-the-haversine-formula/ba-p/902128

    fun distance(
        latA: LatLng,
        latB: LatLng
    ): Double {
        return if (latA.latitude == latB.latitude && latA.longitude == latB.longitude) {
            0.0
        } else {
            val lat1 = latA.latitude
            val lon1 = latA.longitude
            val lat2 = latB.latitude
            val lon2 = latB.longitude

            val theta = lon1 - lon2
            var dist = sin(Math.toRadians(lat1)) * sin(Math.toRadians(lat2)) + cos(
                Math.toRadians(lat1)
            ) * cos(Math.toRadians(lat2)) * cos(Math.toRadians(theta))
            dist = acos(dist)
            dist = Math.toDegrees(dist)
            dist *= 60 * 1.1515
            dist *= 1.609344
            dist
        }
    }


}