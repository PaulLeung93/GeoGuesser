package com.example.geoguesser

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scoreboard")
data class Score(
    @ColumnInfo(name = "Score") val score: Int,
    @ColumnInfo(name = "PlayerName") val playerName: String?,
    @PrimaryKey(autoGenerate = true) val id: Long = 0
)
