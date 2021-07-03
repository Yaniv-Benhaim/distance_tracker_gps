package co.il.dronedistancetracker.misc

import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng

object CameraAndViewPort {

    val telAviv: CameraPosition = CameraPosition.builder()
        .target(LatLng(32.07882010000863, 34.790416514658150))
        .bearing(67f)
        .tilt(45f)
        .zoom(14f)
        .build()


    fun getCameraPosition(newLocation: LatLng): CameraPosition {
        return CameraPosition.builder()
            .target(newLocation)
            .zoom(50f)
            .build()
    }

}