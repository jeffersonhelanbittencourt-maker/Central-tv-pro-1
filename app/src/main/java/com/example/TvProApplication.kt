package com.example

import android.app.Application
import com.example.data.local.TvProDatabase
import com.example.data.repository.TvRepository

class TvProApplication : Application() {

    val database by lazy { TvProDatabase.getDatabase(this) }
    val repository by lazy { TvRepository(database.tvProDao()) }

    override fun onCreate() {
        super.onCreate()
    }
}
