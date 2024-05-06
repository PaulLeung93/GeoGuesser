package com.example.geoguesser

import android.app.Application

class MyApplication : Application() {
    val db by lazy { AppDatabase.getInstance(this) }
}