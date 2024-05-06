package com.example.geoguesser

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query


@Dao
interface ScoreDao {
    @Insert
    suspend fun insertScore(score: Score)

    @Query("SELECT * FROM scoreboard ORDER BY score")
    suspend fun getAllScores(): List<Score>
}

