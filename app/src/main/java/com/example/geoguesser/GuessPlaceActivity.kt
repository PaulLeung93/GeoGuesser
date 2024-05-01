package com.example.geoguesser

import android.content.Intent
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.geoguesser.databinding.ActivityGuessPlaceBinding
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
import org.json.JSONException
import kotlin.random.Random


class GuessPlaceActivity : AppCompatActivity(), OnStreetViewPanoramaReadyCallback,
    OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityGuessPlaceBinding
    private var marker: Marker? = null
    private lateinit var streetViewPanorama: StreetViewPanorama

    //TextViews of the Scoreboard cardview
    private lateinit var scoreboardCardView: CardView
    private lateinit var tvScoreboard: TextView
    private lateinit var tvDistance: TextView
    private lateinit var tvPoints: TextView
    private lateinit var tvTotalScore: TextView
    private lateinit var tvRound: TextView

    var totalScore = 0
    var currentRound = 1
    var maxRounds = 5

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGuessPlaceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //Attaching views of our Scoreboard card
        scoreboardCardView = findViewById(R.id.scoreboardCardView)
        tvScoreboard = scoreboardCardView.findViewById(R.id.tvScoreboard)
        tvDistance = scoreboardCardView.findViewById(R.id.tvDistance)
        tvPoints = scoreboardCardView.findViewById(R.id.tvPoints)
        tvTotalScore = scoreboardCardView.findViewById(R.id.tvTotalScore)
        tvRound = scoreboardCardView.findViewById(R.id.tvRounds)

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
        binding.mapFAB.setOnClickListener {
            toggleMapVisibility()
        }

        //Confirm user's marker selection in the Map Fragment
        binding.markLocationFAB.setOnClickListener {
            markLocation()
        }
    }


    override fun onStreetViewPanoramaReady(streetViewPanorama: StreetViewPanorama) {
        this.streetViewPanorama = streetViewPanorama
        streetViewPanorama.isStreetNamesEnabled = false  //Removes street names

        val coordinates = generateRandomLocation()
        checkStreetViewAvailability(coordinates)
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

        // Move the camera to the clicked location
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng))
    }

    //Confirms the user marker selection in the Map Fragment
    private fun markLocation() {
        //If marker is present on the map fragment, confirms their selection
        if (marker != null) {

            //Hide Mark Location button so user cannot make another selection
            binding.markLocationFAB.visibility = View.GONE

            // Marker is present, retrieve its position
            val markerPosition = marker!!.position

            //Show the StreetView Marker after successful marker selection
            val greenMarkerIcon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
            val streetViewLocation = streetViewPanorama.location.position

            if (streetViewLocation != null) {
                // Add the StreetView marker directly to the map
                mMap.addMarker(MarkerOptions().position(streetViewLocation).title("StreetView Marker").icon(greenMarkerIcon) )

                // Move the camera to the StreetView location
                //mMap.moveCamera(CameraUpdateFactory.newLatLng(streetViewLocation))

                // Move the camera to the StreetView location
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(streetViewLocation, 14f)) // Adjust zoom level as needed



                //Draw a line between two markers
                mMap.addPolyline(PolylineOptions()
                    .add(markerPosition,streetViewLocation)
                    .color(Color.BLUE))

                //Calculate and print the distance of the polyline
                val distance = calculateDistance(markerPosition, streetViewLocation)

                //Update Scoreboard
                scoreboardCardView.visibility = View.VISIBLE

                //Calculate the score. Allow points within the distance of 10000 meters, with a multiplier of 1000
                val points = calculateScore(distance, 10000, 1000)

                tvDistance.text = "$distance meters"
                tvPoints.text = "$points"
                totalScore = totalScore + points
                tvTotalScore.text = "$totalScore"

                tvRound.text = "$currentRound / $maxRounds"


                //TODO: Simplify round checking into own function
                //Check the current round
                if (currentRound < maxRounds) {
                    // Increment current round and start a new round
                    currentRound++

                    //Click Button to start a new round
                    binding.btnNewRound.setOnClickListener {
                        startNewRound()
                    }

                //If it's the last round, display New Game and Main Menu Buttons
                } else {
                    //Display New Game Button, reset round variables, display name prompt for ROOM

                    //Make next round button invisible
                    binding.btnNewRound.visibility = View.GONE

                    //Make new game button visible
                    binding.btnNewGame.visibility = View.VISIBLE

                    //Make Main Menu button visible
                    binding.btnMainMenu.visibility = View.VISIBLE

                    //Click new game button
                    binding.btnNewGame.setOnClickListener {
                        //new game function (clears previous data)
                        startNewGame()
                        Toast.makeText(this, "NEW GAME", Toast.LENGTH_SHORT).show()
                    }

                    binding.btnMainMenu.setOnClickListener {
                        startActivity(Intent(this,MainActivity::class.java))
                    }

                    Toast.makeText(this, "Game Over! Total Score: $totalScore", Toast.LENGTH_SHORT).show()
                }

            }
            //Set marker back to null after User is finished
            mMap.setOnMapClickListener(null)

        //Else User has not selected a marker
        } else {
            // No marker present, show a toast asking the user to mark a location
            Toast.makeText(this, "Mark a location first!", Toast.LENGTH_SHORT).show()
        }
    }


    // Function to calculate distance between two LatLng points
    private fun calculateDistance(latLng1: LatLng, latLng2: LatLng): Int {
        val location1 = Location("")
        location1.latitude = latLng1.latitude
        location1.longitude = latLng1.longitude

        val location2 = Location("")
        location2.latitude = latLng2.latitude
        location2.longitude = latLng2.longitude

        return location1.distanceTo(location2).toInt()
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
        } else {
            // If map is hidden, show it
            supportFragmentManager.beginTransaction()
                .show(mapFragment)
                .commit()
            markLocationFAB.visibility = View.VISIBLE
        }
    }


    //Generating random coordinates (NYC example)
    private fun generateRandomLocation(): LatLng {
        // Define geographical bounds
        val minLatitude = 40.70
        val maxLatitude = 40.78
        val minLongitude = -73.98
        val maxLongitude = -73.90

        //USA
//        val minLatitude = 24.396308
//        val maxLatitude = 49.384358
//        val minLongitude = -125.0
//        val maxLongitude = -66.93457

        val latitude = Random.nextDouble(minLatitude, maxLatitude)
        val longitude = Random.nextDouble(minLongitude, maxLongitude)

        return LatLng(latitude,longitude)
    }

    private fun checkStreetViewAvailability(coordinates: LatLng) {
        val apiKey = com.example.geoguesser.BuildConfig.MAPS_API_KEY
        val streetViewApiUrl =
            "https://maps.googleapis.com/maps/api/streetview/metadata?location=${coordinates.latitude},${coordinates.longitude}&key=$apiKey"

        val queue = Volley.newRequestQueue(this)

        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.GET, streetViewApiUrl, null,
            { response ->
                try {
                    // Check if Street View imagery is available
                    val streetViewAvailable = response.getString("status") == "OK"
                    val locationType = response.optString("location_type")

                    if (streetViewAvailable && locationType != "indoors") {
                        // Street View is available, set the panorama position
                        streetViewPanorama.setPosition(coordinates)
                    } else {
                        // Street View is not available, generate new coordinates
                        val newCoordinates = generateRandomLocation()
                        checkStreetViewAvailability(newCoordinates)
                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            },
            { error ->
                error.printStackTrace()
                // Handle errors
            }
        )

        queue.add(jsonObjectRequest)
    }

    //Calculate Scoreboard
    private fun calculateScore(distance: Int, maxDistance: Int, maxScore: Int): Int {
        val score: Int
        if (distance <= maxDistance) {
            // Use a linear mapping formula to calculate the score
            score = ((maxDistance - distance) / maxDistance.toFloat() * maxScore).toInt()
        } else {
            return 0 // If the distance exceeds the maximum, score is 0
        }
        return score
    }

    //Starting a new round
    private fun startNewRound() {
        // Reset necessary variables for a new round
        // Clear existing markers
        mMap.clear()
        toggleMapVisibility()
        resetCameraPosition()

        scoreboardCardView.visibility = View.INVISIBLE

        // Generate new random location
        val coordinates = generateRandomLocation()
        // Fetch street view for the new location
        checkStreetViewAvailability(coordinates)

        // Reset marker to null
        marker = null

        // Set map click listener again to allow user to choose a new marker
        mMap.setOnMapClickListener { latLng ->
        // Call a function to add a marker at the clicked location
        addOrUpdateMarker(latLng)
        }

        tvDistance.text = ""
        tvPoints.text = ""
    }

    //Starting a new game
    private fun startNewGame() {
        mMap.clear()
        toggleMapVisibility()
        resetCameraPosition()

        scoreboardCardView.visibility = View.INVISIBLE
        binding.btnNewRound.visibility = View.VISIBLE
        binding.btnNewGame.visibility = View.GONE
        binding.btnMainMenu.visibility = View.GONE

        tvDistance.text = ""
        tvPoints.text = ""
        tvTotalScore.text = ""

        totalScore = 0
        currentRound = 1

        // Generate new random location
        val coordinates = generateRandomLocation()
        // Fetch street view for the new location
        checkStreetViewAvailability(coordinates)

        // Reset marker to null
        marker = null

        // Set map click listener again to allow user to choose a new marker
        mMap.setOnMapClickListener { latLng ->
            // Call a function to add a marker at the clicked location
            addOrUpdateMarker(latLng)
        }

    }

    private fun resetCameraPosition() {
        // Define the default LatLng position and zoom level
        val defaultLatLng = LatLng(0.0, 0.0)
        val defaultZoom = 2f

        // Animate the camera to the default position and zoom level
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(defaultLatLng, defaultZoom))
    }

}