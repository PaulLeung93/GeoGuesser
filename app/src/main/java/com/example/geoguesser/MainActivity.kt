package com.example.geoguesser

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.guessPlaceBtn).setOnClickListener {
            startActivity(Intent(this,GuessPlaceActivity::class.java))
        }

        findViewById<Button>(R.id.highScoreBtn).setOnClickListener {
            startActivity(Intent(this,ScoreboardActivity::class.java))
        }
    }
}