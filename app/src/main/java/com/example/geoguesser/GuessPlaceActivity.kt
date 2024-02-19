package com.example.geoguesser

import android.graphics.Color
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.geoguesser.databinding.ActivityGuessPlaceBinding
import com.example.geoguesser.databinding.ActivityMapsBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.OnStreetViewPanoramaReadyCallback
import com.google.android.gms.maps.StreetViewPanorama
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.SupportStreetViewPanoramaFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.material.floatingactionbutton.FloatingActionButton

class GuessPlaceActivity : AppCompatActivity(), OnStreetViewPanoramaReadyCallback,
    OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityGuessPlaceBinding
    private var marker: Marker? = null
    private lateinit var streetViewPanorama: StreetViewPanorama

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGuessPlaceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //StreetView Fragment
        val streetViewPanoramaFragment =
            supportFragmentManager
                .findFragmentById(R.id.streetviewpanorama) as SupportStreetViewPanoramaFragment
        streetViewPanoramaFragment.getStreetViewPanoramaAsync(this)

        //MapView Fragment
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map_Fragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        //Makes sure the map fragment is initially hidden until FAB is clicked
        supportFragmentManager.beginTransaction()
            .hide(mapFragment)
            .commit()

        //FAB used to toggle the visibility of the map fragment
        binding?.mapFAB?.setOnClickListener {
            Toast.makeText(this, "Floating Action Button Clicked!", Toast.LENGTH_SHORT).show()
            toggleMapVisibility()
        }

        //Fab for map fragment. Allows user to confirm their marker selection
        binding?.markLocationFAB?.setOnClickListener {

            if (marker != null) {
                // Marker is present, retrieve its position
                val markerPosition = marker!!.position
                val latitude = markerPosition.latitude
                val longitude = markerPosition.longitude

                // Show a toast with the latitude and longitude
                Toast.makeText(
                    this,
                    "Marker Location: Latitude $latitude, Longitude $longitude",
                    Toast.LENGTH_SHORT
                ).show()

                //Show the StreetView Marker
                val greenMarkerIcon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
                val streetViewLocation = streetViewPanorama.location.position

                if (streetViewLocation != null) {
                    // Add the StreetView marker directly to the map
                    mMap.addMarker(MarkerOptions().position(streetViewLocation).title("StreetView Marker").icon(greenMarkerIcon) )

                    // Move the camera to the StreetView location
                    mMap.moveCamera(CameraUpdateFactory.newLatLng(streetViewLocation))

                    //Draw a line between two markers
                    mMap.addPolyline(PolylineOptions()
                        .add(markerPosition,streetViewLocation)
                        .color(Color.BLUE))

                    //Calculate and print the distance of the polyline
                    val distance = calculateDistance(markerPosition, streetViewLocation)

                    // Toast the distance
                    Toast.makeText(this, "Distance: $distance meters", Toast.LENGTH_SHORT).show()
                }

                mMap.setOnMapClickListener(null)

            } else {
                // No marker present, show a toast asking the user to mark a location
                Toast.makeText(this, "Mark a location first!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onStreetViewPanoramaReady(streetViewPanorama: StreetViewPanorama) {
        val sanFrancisco = LatLng(37.754130, -122.447129)
        streetViewPanorama.setPosition(sanFrancisco)
        this.streetViewPanorama = streetViewPanorama
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.setOnMapClickListener { latLng ->
            // Call a function to add a marker at the clicked location
            addOrUpdateMarker(latLng)
        }
    }

    private fun addOrUpdateMarker(latLng: LatLng) {
        // Check if the marker is already present
        if (marker == null) {
            // If not, add a new marker
            marker = mMap.addMarker(MarkerOptions().position(latLng).title("Marker"))
        } else {
            // If present, update the position of the existing marker
            marker?.position = latLng
        }

        // Optionally, you can move the camera to the clicked location
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng))
    }

    // Function to calculate distance between two LatLng points
    private fun calculateDistance(latLng1: LatLng, latLng2: LatLng): Float {
        val location1 = Location("")
        location1.latitude = latLng1.latitude
        location1.longitude = latLng1.longitude

        val location2 = Location("")
        location2.latitude = latLng2.latitude
        location2.longitude = latLng2.longitude

        return location1.distanceTo(location2)
    }

    private fun toggleMapVisibility() {
        val mapFragment: Fragment = supportFragmentManager.findFragmentById(R.id.map_Fragment)!!
        val markLocationFAB: FloatingActionButton = findViewById(R.id.markLocationFAB)

        if (!mapFragment.isAdded) {
            // If map fragment is not added, add it and hide it
            supportFragmentManager.beginTransaction()
                .add(R.id.map_Fragment, mapFragment)
                .hide(mapFragment)
                .commit()
        }

        if (mapFragment.isVisible) {
            // If map is visible, hide it
            supportFragmentManager.beginTransaction()
                .hide(mapFragment)
                .commit()
            markLocationFAB.visibility = View.GONE

            //Clear existing markers
            //mMap.clear()

        } else {
            // If map is hidden, show it
            supportFragmentManager.beginTransaction()
                .show(mapFragment)
                .commit()
            markLocationFAB.visibility = View.VISIBLE
        }
    }
}