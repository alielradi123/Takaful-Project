package com.example.takaful

import android.app.Application
import com.google.firebase.FirebaseApp

class TakafulApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
    }
}
