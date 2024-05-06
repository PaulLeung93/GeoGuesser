package com.example.geoguesser

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scoreboard")
data class Score(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "Score") val score: Int,
    @ColumnInfo(name = "PlayerName") val playerName: String?
)
