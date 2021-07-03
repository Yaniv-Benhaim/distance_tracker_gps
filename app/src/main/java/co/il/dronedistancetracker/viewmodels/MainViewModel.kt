package co.il.dronedistancetracker.viewmodels

import android.content.Context
import android.provider.Settings
import android.util.Log
import android.view.MotionEvent
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.il.dronedistancetracker.data.models.DroneTravel
import co.il.dronedistancetracker.repositories.Repo
import co.il.dronedistancetracker.utils.KmlUtils
import co.il.dronedistancetracker.utils.ShortestDistanceFormula
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.data.kml.KmlContainer
import com.google.maps.android.data.kml.KmlLayer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import javax.inject.Inject
import kotlin.math.roundToInt

@HiltViewModel
class MainViewModel @Inject constructor(private val utils: KmlUtils, private val repo: Repo) : ViewModel() {

    //Livedata<List<LatLng>> generated from KML Layer
    private val _kmlCoords = MutableLiveData<ArrayList<LatLng>>()
    val kmlCoords: LiveData<ArrayList<LatLng>> = _kmlCoords

    //Livedata<List<LatLng>> generated from kmlCoords with increased accuracy for pathfinding
    private val preciseCoords: LiveData<ArrayList<LatLng>> = utils.polygonList

    //Random points within the polygon
    private val centerPoints: LiveData<ArrayList<LatLng>> = utils.centerPoints

    //Livedata to update if drone is within the polygon
    private val _droneIsInside = MutableLiveData<Boolean>(false)
    val droneIsInside: LiveData<Boolean> = _droneIsInside

    //DISTANCE TO POLYGON IN KM
    private val _distance = MutableLiveData<Double>(0.0)
    val distance: LiveData<Double> = _distance

    //TIME TO TRAVEL TO POLYGON WITH DRONE
    private val _travelTime = MutableLiveData<Double>(0.0)
    val travelTime: LiveData<Double> = _travelTime

    //ALL PAST CHECKS FROM ROOM DATABASE
    val allTravels = repo.getAllTravels()

    private var shortIndex = 0


    //Inserting every distance check in to room database
    fun insertTravel(travel: DroneTravel) = viewModelScope.launch {
        repo.insertTravel(travel)
    }

    fun extractCoords(kmlLayer: KmlLayer) = utils.extractCoords(kmlLayer)
    //Parsing KML Layer coordinates as string
    fun parseKmlToLatLng(containers: Iterable<KmlContainer>) {
        for (c in containers) {
            if (c.hasPlacemarks()) {
                for (p in c.placemarks) {
                    val g = p.geometry
                    val `object`: Any = g.geometryObject

                    if (`object` is List<*>) {
                        parseString(`object`.toString())
                    }
                }
            }
            if (c.hasContainers()) {
                parseKmlToLatLng(c.containers)
            }
        }
    }

    //Parsing Coordinates from string to list of LatLng
    private fun parseString(kmlAsString: String) = viewModelScope.launch {
        val splitCoords = kmlAsString.split("lat/lng: (").toTypedArray()
        _kmlCoords.value = ArrayList()

        splitCoords.forEachIndexed { index, str ->
            val newStr = str
                .replace("),", "")
                .replace("[", "")
                .replace("]", "")
                .replace(" ", "")
                .replace(")", "")
            val split2Coords = newStr.split(",").toTypedArray()

            if(newStr.length > 5 && index != split2Coords.lastIndex) {
                val lat = split2Coords[0].toDouble()
                val lng = split2Coords[1].toDouble()
                val point = LatLng(lat, lng)
                _kmlCoords.value?.add(point)
                Log.d("VIEWMODEL_TESTING", "${_kmlCoords.value?.size}")
            }
        }

    }

