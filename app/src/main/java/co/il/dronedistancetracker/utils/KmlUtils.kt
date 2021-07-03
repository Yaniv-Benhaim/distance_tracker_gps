package co.il.dronedistancetracker.utils

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.data.kml.KmlLayer
import kotlinx.coroutines.*



class KmlUtils {

    private val _polygonList = MutableLiveData<ArrayList<LatLng>>()
    val polygonList: LiveData<ArrayList<LatLng>> = _polygonList

    private val _centerPoints = MutableLiveData<ArrayList<LatLng>>()
    val centerPoints: LiveData<ArrayList<LatLng>> = _centerPoints


    //EXTRACTING COORDS FROM KML AND CONVERTING TO ONE BIG STRING
    fun extractCoords(kmlLayer: KmlLayer) = GlobalScope.launch(Dispatchers.Default) {

        for (c in kmlLayer.containers) {
            if (c.hasPlacemarks()) {
                for (p in c.placemarks) {
                    val g = p.geometry
                    val `object`: Any = g.geometryObject

                    if (`object` is List<*>) {
                        parseString(`object`.toString())
                    }
                }
            }
        }
    }

    //CONVERTING STRING TO A LIST OF LATLNG COORDS
    private suspend fun parseString(kmlAsString: String) {
        var basicPoints = ArrayList<LatLng>()
        val splitCoords = kmlAsString.split("lat/lng: (").toTypedArray()
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
                basicPoints.add(point)
                Log.d("KML_UTILS", "${basicPoints.size}")
            }
        }
        createPrecisePolygon(basicPoints)
    }


    /*
    THIS FUNCTION CREATES A LIST WITH MORE POLYGONS THROUGH MULTIPLE CYCLES
    CYCLE ONE, TWO, THREE, FOUR
    1 CALCULATE MID POINT BETWEEN A POINT IN LIST AND THE NEXT POINT IN LIST
    2 AFTER CREATING A NEW MID POINT WE ADD IT TO THE LIST
    3 I REPEATED THIS MULTIPLE TIMES UNTIL I WENT FROM 118 POINTS TO ABOVE 2600 POINTS
    */

    private suspend fun createPrecisePolygon(basicPoints: ArrayList<LatLng>) {
        withContext(Dispatchers.Main) {
            _polygonList.value = arrayListOf()
        }
        val newPolygonPoints = ArrayList<LatLng>()
        val cycleOne = ArrayList<LatLng>()
        val cycleTwo = ArrayList<LatLng>()
        val cycleThree = ArrayList<LatLng>()

        //Cycle 1 in increasing points for accurate calculations
        basicPoints.forEachIndexed { index, point ->
            if (index < basicPoints.size -1) {
                newPolygonPoints.add(point)
                val pointOneLat = point.latitude
                val pointOneLon = point.longitude
                val pointTwoLat = basicPoints[index + 1].latitude
                val pointTwoLon = basicPoints[index + 1].longitude

                val avgLat = (pointOneLat + pointTwoLat) / 2
                val avgLon = (pointOneLon + pointTwoLon) / 2
                newPolygonPoints.add(LatLng(avgLat, avgLon))
                Log.d("KML_UTILS", newPolygonPoints.size.toString())
            }

        }

        //Cycle 2 in increasing points for accurate calculations
        newPolygonPoints.forEachIndexed { index, point ->
            if (index < newPolygonPoints.size -1) {
                cycleOne.add(point)
                val pointOneLat = point.latitude
                val pointOneLon = point.longitude
                val pointTwoLat = newPolygonPoints[index + 1].latitude
                val pointTwoLon = newPolygonPoints[index + 1].longitude

                val avgLat = (pointOneLat + pointTwoLat) / 2
                val avgLon = (pointOneLon + pointTwoLon) / 2
                cycleOne.add(LatLng(avgLat, avgLon))
            }

        }

        //Cycle 3 in increasing points for accurate calculations
        cycleOne.forEachIndexed { index, point ->
            if (index < cycleOne.size -1) {
                cycleTwo.add(point)
                val pointOneLat = point.latitude
                val pointOneLon = point.longitude
                val pointTwoLat = cycleOne[index + 1].latitude
                val pointTwoLon = cycleOne[index + 1].longitude

                val avgLat = (pointOneLat + pointTwoLat) / 2
                val avgLon = (pointOneLon + pointTwoLon) / 2
                cycleTwo.add(LatLng(avgLat, avgLon))
            }

        }

        //Cycle 4 in increasing points for accurate calculations
        cycleTwo.forEachIndexed { index, point ->
            if (index < cycleTwo.size -1) {
                cycleThree.add(point)
                val pointOneLat = point.latitude
                val pointOneLon = point.longitude
                val pointTwoLat = cycleTwo[index + 1].latitude
                val pointTwoLon = cycleTwo[index + 1].longitude

                val avgLat = (pointOneLat + pointTwoLat) / 2
                val avgLon = (pointOneLon + pointTwoLon) / 2
                cycleThree.add(LatLng(avgLat, avgLon))
            }

        }

        withContext(Dispatchers.Main) {

        }
        //Mapping new points to livedata from 118 points to above 2066 points
        cycleThree.forEach {
            _polygonList.value!!.add(it)
        }

        //Adding extra points between last and first point in list

            //Mid point
            val lat = (cycleThree.last().latitude + cycleThree.first().latitude)/2
            val lon = (cycleThree.last().longitude + cycleThree.first().longitude)/2
            val newPoint = LatLng(lat, lon)

            //Quarter point
            val lat2 = (cycleThree.first().latitude + newPoint.latitude)/2
            val lon2 = (cycleThree.first().longitude + newPoint.longitude)/2
            val quarterPoint = LatLng(lat2, lon2)


            //Three quarter point
            val lat1 = (newPoint.latitude + cycleThree.last().latitude)/2
            val lon1 = (newPoint.longitude + cycleThree.last().longitude)/2
            val threeQuarterPoint = LatLng(lat1, lon1)

            _polygonList.value!!.add(quarterPoint)
            _polygonList.value!!.add(newPoint)
            _polygonList.value!!.add(threeQuarterPoint)

        //Creating list of center points to calculate if drone is inside the polygon
        createCenterPoints()

        Log.d("KML_UTILS", "Size of coords after cycle three " + _polygonList.value?.size.toString())
    }

    //ADDING & CREATING RANDOM TEST POINTS WITHIN POLYGON TO LIST OF LIVEDATA
    private suspend fun createCenterPoints() {

        //CREATING RANDOM TEST POINTS WITHIN POLYGON
        withContext(Dispatchers.Main) {
            _centerPoints.value = arrayListOf()
        }
        withContext(Dispatchers.Main) {
            //Creating some random points for testing
            _centerPoints.value!!.add(LatLng(32.07058, 34.79616))
            _centerPoints.value!!.add(LatLng(32.07654, 34.79676))
            _centerPoints.value!!.add(LatLng(32.07562, 34.79247))
            _centerPoints.value!!.add(LatLng(32.07678, 34.79439))
            _centerPoints.value!!.add(LatLng(32.07526, 34.79896))
            _centerPoints.value!!.add(LatLng(32.07033, 34.79827))
            _centerPoints.value!!.add(LatLng(32.07677, 34.7932))
            _centerPoints.value!!.add(LatLng(32.07756, 34.79421))
            _centerPoints.value!!.add(LatLng(32.07725, 34.79368))
            _centerPoints.value!!.add(LatLng(32.06454, 34.79475))
            _centerPoints.value!!.add(LatLng(32.07787, 34.79452))
            _centerPoints.value!!.add(LatLng(32.0786, 34.79948))

        }
    }
}