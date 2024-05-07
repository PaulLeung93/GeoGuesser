package com.example.geoguesser

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.geoguesser.databinding.ActivityScoreboardBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect


class ScoreboardActivity : AppCompatActivity() {

    private val scoreList = mutableListOf<Score>()
    private lateinit var binding: ActivityScoreboardBinding
    private lateinit var scoreAdapter: ScoreAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityScoreboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        scoreAdapter = ScoreAdapter(scoreList)
        binding.scoreboardRV.apply {
            adapter = scoreAdapter
            layoutManager = LinearLayoutManager(this@ScoreboardActivity)
        }

        lifecycleScope.launch{
            (application as MyApplication).db.ScoreDao().getAllScores().collect { databaseList ->
                databaseList.map { mappedList ->
                    scoreList.addAll(listOf(mappedList))
                    scoreAdapter.notifyDataSetChanged()
                }
            }
        }

    }

}

