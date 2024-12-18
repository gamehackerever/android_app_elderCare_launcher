package com.srinand.induction

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import android.content.SharedPreferences
import android.view.MotionEvent
import android.widget.ScrollView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.OnSuccessListener
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.MapEventsOverlay

class LocationPickerActivity : AppCompatActivity() {

    private lateinit var mapView: MapView
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var marker: Marker
    private lateinit var locationTextView: TextView
    private lateinit var confirmLocationButton: TextView
    private lateinit var setMarkerAtCurrentLocationButton: TextView
    private var selectedLocation: GeoPoint? = null
    private lateinit var parentScrollView: ScrollView

    // Zoom level threshold below which we will apply a specific zoom (e.g., level 10)
    private val zoomThreshold = 10.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.content_pick_location)


        // Initialize views
        mapView = findViewById(R.id.osmMapView)
        locationTextView = findViewById(R.id.locationText)
        confirmLocationButton = findViewById(R.id.btnConfirmLocation)
        setMarkerAtCurrentLocationButton = findViewById(R.id.btnSetMarkerAtCurrentLocation)
        parentScrollView = findViewById(R.id.parentPage)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Configure the map
        setupMap()

        // Request location permission if not already granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fetchLocation()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1000)
        }

        // Button to confirm location
        confirmLocationButton.setOnClickListener {
            selectedLocation?.let { location ->
                saveLocationToSharedPreferences(location)
                val intent = Intent(this, EditContactsAct::class.java)
                startActivity(intent)
                Toast.makeText(this, "Location Confirmed!", Toast.LENGTH_SHORT).show()
            } ?: run {
                Toast.makeText(this, "Please select a location first", Toast.LENGTH_SHORT).show()
            }
        }

        // Button to set marker at current location
        setMarkerAtCurrentLocationButton.setOnClickListener {
            fetchLocation()
            Toast.makeText(this, "Current Location Set!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun fetchLocation() {
        fusedLocationClient.lastLocation.addOnSuccessListener(this, OnSuccessListener { location ->
            location?.let {
                placeMarkerAtLocation(it.latitude, it.longitude)
                locationTextView.text = "Selected Location:\n Lat: ${it.latitude}, Lon: ${it.longitude}"
            } ?: run {
                val defaultLatitude = 28.6139
                val defaultLongitude = 77.2090
                placeMarkerAtLocation(defaultLatitude, defaultLongitude)
                locationTextView.text = "Selected Location:\n Lat: ${defaultLatitude}, Lon: ${defaultLongitude}"
                Toast.makeText(this, "Current location is unavailable!", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun placeMarkerAtLocation(latitude: Double, longitude: Double) {
        val geoPoint = GeoPoint(latitude, longitude)

        // If no marker exists, create one
        if (!::marker.isInitialized) {
            marker = Marker(mapView).apply {
                position = geoPoint
                title = "Selected Location"
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            }
            mapView.overlays.add(marker)
        } else {
            marker.position = geoPoint
        }

        // Get the current zoom level of the map
        val zoomLevel = mapView.zoomLevelDouble

        // If zoom level is lower than threshold, zoom in and center
        if (zoomLevel > zoomThreshold) {
            mapView.controller.setZoom(18.0)  // Zoom in to a comfortable level
            mapView.controller.setCenter(geoPoint)  // Recenter map on the marker
        } else {
            mapView.controller.setCenter(geoPoint)  // Just recenter the map if zoomed in enough
        }

        selectedLocation = geoPoint
    }


    private fun setupMap() {
        Configuration.getInstance().load(applicationContext, getSharedPreferences("osmdroid", MODE_PRIVATE))
        mapView.setMultiTouchControls(true)
        mapView.setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE)
        mapView.setBuiltInZoomControls(true)
        mapView.controller.setZoom(18.0)

        // Handle map touch events to disable scroll
        mapView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // Disabling the parent ScrollView's scroll behavior when touching the map
                    parentScrollView.requestDisallowInterceptTouchEvent(true)
                    mapView.performClick()  // Important to override for accessibility
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    // Allowing the parent ScrollView's scroll behavior once touch on map ends
                    parentScrollView.requestDisallowInterceptTouchEvent(false)
                }
            }
            false // Consume the touch event so map can be interacted with
        }

        // Map event listener for user taps
        val mapEventsReceiver = object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                placeMarkerAtLocation(p.latitude, p.longitude)
                locationTextView.text = "Selected Location:\n Lat: ${
                    String.format(
                        "%.7f",
                        p.latitude
                    )
                }, Lon: ${String.format("%.7f", p.longitude)}"
                return true
            }

            override fun longPressHelper(p: GeoPoint): Boolean {
                return false
            }
        }

        // Add a map events overlay to the map
        val mapEventsOverlay = MapEventsOverlay(mapEventsReceiver)
        mapView.overlays.add(mapEventsOverlay)
    }

    private fun saveLocationToSharedPreferences(location: GeoPoint) {
        val sharedPreferences: SharedPreferences = getSharedPreferences("SelectedLocation", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putFloat("latitude", location.latitude.toFloat())
        editor.putFloat("longitude", location.longitude.toFloat())
        editor.apply()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1000 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            fetchLocation()
        } else {
            Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
        }
    }
}