    //Calculating a route to the closest point from list of precise points generated above
    //HAVERSINE FORMULA USED TO DETERMINE DISTANCE BETWEEN POINTS
    suspend fun calcShortRoute(startPoint: LatLng): LatLng {


       var shortestPoint = LatLng(0.0,0.0)
       var distance: Double = Double.MAX_VALUE
       var shortestIndex = 0

        val job = GlobalScope.launch(Dispatchers.IO) {
            preciseCoords.value?.forEachIndexed { index, point ->
                val newDistance = ShortestDistanceFormula.distance(startPoint, point)
                if (newDistance < distance) {
                    distance = newDistance
                    shortestPoint = point
                    shortestIndex = index
                }
            }
        }
        job.join()
        shortIndex = shortestIndex
        //Checking if points is inside the polygon
        isInside(startPoint, shortestPoint)

        //Updating livedata with distance to shortest point in KM
        withContext(Dispatchers.Main) {
            val distance3digits: Double = String.format("%.3f", distance).toDouble()
            val travelTime3Digits: Double = String.format("%.2f", (distance3digits/30.0) * 60).toDouble()
            _distance.value = distance3digits
            _travelTime.value = travelTime3Digits
        }
        return shortestPoint
    }

    //Function for checking if points is inside
    /*
    1 CHECKS DISTANCE BETWEEN FASTEST POINT AND RANDOM CLOSE POINT IN POLYGON
    2 IF DISTANCE BETWEEN START POINT AND POINT WITHIN POLYGON IS SMALLER THEN -
      DISTANCE BETWEEN CLOSEST POINT AND POINT WITHIN POLYGON.
      THEN POINT MUST BE INSIDE.

      ACCURACY AROUND 99%
      DIDN'T WANT TO USE ANY KNOWN METHODS FOR DETERMINING IF POINT IS INSIDE POLYGON.
      LIKE: CHECKING ODD/EVEN TRIANGLES AND CHECKING IF HORIZONTAL LINES CROSS POLYGON BOUNDERY.

      THIS IS THE BEST I CAME UP WITH FOR NOW
     */
    private suspend fun isInside(startPoint: LatLng, shortestPoint: LatLng) {

        var shortestCenterPoint = LatLng(0.0,0.0)
        var distanceToCenterPoint = Double.MAX_VALUE
        centerPoints.value!!.forEach {
            val shortToCenter = ShortestDistanceFormula.distance(startPoint, it)
            if (shortToCenter < distanceToCenterPoint) {
                shortestCenterPoint = LatLng(it.latitude, it.longitude)
                distanceToCenterPoint = shortToCenter
            }
        }

        val startToCenter = ShortestDistanceFormula.distance(startPoint, shortestCenterPoint)
        val shortToCenter = ShortestDistanceFormula.distance(shortestPoint, shortestCenterPoint)
        if(startToCenter < shortToCenter) {
            withContext(Dispatchers.Main) {
                _droneIsInside.value = startToCenter < shortToCenter
                Log.d("INSIDE", "Distance start to center: $startToCenter centerpoint: $shortestCenterPoint")
                Log.d("INSIDE", "Distance shortest point to center: $shortToCenter")
                Log.d("INSIDE", "Shortest Center Point: $shortestCenterPoint")
            }

        } else {
            withContext(Dispatchers.Main) {
                _droneIsInside.value = startToCenter < shortToCenter
                Log.d("INSIDE", "Distance start to center: $startToCenter centerpoint: $shortestCenterPoint")
                Log.d("INSIDE", "Distance shortest point to center: $shortToCenter")
                Log.d("INSIDE", "Shortest Center Point: $shortestCenterPoint")
            }
        }

//        var index = 0
//        while(index < centerPoints.value!!.size-1 ) {
//            val centerPoint = centerPoints.value!![index]
//            val startToCenter = ShortestDistanceFormula.distance(startPoint, centerPoint)
//            val shortToCenter = ShortestDistanceFormula.distance(shortestPoint, centerPoint)
//            if(startToCenter < shortToCenter) {
//                withContext(Dispatchers.Main) {
//                    _droneIsInside.value = startToCenter < shortToCenter
//                    Log.d("INSIDE", "Distance start to center: ${startToCenter} centerpoint: $centerPoint")
//                    Log.d("INSIDE", "Distance shortest point to center: ${shortToCenter}")
//                    Log.d("INSIDE", "Shortest Center Point: ${shortestCenterPoint}")
//                }
//                index = centerPoints.value!!.size
//            } else {
//                index++
//            }
//            if(index == centerPoints.value!!.size-2) {
//                withContext(Dispatchers.Main) {
//                    _droneIsInside.value = startToCenter < shortToCenter
//                    Log.d("INSIDE", "Distance start to center: ${startToCenter} centerpoint: $centerPoint")
//                    Log.d("INSIDE", "Distance shortest point to center: ${shortToCenter}")
//                }
//            }
//        }
    }

}