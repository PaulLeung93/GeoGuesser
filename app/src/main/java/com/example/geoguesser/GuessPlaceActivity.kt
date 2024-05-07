package com.example.geoguesser

import android.content.Intent
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONException
import kotlin.random.Random


class GuessPlaceActivity : AppCompatActivity(), OnStreetViewPanoramaReadyCallback,
    OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityGuessPlaceBinding
    private var marker: Marker? = null
    private lateinit var streetViewPanorama: StreetViewPanorama

    //Variables used to update the scoreboard views
    var totalScore = 0
    var currentRound = 1
    var maxRounds = 5

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

    //Updates the marker to where the user clicks on the map fragment
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

            // Add the StreetView marker directly to the map
            mMap.addMarker(MarkerOptions().position(streetViewLocation).title("StreetView Marker").icon(greenMarkerIcon) )

            // Move the camera to the StreetView location
            mMap.moveCamera(CameraUpdateFactory.newLatLng(streetViewLocation))

            //Draw a line between two markers
            mMap.addPolyline(PolylineOptions()
                .add(markerPosition,streetViewLocation)
                .color(Color.BLUE))

            //Update Scoreboard views
            updateScoreboard(markerPosition, streetViewLocation)

            //Checks the current round of the game, determines whether to display NewRound or NewGame button
            checkCurrentRound()

            //Set marker back to null after User is finished
            mMap.setOnMapClickListener(null)

        //Else User has not selected a marker, display toast
        } else {
            // No marker present, show a toast asking the user to mark a location
            Toast.makeText(this, "Mark a location first!", Toast.LENGTH_SHORT).show()
        }
    }

    //Updates the Scoreboard cardview to display the new distance,points, and total score
    private fun updateScoreboard(markerGuess: LatLng, markerAnswer: LatLng){
        binding.scoreboardCardView.visibility = View.VISIBLE

        //Calculate and print the distance of the polyline
        val distance = calculateDistance(markerGuess, markerAnswer)

        //Calculate the score. Allow points within the distance of 10000 meters, with a multiplier of 1000
        //Maximum 1k points per round, Perfect score is 5k points
        val points = calculateScore(distance, 10000, 1000)

        //Update scoreboard views with updated values
        binding.tvDistance.text = "$distance meters"
        binding.tvPoints.text = "$points"
        totalScore = totalScore + points
        binding.tvTotalScore.text = "$totalScore"
        binding.tvRounds.text = "$currentRound / $maxRounds"
    }

    // Function to calculate distance between two LatLng points
    private fun calculateDistance(markerGuess: LatLng, markerAnswer: LatLng): Int {
        val location1 = Location("")
        location1.latitude = markerGuess.latitude
        location1.longitude = markerGuess.longitude

        val location2 = Location("")
        location2.latitude = markerAnswer.latitude
        location2.longitude = markerAnswer.longitude

        return location1.distanceTo(location2).toInt()
    }

    //This function displays/hides the MapFragment when the user selects the map FAB.
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

    //Checks if the streetview panorama exists for the given coordinates
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

    //Check the current Round, determines whether to display NewRound or NewGame Button
    private fun checkCurrentRound() {
        if (currentRound < maxRounds) {
            // Increment current round and start a new round
            currentRound++

            //Click Button to start a new round
            binding.btnNewRound.setOnClickListener {
                startNewRound()
            }

        //If it's the last round, display New Game and Main Menu Buttons. Reset scoreboard values
        } else {
            //Displays dialog box to allow users to input their name and record their score
            showNameInputDialog(totalScore)

            //Next round button is invisible because this is the last round
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

    //Starting a new round
    private fun startNewRound() {
        // Reset necessary variables for a new round
        // Clear existing markers
        mMap.clear()
        toggleMapVisibility()
        resetCameraPosition()

        binding.scoreboardCardView.visibility = View.INVISIBLE

        val coordinates = generateRandomLocation()   // Generate new random location
        checkStreetViewAvailability(coordinates)   // Fetch street view for the new location

        // Reset marker to null
        marker = null

        // Set map click listener again to allow user to choose a new marker
        mMap.setOnMapClickListener { latLng ->
            // Call a function to add a marker at the clicked location
            addOrUpdateMarker(latLng)
        }

        binding.tvDistance.text = ""
        binding.tvPoints.text = ""
    }

    //Starting a new game
    private fun startNewGame() {
        mMap.clear()
        toggleMapVisibility()
        resetCameraPosition()

        binding.scoreboardCardView.visibility = View.INVISIBLE
        binding.btnNewRound.visibility = View.VISIBLE
        binding.btnNewGame.visibility = View.GONE
        binding.btnMainMenu.visibility = View.GONE

        binding.tvDistance.text = ""
        binding.tvPoints.text = ""
        binding.tvTotalScore.text = ""

        totalScore = 0
        currentRound = 1

        val coordinates = generateRandomLocation()   // Generate new random location
        checkStreetViewAvailability(coordinates)  // Fetch street view for the new location

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

    //Alert dialog that displays an EditText to enter the user's name
    private fun showNameInputDialog(score: Int) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Enter your name")

        val input = EditText(this)
        builder.setView(input)

        builder.setPositiveButton("OK") { dialog, _ ->
            val playerName = input.text.toString().trim()
            if (playerName.isNotEmpty()) {
                saveScoreToDatabase(playerName, score)
                dialog.dismiss() // Close the dialog box after submission
            }
        }

        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }

        builder.show()
    }

    //Saves the user's name and score to a Room database
    private fun saveScoreToDatabase(playerName: String, score: Int) {
        val scoreEntity = Score(playerName = playerName, score = score)

        lifecycleScope.launch(Dispatchers.IO) {
            (application as MyApplication).db.ScoreDao().insertScore(scoreEntity)
        }
    }
}