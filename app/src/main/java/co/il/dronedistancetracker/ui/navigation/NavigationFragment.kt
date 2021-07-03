package co.il.dronedistancetracker.ui.navigation

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import co.il.dronedistancetracker.R
import co.il.dronedistancetracker.data.models.DroneTravel
import co.il.dronedistancetracker.misc.CameraAndViewPort
import co.il.dronedistancetracker.misc.setCustomStyle
import co.il.dronedistancetracker.viewmodels.MainViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.maps.android.data.kml.KmlLayer
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_navigation.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


@AndroidEntryPoint
class NavigationFragment : Fragment(R.layout.fragment_navigation), OnMapReadyCallback {

    private val mainViewModel by viewModels<MainViewModel>()
    private lateinit var map: GoogleMap
    private lateinit var kmlLayer: KmlLayer
    private var insidePolygon = false
    private var firstRun = 0


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupLoadingView()
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
        mapFragment!!.getMapAsync(this)
        subscribeToObservers()

    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        setupMap()
    }

    //ADDED LOADING VIEW TO ADD SOME SECONDS TO LOAD MAP AND PREPARE POLYGON
    private fun setupLoadingView() = lifecycleScope.launch {
        withContext(Dispatchers.Main) {
            activity?.nav_view?.isVisible = false
        }
        delay(4000)
        withContext(Dispatchers.Main) {

            loadingView?.visibility = View.GONE
            activity?.nav_view?.isVisible = true
        }
    }

    //KML DOCUMENTATION READ: 1 Hour
    //LINK: https://developers.google.com/maps/documentation/android-sdk/utility/kml
    private fun setupMap() {

        //Creating basic KML Layer
        kmlLayer = KmlLayer(map, R.raw.allowed_area, context)
        kmlLayer.addLayerToMap()
        //Passing KML Layer to View model for creating accurate Polygon
        mainViewModel.extractCoords(kmlLayer)

        //Basic Map Setup
        map.moveCamera(CameraUpdateFactory.newCameraPosition(CameraAndViewPort.telAviv))
        map.isBuildingsEnabled = true
        map.setCustomStyle(requireContext())
        map.uiSettings.apply {
            isZoomGesturesEnabled = true
            isZoomControlsEnabled = false
            isCompassEnabled = false
            isMapToolbarEnabled = false
        }

        //KML PARSING TO STRING AND FROM STRING TO LATLNG:
        //TRIED A LOT OF METHODS OF WORKING WITH THE COORDS, In the end i came up with this method
        mainViewModel.parseKmlToLatLng(kmlLayer.containers)

        map.setOnMapClickListener {
            addPolyLine(it)
        }

    }

    //ADDING POLYLINES:
    //DOCUMENTATION READ: https://developers.google.com/maps/documentation/android-sdk/polygon-tutorial
    private fun addPolyLine(newLocation: LatLng) = lifecycleScope.launch(Dispatchers.Default) {

        withContext(Dispatchers.Main) {
            lottieLoading.isVisible = true
        }
        delay(2000)
        withContext(Dispatchers.Main) {
            lottieLoading.isVisible = false
        }

            val shortestDistance = mainViewModel.calcShortRoute(newLocation)
            withContext(Dispatchers.Main) {
                val polyline = map.addPolyline(
                    PolylineOptions().apply {
                        add(newLocation, shortestDistance)
                        width(15f)
                        color(Color.CYAN)
                        endCap(RoundCap())
                    }
                )
            }
            withContext(Dispatchers.Main) {
                addNewMarker(newLocation)
            }



    }

    //ADDING START POINT MARKER WITH CUSTOM DRONE ICON
    private fun addNewMarker(location: LatLng) {
        val startPoint = map.addMarker(
            MarkerOptions()
                .position(location)
                .title("Start point")
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.drone))
        )
    }

    //ADDING KML LAYER TO MAP
    //REMOVING THE LAYER RIGHT AFTER PARSING THE COORDS TO CREATE MORE ACCURATE POLYGON
    private fun addKmlPolygon(latLngList: ArrayList<LatLng>)  {
        if(latLngList.size > 100) {
            val kmlMap = map.addPolygon(
                PolygonOptions().apply {
                    addAll(latLngList)
                    strokeWidth(20f)
                    strokeColor(Color.CYAN)
                }
            )
            kmlLayer.removeLayerFromMap()
        }
    }

    //SUBCRIBING TO LIVEDATA CHANGES FROM VIEWMODEL
    @SuppressLint("SetTextI18n")
    private fun subscribeToObservers() {
        mainViewModel.kmlCoords.observe(viewLifecycleOwner, {
            lifecycleScope.launch(Dispatchers.Main) {
                delay(2000)
                addKmlPolygon(it)
            }
        })

        mainViewModel.distance.observe(viewLifecycleOwner, {
            tvDistance.text = "DISTANCE: $it KM"
        })

        mainViewModel.travelTime.observe(viewLifecycleOwner, {
            tvTravelTime.text = "FLIGHT TIME: $it MIN"
            val travel = DroneTravel(mainViewModel.distance.value!!, it)
            mainViewModel.insertTravel(travel)
            Log.d("TRAVELS", travel.toString())
        })

        mainViewModel.allTravels.observe(viewLifecycleOwner, {
            Log.d("TRAVELS", "DATABASE SIZE ${it.size}")
        })

        mainViewModel.droneIsInside.observe(viewLifecycleOwner, { inside ->
            if(inside) {
                insidePolygon = inside
                firstRun++
                tvInside.text = "Drone inside polygon"
                Toast.makeText(context, "Drone is inside the polygon", Toast.LENGTH_SHORT).show()
            } else {
                if(firstRun >= 1) {
                    tvInside.text = "Drone outside polygon"
                    Toast.makeText(context, "Drone is outside the polygon", Toast.LENGTH_SHORT)
                        .show()
                    insidePolygon = inside
                } else {
                    firstRun++
                }
            }
        })
    }

}