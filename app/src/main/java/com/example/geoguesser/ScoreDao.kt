package com.example.geoguesser

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow


@Dao
interface ScoreDao {
    @Insert
    fun insertScore(score: Score)

    @Query("SELECT * FROM scoreboard")
    fun getAllScores(): Flow<List<Score>>
}

